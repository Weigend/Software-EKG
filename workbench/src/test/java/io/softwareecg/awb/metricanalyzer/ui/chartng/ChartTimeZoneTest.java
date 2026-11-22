package io.softwareecg.awb.metricanalyzer.ui.chartng;

import org.junit.Test;

import java.time.ZoneId;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The time zones a chart offers, and the one it begins with.
 */
public class ChartTimeZoneTest {

    /** It was Europe/Berlin on every computer, wherever it stood. */
    @Test
    public void aChartBeginsWithTheTimeZoneOfTheComputer() {
        assertEquals(ZoneId.systemDefault(), ChartTimeZone.getDefaultTimeZone().getTimeZone());
    }

    @Test
    public void theLocalTimeZoneIsNamedAsSuch() {
        assertEquals("Local time (America/Sao_Paulo)",
                ChartTimeZone.localTimeZone(ZoneId.of("America/Sao_Paulo")).getAlias());
    }

    @Test
    public void theListBeginsWithTheLocalZoneAndHasUtcAndTheOffsets() {
        List<ChartTimeZone> zones = ChartTimeZone.getChartTimeZones();

        assertEquals(ChartTimeZone.getDefaultTimeZone(), zones.get(0));
        assertEquals(ZoneId.of("UTC"), zones.get(1).getTimeZone());
        assertTrue(zones.stream().anyMatch(zone -> "GMT+1".equals(zone.getAlias())));
        assertTrue("Europe/Berlin is in the list, as the local zone or beside it",
                zones.stream().anyMatch(zone -> ZoneId.of("Europe/Berlin").equals(zone.getTimeZone())));
    }
}
