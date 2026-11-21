package io.softwareecg.collector.repository.rdb.api;

import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.rdb.api.schema.EkgSchemaField;
import io.softwareecg.collector.repository.rdb.api.schema.FieldReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TimeSeriesTest {

    @Test
    public void aSeriesTakesTheGroupsOfItsJob() {
        TimeSeries series = new TimeSeries("2026-11-21-12-00-00", header("app-jvm"), "heap");

        assertEquals("app-servers", series.getHostGroupName());
        assertEquals("app-01", series.getHostName());
        assertEquals("app-jvm", series.getProcessGroupName());
        assertEquals("Checkout API", series.getProcessName());
        assertEquals("jvm", series.getMetricGroupId());
        assertEquals("heap", series.getMetric());
    }

    @Test
    public void theGroupOfTheProcessIsStoredUnderTheNameTheWorkbenchReads() {
        assertEquals("ts_processGroup", EkgSchemaField.TS_PROCESS_GROUP_NAME.getName());
        assertTrue("it is a field of the stored record",
                FieldReader.getSchemaFields(TimeSeries.class).containsKey("ts_processGroup"));
    }

    @Test
    public void processesOfTheSameNameInTwoGroupsAreTwoSeries() {
        TimeSeries inOneGroup = new TimeSeries("m", header("app-jvm"), "heap");
        TimeSeries inAnother = new TimeSeries("m", header("batch-jvm"), "heap");
        TimeSeries inTheSame = new TimeSeries("another measurement", header("app-jvm"), "heap");

        assertNotEquals("the records of the two must not be merged into one",
                inOneGroup.getGroupKeyHash(), inAnother.getGroupKeyHash());
        assertEquals("records of one series belong together whenever they were written",
                inOneGroup.getGroupKeyHash(), inTheSame.getGroupKeyHash());
    }

    @Test
    public void aSeriesWithoutAGroupOfItsProcessHasTheKeyItAlwaysHad() {
        TimeSeries series = new TimeSeries("m", header(null), "heap");

        assertNull(series.getProcessGroupName());
        // the key as it was computed before the group of the process was part of it
        int before = ("Shop" + "app-servers" + "app-01" + "" + "" + "" + "" + "Checkout API" + "jvm" + "heap").hashCode();
        assertEquals(Integer.valueOf(before), series.getGroupKeyHash());
        assertEquals(before, TimeSeries.calculateGroupHashKey("Shop", "app-servers", "app-01", null, null, null, null,
                null, "Checkout API", "m", "jvm", "heap"));
    }

    private static TimeSeriesHeader header(String processGroup) {
        TimeSeriesHeader header = new TimeSeriesHeader();
        header.setEkgProjectName("Shop");
        header.setHostGroupId("app-servers");
        header.setHostName("app-01");
        header.setProcessGroupId(processGroup);
        header.setProcessLabel("Checkout API");
        header.setMetricGroupId("jvm");
        return header;
    }
}
