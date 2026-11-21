package io.softwareecg.collector.source.log;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LogMetricTest {

    @Test
    public void aLineThatContainsTheExpressionCounts() {
        LogMetric metric = LogMetric.of("login succeeded");

        assertEquals(LogMetric.Aggregate.COUNT, metric.aggregate());
        assertEquals(Double.valueOf(1), metric.valueOf("2026-11-21 10:00:00 INFO user login succeeded for anna"));
        assertNull(metric.valueOf("2026-11-21 10:00:00 INFO user login failed for anna"));
    }

    @Test
    public void anExpressionWrittenForTheWholeLineFindsTheSameLines() {
        LogMetric metric = LogMetric.of(".*transaction failed.*code=(TX-41|TX-72).*");

        assertEquals(Double.valueOf(1), metric.valueOf("10:00:00 ERROR transaction failed, code=TX-72, rolled back"));
        assertNull(metric.valueOf("10:00:00 ERROR transaction failed, code=TX-99"));
    }

    @Test
    public void aLineCountsOnceHoweverOftenItMatches() {
        assertEquals(Double.valueOf(1), LogMetric.of("retry").valueOf("retry retry retry"));
    }

    @Test
    public void aNamedGroupMakesTheSeriesAnAggregateOfTheCapturedNumbers() {
        for (LogMetric.Aggregate aggregate : LogMetric.Aggregate.values()) {
            if (aggregate == LogMetric.Aggregate.COUNT) {
                continue;
            }
            LogMetric metric = LogMetric.of("took (?<" + aggregate.groupName() + ">[0-9.,]+) ms");
            assertEquals(aggregate, metric.aggregate());
            assertEquals(Double.valueOf(12.5), metric.valueOf("request took 12.5 ms"));
            assertEquals("a decimal comma is one too", Double.valueOf(12.5), metric.valueOf("request took 12,5 ms"));
            assertNull(metric.valueOf("request took ages"));
        }
    }

    @Test
    public void whatIsCapturedButNoNumberDoesNotCount() {
        LogMetric metric = LogMetric.of("size=(?<sum>\\S+)");

        assertNull(metric.valueOf("size=unknown"));
        assertNull(metric.valueOf("size=1,234,567"));
        assertNull(metric.valueOf("size=NaN"));
        assertEquals(Double.valueOf(-3), metric.valueOf("size=-3"));
    }

    @Test
    public void whatIsNotARegularExpressionIsSaidSo() {
        try {
            LogMetric.of("login (succeeded");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'login (succeeded'"));
            assertTrue(e.getMessage(), e.getMessage().contains("not a regular expression"));
        }
    }

    @Test
    public void aMetricIsOneSeries() {
        try {
            LogMetric.of("took (?<avg>\\d+) of (?<max>\\d+)");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("two aggregates"));
        }
    }
}
