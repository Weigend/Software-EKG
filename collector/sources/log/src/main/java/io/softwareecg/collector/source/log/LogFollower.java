package io.softwareecg.collector.source.log;

import org.slf4j.LoggerFactory;
import io.softwareecg.collector.source.CounterSeries;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Follows one log file and turns its lines into series: for every interval and every metric the
 * number of matching lines, or the aggregate of the numbers they hold.
 * <p>
 * <b>Intervals.</b> Time is cut into intervals of the length the job collects with, on a fixed
 * grid. A value set carries the start of its interval and says what happened from then until
 * the next one. An interval is delivered when it is over, and intervals without a matching line
 * are delivered too - with a 0 for a count: that nothing happened is what the file says, and a
 * chart must not draw a line across it.
 * <p>
 * <b>Time.</b> A line belongs to the interval of its timestamp. A line without one - the further
 * lines of a stack trace - belongs where the line before it does. If the lines have no
 * timestamps at all, they belong to the moment they are read.
 * <p>
 * <b>What the file already has.</b> Without a lookback only what is written from now on is
 * read, as {@code tail -f} does. With one, the file is first read from its beginning, and
 * everything not older than the lookback is delivered at the time it happened. That needs the
 * timestamps: lines of which nobody knows when they were written are left out.
 * <p>
 * <b>Position.</b> The follower remembers up to where it read. A machine that cannot be reached
 * for a while costs nothing but the delay - afterwards reading continues where it ended, and
 * the lines written in between are counted at their own time. If the file is replaced
 * (rotated or emptied), reading continues at the beginning of the new one. What was added to the
 * old file after the last reading and before it was replaced is not seen: at most the lines of
 * one interval.
 */
final class LogFollower {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogFollower.class);

    /** How much of a file is read at once at most, so that a long file is not held in memory. */
    static final int MAX_PART_BYTES = 1 << 20;

    private static final int MIN_PART_BYTES = 1 << 16;

    /** A line longer than this is not waited for any longer; it is evaluated as far as it came. */
    private static final int MAX_LINE_CHARS = 1 << 20;

    /** Timestamps further ahead of this machine's clock are not believed: a wrong time zone. */
    private static final long FUTURE_TOLERANCE = 60_000;

    /** Timestamps that lag more than this behind are worth a hint: a wrong time zone, too. */
    private static final long LATE_TOLERANCE = 60_000;

    private static final long NOT_SET = Long.MIN_VALUE;

    private final LogFileReader reader;

    private final List<LogMetric> metrics;

    private final LineTimestamps timestamps;

    private final long lookbackMillis;

    // --- where reading stands -------------------------------------------------------------------

    private String fileKey;

    private long position;

    /** The beginning of a line whose end was not written yet. */
    private final StringBuilder unfinishedLine = new StringBuilder();

    private int partBytes = MAX_PART_BYTES;

    /**
     * {@code true} while the content the file had when reading began is read. Lines of that
     * content are placed by their timestamp or not at all.
     */
    private boolean readingHistory;

    /** The time of the last line that had a timestamp. */
    private long lastLineTime = NOT_SET;

    // --- what was counted and not delivered yet -----------------------------------------------------

    private long step = NOT_SET;

    /** The start of the last interval delivered; the next one follows it. */
    private long delivered = NOT_SET;

    /**
     * {@code false} until it is known from when on the file tells what happened - and what did
     * not: from the first reading that succeeds, or, if the content the file already has is read,
     * from its first line with a timestamp. Nothing is delivered before, not even zeros.
     */
    private boolean coverageKnown;

    /**
     * While the content the file already has is read: lines of intervals up to this one are left
     * out. They are older than what is asked for, or stored already.
     */
    private long leftOutUpTo = NOT_SET;

    private final TreeMap<Long, Interval> intervals = new TreeMap<>();

    private boolean warnedAboutFuture;

    private boolean warnedAboutLate;

    private boolean warnedAboutNoTimestamps;

    LogFollower(LogFileReader reader, List<LogMetric> metrics, LineTimestamps timestamps, long lookbackMillis) {
        this.reader = reader;
        this.metrics = metrics;
        this.timestamps = timestamps;
        this.lookbackMillis = lookbackMillis;
    }

    /**
     * @return {@code null} if the file can be read; otherwise what is wrong, written for the
     *         person who configured it
     */
    String check() {
        try {
            // reads nothing and changes nothing: the first reading still decides where to begin
            LogFileReader.Part part = reader.read(fileKey, position, true, 0);
            return part != null ? null
                    : "The log file " + reader.describe() + " does not exist or may not be read.";
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    /**
     * Reads what the file has got since the last call and delivers the intervals that are over.
     *
     * @param now      the current time in milliseconds
     * @param interval the length of an interval in milliseconds
     * @param since    the start of the last interval the storage holds, or {@code null} if it holds
     *                 none or does not know; only later intervals are delivered
     * @param sink     receives the value sets, oldest first: one value per metric, {@code null}
     *                 where an aggregate has no line to be computed from
     * @return {@code false} if the file could not be read
     */
    boolean poll(long now, long interval, Date since, Consumer<CounterSeries> sink) {
        if (step != interval) {
            // the grid changed: what was counted on the old one cannot be delivered on the new
            step = interval;
            intervals.clear();
            if (delivered != NOT_SET) {
                delivered = floor(delivered);
            }
        }
        long storedUpTo = since == null ? NOT_SET : floor(since.getTime());

        if (delivered == NOT_SET) {
            begin(now, storedUpTo);
        } else if (storedUpTo != NOT_SET && storedUpTo < delivered) {
            // The storage holds less than was delivered: it lost values. The file still has the
            // lines, so they are read again from its beginning.
            LOGGER.info("The storage of {} ends before what was delivered. Reading the file again from {}.",
                    reader.describe(), new Date(storedUpTo));
            readAgainFrom(storedUpTo);
        } else if (storedUpTo != NOT_SET && storedUpTo > delivered) {
            delivered = storedUpTo;
            intervals.headMap(delivered, true).clear();
        }

        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    return true;
                }
                boolean first = fileKey == null;
                LogFileReader.Part part = reader.read(fileKey, position, first && !readingHistory, partBytes);
                if (part == null) {
                    LOGGER.debug("The log file {} does not exist or may not be read.", reader.describe());
                    return false;
                }
                if (!first && part.start() != position) {
                    LOGGER.info("The log file {} was replaced. Reading the new one from its beginning.",
                            reader.describe());
                    unfinishedLine.setLength(0);
                }
                fileKey = part.fileKey();
                position = part.end();
                if (!coverageKnown && !readingHistory) {
                    // The file is followed from here on. Before, it could not be read, and nothing
                    // is known about that time.
                    coverageKnown = true;
                    delivered = Math.max(delivered, floor(now) - step);
                }
                evaluate(part.text(), now);
                partBytes = Math.min(MAX_PART_BYTES, partBytes * 2);

                if (!part.hasMore()) {
                    break;
                }
                // While a long file is caught up with, what is complete is delivered on the way.
                // Lines are written in the order of their time, give or take a moment: one
                // interval is held back for those that are not.
                if (lastLineTime != NOT_SET) {
                    deliver(Math.min(now, lastLineTime) - step, sink);
                }
            }
        } catch (IOException e) {
            // perhaps the part was too large for the connection: try with less
            partBytes = Math.max(MIN_PART_BYTES, partBytes / 2);
            LOGGER.debug("The log file {} could not be read: {}", reader.describe(), e.getMessage());
            return false;
        }

        if (readingHistory) {
            readingHistory = false;
            if (!coverageKnown) {
                // nothing in the file says when it was written: only what comes now can be placed
                if (!warnedAboutNoTimestamps && lookbackMillis > 0) {
                    warnedAboutNoTimestamps = true;
                    LOGGER.warn("No timestamps were found in the lines of {}. The lookback has no effect; set the "
                            + "argument 'timestamp' to the pattern the lines are written with.", reader.describe());
                }
                coverageKnown = true;
                delivered = Math.max(delivered, floor(now) - step);
            }
        }
        deliver(now, sink);
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // where to begin
    // ---------------------------------------------------------------------------------------------

    private void begin(long now, long storedUpTo) {
        if (lookbackMillis > 0) {
            readingHistory = true;
            delivered = Math.max(storedUpTo, floor(now - lookbackMillis) - step);
        } else {
            // Only what is written from now on. Nothing is known about the time between what the
            // storage holds and now, so nothing is delivered for it - not even zeros.
            readingHistory = false;
            delivered = Math.max(storedUpTo, floor(now) - step);
        }
        coverageKnown = false;
        leftOutUpTo = delivered;
    }

    private void readAgainFrom(long storedUpTo) {
        fileKey = null;
        position = 0;
        unfinishedLine.setLength(0);
        lastLineTime = NOT_SET;
        intervals.clear();
        readingHistory = true;
        coverageKnown = false;
        delivered = storedUpTo;
        leftOutUpTo = delivered;
    }

    // ---------------------------------------------------------------------------------------------
    // lines
    // ---------------------------------------------------------------------------------------------

    private void evaluate(String text, long now) {
        int from = 0;
        if (unfinishedLine.length() > 0) {
            int end = text.indexOf('\n');
            if (end < 0) {
                unfinishedLine.append(text);
                if (unfinishedLine.length() > MAX_LINE_CHARS) {
                    evaluateLine(takeUnfinishedLine(), now);
                }
                return;
            }
            unfinishedLine.append(text, 0, end);
            evaluateLine(takeUnfinishedLine(), now);
            from = end + 1;
        }
        while (from < text.length()) {
            int end = text.indexOf('\n', from);
            if (end < 0) {
                unfinishedLine.append(text, from, text.length());
                return;
            }
            evaluateLine(text.substring(from, end), now);
            from = end + 1;
        }
    }

    private String takeUnfinishedLine() {
        String line = unfinishedLine.toString();
        unfinishedLine.setLength(0);
        return line;
    }

    private void evaluateLine(String rawLine, long now) {
        // lines written on Windows, or sent through a terminal, end with carriage returns
        int length = rawLine.length();
        while (length > 0 && rawLine.charAt(length - 1) == '\r') {
            length--;
        }
        if (length == 0) {
            return;
        }
        String line = length == rawLine.length() ? rawLine : rawLine.substring(0, length);

        long time = timestamps.of(line);
        boolean ownTimestamp = time != LineTimestamps.NONE;
        if (ownTimestamp) {
            if (time > now + FUTURE_TOLERANCE) {
                if (!warnedAboutFuture) {
                    warnedAboutFuture = true;
                    LOGGER.warn("The timestamps of {} are ahead of this machine's clock by {} minutes. They are not "
                            + "used; lines count at the time they are read. Is the argument 'zone' missing?",
                            reader.describe(), (time - now) / 60_000);
                }
                ownTimestamp = false;
                time = readingHistory ? LineTimestamps.NONE : now;
            } else {
                lastLineTime = time;
            }
        } else if (lastLineTime != NOT_SET) {
            time = lastLineTime;
        } else if (!readingHistory) {
            time = now;
        }
        if (time == LineTimestamps.NONE) {
            return;   // old content of which nobody knows when it was written
        }

        long start = floor(time);
        if (readingHistory) {
            if (!coverageKnown) {
                // from its first line with a timestamp on the file tells what happened
                coverageKnown = true;
                delivered = Math.max(delivered, start - step);
                leftOutUpTo = delivered;
            }
            if (start <= leftOutUpTo) {
                return;   // older than what is asked for, or stored already
            }
        }
        if (start <= delivered) {
            // The interval of the line was delivered already. The line is counted in the next one,
            // so that nothing is lost; its time is off by what it came late.
            if (ownTimestamp && !readingHistory && !warnedAboutLate && now - time > LATE_TOLERANCE + step) {
                warnedAboutLate = true;
                LOGGER.warn("The timestamps of {} lag {} minutes behind this machine's clock. Such lines count at "
                        + "the time they are read. Is the argument 'zone' missing?",
                        reader.describe(), (now - time) / 60_000);
            }
            start = delivered + step;
        }

        Interval interval = null;
        for (int i = 0; i < metrics.size(); i++) {
            Double value = metrics.get(i).valueOf(line);
            if (value != null) {
                if (interval == null) {
                    interval = intervals.computeIfAbsent(start, s -> new Interval(metrics.size()));
                }
                interval.add(i, value);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // intervals
    // ---------------------------------------------------------------------------------------------

    private long floor(long time) {
        return Math.floorDiv(time, step) * step;
    }

    /**
     * Delivers the intervals that ended before the given time.
     */
    private void deliver(long upTo, Consumer<CounterSeries> sink) {
        if (!coverageKnown) {
            return;
        }
        Interval nothing = new Interval(metrics.size());
        for (long start = delivered + step; start + step <= upTo; start += step) {
            Interval interval = intervals.get(start);
            sink.accept(new CounterSeries(new Date(start), Arrays.asList((interval != null ? interval : nothing).values())));
            // only now: a sink that fails leaves the interval where it is, to be delivered again
            intervals.remove(start);
            delivered = start;
        }
    }

    /**
     * What the lines of one interval add up to, for every metric.
     */
    private final class Interval {

        private final long[] count;
        private final double[] sum;
        private final double[] min;
        private final double[] max;
        private final double[] last;

        Interval(int metricCount) {
            count = new long[metricCount];
            sum = new double[metricCount];
            min = new double[metricCount];
            max = new double[metricCount];
            last = new double[metricCount];
        }

        void add(int metric, double value) {
            min[metric] = count[metric] == 0 ? value : Math.min(min[metric], value);
            max[metric] = count[metric] == 0 ? value : Math.max(max[metric], value);
            count[metric]++;
            sum[metric] += value;
            last[metric] = value;
        }

        Double[] values() {
            Double[] values = new Double[count.length];
            for (int i = 0; i < values.length; i++) {
                boolean none = count[i] == 0;
                values[i] = switch (metrics.get(i).aggregate()) {
                    case COUNT -> (double) count[i];
                    case SUM -> sum[i];
                    case AVG -> none ? null : sum[i] / count[i];
                    case MIN -> none ? null : min[i];
                    case MAX -> none ? null : max[i];
                    case LAST -> none ? null : last[i];
                };
            }
            return values;
        }
    }
}
