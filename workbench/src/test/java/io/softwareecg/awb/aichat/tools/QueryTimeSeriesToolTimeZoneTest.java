package io.softwareecg.awb.aichat.tools;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

/**
 * The assistant names times as the chart beside it shows them: in the time zone of the chart,
 * not in that of the computer.
 */
public class QueryTimeSeriesToolTimeZoneTest {

    private static final long NOON_UTC = Instant.parse("2026-11-18T12:00:00Z").toEpochMilli();

    @Test
    public void aTimeIsGivenInTheZoneAskedFor() {
        assertEquals("2026-11-18 13:00:00", QueryTimeSeriesTool.formatEpoch(NOON_UTC, ZoneId.of("Europe/Berlin")));
        assertEquals("2026-11-18 12:00:00", QueryTimeSeriesTool.formatEpoch(NOON_UTC, ZoneId.of("UTC")));
        assertEquals("2026-11-18 09:00:00", QueryTimeSeriesTool.formatEpoch(NOON_UTC, ZoneId.of("America/Sao_Paulo")));
    }
}
