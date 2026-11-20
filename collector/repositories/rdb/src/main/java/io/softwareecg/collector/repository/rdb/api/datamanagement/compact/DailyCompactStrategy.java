package io.softwareecg.collector.repository.rdb.api.datamanagement.compact;

import java.time.*;
import java.util.Date;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * An implementation of CompactIntervalStrategy that will calculate
 * time intervals of 1 day that will begin at midnight of UTC timezone.
 * The execution of the compact process will scheduled to 3:00am UTC.
 *
 * @since 29.11.2018
 */
public class DailyCompactStrategy implements CompactIntervalStrategy {

    /**
     * The timestamp that represents the moment than the next compaction
     * should start.
     */
    protected long nextCompactExecTime;

    /**
     * A calendar instance that will use to schedule the execution time
     * of the compaction/commit tasks.
     */
    protected LocalDateTime executionCalendar;

    /**
     * Constructs a new instance of the DailyCompactStrategy
     * with initialized scheduling plan.
     */
    public DailyCompactStrategy() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);
        executionCalendar = todayMidnight.plusHours(3);
        nextCompactExecTime = executionCalendar.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public CompactInterval getTimeIntervalToCompact() {

        if (System.currentTimeMillis() > nextCompactExecTime) {
            setNextExecutionTime();
        }

        // calculate the UTC timestamp of the beginning of the current day
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);

        Instant rangeBeginTime = todayMidnight.toInstant(ZoneOffset.UTC).minus(1, DAYS);
        Instant rangeEndTime = todayMidnight.toInstant(ZoneOffset.UTC);

        // define filters to select the time series by its 'start', 'stop' and 'import-time' attributes
         return new CompactInterval(Date.from(rangeBeginTime), Date.from(rangeEndTime));
    }

    @Override
    public long getNextExecutionTime() {
        return nextCompactExecTime;
    }

    private void setNextExecutionTime() {
        executionCalendar = executionCalendar.plusDays(1);
        nextCompactExecTime = executionCalendar.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
