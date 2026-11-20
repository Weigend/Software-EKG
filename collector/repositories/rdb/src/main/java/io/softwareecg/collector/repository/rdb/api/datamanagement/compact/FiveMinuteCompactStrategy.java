package io.softwareecg.collector.repository.rdb.api.datamanagement.compact;

import java.util.Date;

/**
 * An implementation of CompactIntervalStrategy that will calculate
 * time intervals of 5 minutes that will start at midnight each day.
 * The first computed interval is may long to match this time shapes.
 *
 * @since 29.11.2018
 */
public class FiveMinuteCompactStrategy implements CompactIntervalStrategy {

    private static final long FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;

    /**
     * The timestamp that represents the moment than the next compaction
     * should start.
     */
    protected long nextCompactExecTime = 0;

    private long rangeBegin;
    private long rangeEnd;


    public FiveMinuteCompactStrategy() {

        long currentTimeStamp = System.currentTimeMillis();
        long lastFiveMinCompleted = currentTimeStamp - currentTimeStamp % FIVE_MINUTES_IN_MILLIS;

        // the first commit interval is longer to ensure we are moving in 5 minute blocks beginning at 0:00 every day
        rangeBegin = currentTimeStamp;
        rangeEnd = lastFiveMinCompleted + FIVE_MINUTES_IN_MILLIS;

        nextCompactExecTime = rangeEnd + (2 * 60 * 1000);
    }

    @Override
    public CompactInterval getTimeIntervalToCompact() {

        CompactInterval interval = new CompactInterval(new Date(rangeBegin), new Date(rangeEnd));

        if (System.currentTimeMillis() > nextCompactExecTime) {
            setNextExecutionTime();
        }

        return interval;
    }

    @Override
    public long getNextExecutionTime() {
        return nextCompactExecTime;
    }

    private void setNextExecutionTime() {
        rangeBegin = rangeEnd;
        rangeEnd = rangeBegin + FIVE_MINUTES_IN_MILLIS;

        // let begin the start one milli later so we do not have any overlap with the previous interval
        rangeBegin++;

        nextCompactExecTime += FIVE_MINUTES_IN_MILLIS;
    }

    @Override
    public String toString() {
        return FiveMinuteCompactStrategy.class.getSimpleName();
    }
}
