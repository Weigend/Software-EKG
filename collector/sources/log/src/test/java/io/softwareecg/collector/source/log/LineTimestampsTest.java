package io.softwareecg.collector.source.log;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LineTimestampsTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    /** 2026-11-21 10:15:30 in Berlin, which is one hour ahead of UTC in November. */
    private static final long TIME = Instant.parse("2026-11-21T09:15:30Z").toEpochMilli();

    private static final long NOW = Instant.parse("2026-11-21T12:00:00Z").toEpochMilli();

    private final LineTimestamps timestamps = new LineTimestamps(null, BERLIN, () -> NOW);

    @Test
    public void whatApplicationsUsuallyWrite() {
        assertEquals(TIME + 123, timestamps.of("2026-11-21 10:15:30,123 INFO  [main] started"));
        assertEquals(TIME + 123, timestamps.of("2026-11-21 10:15:30.123 INFO started"));
        assertEquals(TIME, timestamps.of("2026-11-21 10:15:30 INFO started"));
        assertEquals(TIME, timestamps.of("[2026-11-21 10:15:30] started"));
        assertEquals(TIME + 123, timestamps.of("2026-11-21T10:15:30.123456 started"));
    }

    @Test
    public void aTimestampThatNamesItsOffsetDoesNotNeedTheZone() {
        assertEquals(TIME, timestamps.of("2026-11-21T09:15:30Z started"));
        assertEquals(TIME + 500, timestamps.of("2026-11-21T09:15:30.5Z started"));
        assertEquals(TIME, timestamps.of("2026-11-21T04:15:30-05:00 started"));
        assertEquals(TIME, timestamps.of("2026-11-21T14:45:30+0530 started"));
        assertEquals(TIME, timestamps.of("2026-11-21 11:15:30 +0200 started"));
        assertEquals(TIME + 7, timestamps.of("{\"@timestamp\":\"2026-11-21T09:15:30.007Z\",\"message\":\"started\"}"));
    }

    @Test
    public void aNumberAfterABlankIsTheMessageNotAnOffset() {
        assertEquals(TIME, timestamps.of("2026-11-21 10:15:30 -12 degrees outside"));
    }

    @Test
    public void accessLogsOfWebServers() {
        assertEquals(TIME, timestamps.of("10.0.0.7 - anna [21/Nov/2026:10:15:30 +0100] \"GET /cart HTTP/1.1\" 200 512"));
        assertEquals(TIME, timestamps.of("10.0.0.7 - - [21/Nov/2026:09:15:30 +0000] \"GET / HTTP/1.1\" 200 1"));
    }

    @Test
    public void syslogNamesNoYear() {
        assertEquals(TIME, timestamps.of("Nov 21 10:15:30 db-01 sshd[412]: Accepted password for ekg"));
        assertEquals("a day of one digit", Instant.parse("2026-11-01T09:15:30Z").toEpochMilli(),
                timestamps.of("Nov  1 10:15:30 db-01 sshd[412]: Accepted"));
        assertEquals("december has not come yet this year: it is the one of last year",
                Instant.parse("2025-12-24T17:00:00Z").toEpochMilli(),
                timestamps.of("Dec 24 18:00:00 db-01 cron[1]: run"));
    }

    @Test
    public void linesWithoutATimestamp() {
        assertEquals(LineTimestamps.NONE, timestamps.of(""));
        assertEquals(LineTimestamps.NONE, timestamps.of("\tat com.example.Checkout.pay(Checkout.java:42)"));
        assertEquals(LineTimestamps.NONE, timestamps.of("Caused by: java.lang.IllegalStateException: no stock"));
        assertEquals("digits in the shape of a date", LineTimestamps.NONE, timestamps.of("2026-13-45 99:99:99 broken"));
        assertEquals("syslog format only at the start of the line", LineTimestamps.NONE,
                timestamps.of("note: Nov 21 10:15:30 was the deadline"));
    }

    @Test
    public void aDateFarIntoALongMessageIsNotTheTimestamp() {
        String line = "x".repeat(300) + " 2026-11-21 10:15:30 ";

        assertEquals(LineTimestamps.NONE, timestamps.of(line));
    }

    @Test
    public void theFormatMayChangeFromLineToLine() {
        assertEquals(TIME, timestamps.of("2026-11-21 10:15:30 INFO started"));
        assertEquals(TIME, timestamps.of("10.0.0.7 - - [21/Nov/2026:10:15:30 +0100] \"GET / HTTP/1.1\" 200 1"));
        assertEquals(TIME, timestamps.of("Nov 21 10:15:30 db-01 sshd[412]: Accepted"));
        assertEquals(TIME, timestamps.of("2026-11-21 10:15:30 INFO started"));
    }

    @Test
    public void anyOtherFormatIsDescribedByAPattern() {
        LineTimestamps german = new LineTimestamps("dd.MM.yyyy HH:mm:ss", BERLIN, () -> NOW);

        assertEquals(TIME, german.of("21.11.2026 10:15:30 INFO gestartet"));
        assertEquals("not at the start of the line", TIME, german.of("[worker-3] 21.11.2026 10:15:30 gestartet"));
        assertEquals(TIME, german.of("21.11.2026 10:15:30 INFO gestartet"));
        assertEquals(LineTimestamps.NONE, german.of("2026-11-21 10:15:30 INFO started"));
        assertEquals(LineTimestamps.NONE, german.of("41.11.2026 10:15:30 no such day"));
        assertEquals(LineTimestamps.NONE, german.of(""));
    }

    @Test
    public void aPatternMayNameMonthsAndOffsets() {
        LineTimestamps withMonth = new LineTimestamps("dd-MMM-yyyy HH:mm:ss.SSS", BERLIN, () -> NOW);
        assertEquals(TIME + 123, withMonth.of("21-Nov-2026 10:15:30.123 INFO [main] org.apache.catalina started"));

        LineTimestamps withOffset = new LineTimestamps("yyyy/MM/dd HH:mm:ss Z", BERLIN, () -> NOW);
        assertEquals(TIME, withOffset.of("2026/11/21 09:15:30 +0000 started"));
    }

    @Test
    public void aPatternHasToDescribeDateAndTime() {
        try {
            new LineTimestamps("HH:mm:ss", BERLIN, () -> NOW);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not describe a date with its time"));
        }
        try {
            new LineTimestamps("yyyy-MM-dd HH:mm:ss {", BERLIN, () -> NOW);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("not a pattern for dates"));
        }
    }
}
