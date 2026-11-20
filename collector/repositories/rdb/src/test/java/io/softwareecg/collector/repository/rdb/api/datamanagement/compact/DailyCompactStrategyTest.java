package io.softwareecg.collector.repository.rdb.api.datamanagement.compact;


import org.junit.Test;

import java.time.*;

import static org.junit.Assert.assertEquals;

/**
 * Unit-Test for the DailyCompactStrategy
 *
 * @since 02.12.2018
 */
public class DailyCompactStrategyTest {

    private static final ZoneId UTC_TIME_ZONE = ZoneId.of("UTC");

    @Test
    public void testGetNextExecTime() {

        DailyCompactStrategy strategy = new DailyCompactStrategy();
        long nextExecTime = strategy.getNextExecutionTime();

        LocalDateTime dateTime = LocalDateTime.now(UTC_TIME_ZONE);
        dateTime = dateTime.plusDays(0).withHour(3).withMinute(0).withSecond(0).withNano(0);

        assertEquals("The calculation of the next execution time didn't match the 3:00am next day specification.",
                dateTime.toInstant(ZoneOffset.UTC).toEpochMilli(), nextExecTime);
    }

    @Test
    public void testGetInterval() {

        DailyCompactStrategy strategy = new DailyCompactStrategy();
        CompactInterval interval = strategy.getTimeIntervalToCompact();

        LocalDateTime begin = LocalDateTime.of(LocalDate.ofInstant(Instant.now(), UTC_TIME_ZONE), LocalTime.MIDNIGHT);
        LocalDateTime end = begin;
        begin = begin.minusDays(1);

        assertEquals("The begin of the compact interval isn't 0:00am of last day",
                begin.toInstant(ZoneOffset.UTC).toEpochMilli(),  interval.getRangeBeginDatetime().getTime());

        assertEquals("The end of the compact interval isn't 0:00am of this day",
                end.toInstant(ZoneOffset.UTC).toEpochMilli(),  interval.getRangeEndDatetime().getTime());
    }
}
