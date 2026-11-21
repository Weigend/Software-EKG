package io.softwareecg.collector.source.log;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the time a log line was written at, in the line itself.
 * <p>
 * The time a line arrives at the collector is not the time of the event: content a file already
 * has is read long after it was written, and so is everything written while a host could not be
 * reached. The timestamp of the line is what puts an event where it belongs.
 * <p>
 * Without a pattern the usual formats are recognised:
 * <ul>
 *   <li>{@code 2026-11-21 10:15:30,123} and {@code 2026-11-21T10:15:30.123+01:00} - what logback,
 *       log4j and most applications write, with or without fraction and offset;</li>
 *   <li>{@code [21/Nov/2026:10:15:30 +0100]} - the access logs of web servers;</li>
 *   <li>{@code Nov 21 10:15:30} at the start of the line - syslog, which names no year.</li>
 * </ul>
 * Any other format is described by a pattern of {@link DateTimeFormatter}. A timestamp without
 * an offset is one of the configured zone.
 * <p>
 * The timestamp is searched at the beginning of the line only, where it stands; a date somewhere
 * in a long message is not mistaken for it.
 */
final class LineTimestamps {

    /** The answer for a line without a timestamp. */
    static final long NONE = Long.MIN_VALUE;

    /** How much of a line is searched for the timestamp. */
    private static final int SEARCH_LIMIT = 256;

    /** Up to where in a line a timestamp described by a pattern may begin. */
    private static final int PATTERN_START_LIMIT = 96;

    private static final List<String> MONTHS = List.of(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");

    private interface Format {
        /**
         * @return the time in milliseconds, or {@link #NONE}
         */
        long find(String line, int limit);
    }

    private final ZoneId zone;

    private final LongSupplier clock;

    private final List<Format> formats;

    /** The format that fitted the last line; lines of one file are written alike. */
    private int usual = 0;

    /**
     * @param pattern the pattern of the timestamps, or {@code null} to recognise the usual formats
     * @param zone    the zone of timestamps that name none
     * @param clock   the current time in milliseconds, for timestamps without a year
     * @throws IllegalArgumentException if the pattern is not one, or does not describe date and time
     */
    LineTimestamps(String pattern, ZoneId zone, LongSupplier clock) {
        this.zone = zone;
        this.clock = clock;
        this.formats = pattern == null
                ? List.of(new IsoFormat(), new AccessLogFormat(), new SyslogFormat())
                : List.of(new PatternFormat(pattern));
    }

    /**
     * @return the time the line was written at, in milliseconds; {@link #NONE} if the line has no
     *         timestamp - the further lines of a stack trace, for example
     */
    long of(String line) {
        int limit = Math.min(line.length(), SEARCH_LIMIT);
        long time = formats.get(usual).find(line, limit);
        if (time != NONE) {
            return time;
        }
        for (int i = 0; i < formats.size(); i++) {
            if (i != usual) {
                time = formats.get(i).find(line, limit);
                if (time != NONE) {
                    usual = i;
                    return time;
                }
            }
        }
        return NONE;
    }

    private long millis(LocalDateTime local, ZoneOffset offset) {
        return offset != null
                ? local.toInstant(offset).toEpochMilli()
                : local.atZone(zone).toInstant().toEpochMilli();
    }

    // ---------------------------------------------------------------------------------------------
    // 2026-11-21 10:15:30,123   2026-11-21T10:15:30.123456Z   2026-11-21 10:15:30 +0100
    // ---------------------------------------------------------------------------------------------

    private final class IsoFormat implements Format {

        // An offset stands directly behind the time, or after one blank if it has four digits: a
        // blank and "-12" is the beginning of the message, not an offset.
        private final Pattern pattern = Pattern.compile(
                "(?<!\\d)(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2}):(\\d{2})(?:[.,](\\d{1,9}))?"
                        + "(?:(Z|[+-]\\d{2}(?::?\\d{2})?)(?![\\d:])| ([+-]\\d{2}:?\\d{2})(?!\\d))?");

        @Override
        public long find(String line, int limit) {
            Matcher matcher = pattern.matcher(line).region(0, limit);
            if (!matcher.find()) {
                return NONE;
            }
            try {
                String fraction = matcher.group(7);
                int nanos = fraction == null ? 0 : Integer.parseInt((fraction + "000000000").substring(0, 9));
                LocalDateTime local = LocalDateTime.of(
                        Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)),
                        Integer.parseInt(matcher.group(5)), Integer.parseInt(matcher.group(6)), nanos);
                String offset = matcher.group(8) != null ? matcher.group(8) : matcher.group(9);
                return millis(local, offset == null ? null : ZoneOffset.of(offset));
            } catch (DateTimeException e) {
                return NONE;   // digits in the shape of a date, such as a month 13
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // [21/Nov/2026:10:15:30 +0100]
    // ---------------------------------------------------------------------------------------------

    private final class AccessLogFormat implements Format {

        private final Pattern pattern = Pattern.compile(
                "\\[(\\d{2})/([A-Za-z]{3})/(\\d{4}):(\\d{2}):(\\d{2}):(\\d{2}) ([+-]\\d{4})]");

        @Override
        public long find(String line, int limit) {
            Matcher matcher = pattern.matcher(line).region(0, limit);
            if (!matcher.find()) {
                return NONE;
            }
            int month = MONTHS.indexOf(matcher.group(2).toLowerCase(Locale.ROOT)) + 1;
            if (month == 0) {
                return NONE;
            }
            try {
                LocalDateTime local = LocalDateTime.of(
                        Integer.parseInt(matcher.group(3)), month, Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)),
                        Integer.parseInt(matcher.group(6)));
                return millis(local, ZoneOffset.of(matcher.group(7)));
            } catch (DateTimeException e) {
                return NONE;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Nov 21 10:15:30   Nov  1 10:15:30
    // ---------------------------------------------------------------------------------------------

    private final class SyslogFormat implements Format {

        private static final long ONE_DAY = 86_400_000L;

        private final Pattern pattern = Pattern.compile(
                "^([A-Z][a-z]{2}) {1,2}(\\d{1,2}) (\\d{2}):(\\d{2}):(\\d{2})(?!\\d)");

        @Override
        public long find(String line, int limit) {
            Matcher matcher = pattern.matcher(line).region(0, limit);
            if (!matcher.find()) {
                return NONE;
            }
            int month = MONTHS.indexOf(matcher.group(1).toLowerCase(Locale.ROOT)) + 1;
            if (month == 0) {
                return NONE;
            }
            try {
                long now = clock.getAsLong();
                int year = Instant.ofEpochMilli(now).atZone(zone).getYear();
                LocalDateTime local = LocalDateTime.of(year, month, Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)),
                        Integer.parseInt(matcher.group(5)));
                long time = millis(local, null);
                // the line names no year: one that would lie in the future is of last year
                return time > now + ONE_DAY ? millis(local.minusYears(1), null) : time;
            } catch (DateTimeException e) {
                return NONE;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // a pattern of the configuration
    // ---------------------------------------------------------------------------------------------

    private final class PatternFormat implements Format {

        private final DateTimeFormatter formatter;

        /** Where in the line the timestamp of the last line began. */
        private int usualStart = 0;

        PatternFormat(String pattern) {
            try {
                formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
            } catch (IllegalArgumentException e) {
                // without the cause, so that a job reports this message and not the one inside it
                throw new IllegalArgumentException("The timestamp pattern '" + pattern + "' of the source log is not "
                        + "a pattern for dates: " + e.getMessage());
            }
            // a pattern that cannot read back what it writes says less than date and time
            String sample;
            try {
                sample = formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.getAsLong()), zone));
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("The timestamp pattern '" + pattern + "' of the source log cannot "
                        + "be used: " + e.getMessage());
            }
            if (find(sample, sample.length()) == NONE) {
                throw new IllegalArgumentException("The timestamp pattern '" + pattern + "' of the source log does "
                        + "not describe a date with its time. It needs year, month, day, hour, minute and second, "
                        + "for example dd.MM.yyyy HH:mm:ss.");
            }
        }

        @Override
        public long find(String line, int limit) {
            long time = parseAt(line, usualStart);
            if (time != NONE) {
                return time;
            }
            int last = Math.min(limit, PATTERN_START_LIMIT);
            for (int start = 0; start < last; start++) {
                if (start != usualStart) {
                    time = parseAt(line, start);
                    if (time != NONE) {
                        usualStart = start;
                        return time;
                    }
                }
            }
            return NONE;
        }

        private long parseAt(String line, int start) {
            if (start >= line.length()) {
                return NONE;
            }
            ParsePosition position = new ParsePosition(start);
            // does not throw for text that is no timestamp, which is what most positions hold
            if (formatter.parseUnresolved(line, position) == null || position.getErrorIndex() >= 0) {
                return NONE;
            }
            try {
                TemporalAccessor parsed = formatter.parse(line.subSequence(start, position.getIndex()));
                if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
                    return Instant.from(parsed).toEpochMilli();
                }
                LocalDateTime local = LocalDateTime.from(parsed);
                return millis(local, parsed.isSupported(ChronoField.OFFSET_SECONDS) ? ZoneOffset.from(parsed) : null);
            } catch (DateTimeException e) {
                return NONE;
            }
        }
    }
}
