package io.softwareecg.awb.importer.adapter.csv;

import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.timeseries.RawSeriesData;
import org.junit.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class CsvMetricsParserNGTest {

    private static final String CSV_TEST_RESOURCE_DIR = "/io/softwareecg/awb/importer/adapter/csv";

    private static final SimpleDateFormat EKG_STD_DATEFORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");

    static {
        // CsvFormat now uses UTC for all date parsing
        EKG_STD_DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testParse() throws Exception {
        CsvMetricsParserNG parser = new CsvMetricsParserNG();

        // the classic format, JMX of a WebLogic server (2014)
        RawSeriesData[] weblogicData = parser.parse(getCsvFile("classic-weblogic-2014.csv"));
        assertTrue("Should have parsed multiple series", weblogicData.length > 0);
        
        // Validate first column
        RawSeriesData firstCol = weblogicData[0];
        assertEquals("MXBean(com.bea:Name=replikat02,ServerRuntime=i0_hosta107,Type=JDBCDataSourceRuntime).PrepStmtCacheDeleteCount", 
                firstCol.getMetricName());
        assertEquals(EKG_STD_DATEFORMAT.parse("28.12.2014 00:00:43.768").getTime(), 
                firstCol.getValues().get(0).getTimestamp());
        assertEquals(-3351, firstCol.getValues().get(0).getValue(), 0.0);

        // the classic format compressed, JMX of a JVM (2015)
        RawSeriesData[] jmxData = parser.parse(getCsvFile("classic-jmx-2015.csv.gz"));
        assertTrue("Should have parsed multiple series", jmxData.length > 0);
        
        // Validate first column
        firstCol = jmxData[0];
        assertEquals("java.lang:name=PS Perm Gen,type=MemoryPool/Usage/committed", 
                firstCol.getMetricName());
        assertEquals(EKG_STD_DATEFORMAT.parse("17.11.2015 11:14:45.892").getTime(), 
                firstCol.getValues().get(0).getTimestamp());
        assertEquals(135_266_304.0, firstCol.getValues().get(0).getValue(), 0.0);

        // the classic format, Windows performance counters (2012)
        RawSeriesData[] windowsData = parser.parse(getCsvFile("classic-windows-2012.csv"));
        assertTrue("Should have parsed multiple series", windowsData.length > 0);
        
        // Validate first column
        firstCol = windowsData[0];
        assertEquals("\\Prozess(Java)\\Prozesskennung erstellen", 
                firstCol.getMetricName());
        assertEquals(EKG_STD_DATEFORMAT.parse("11.05.2012 14:09:57.679").getTime(), 
                firstCol.getValues().get(0).getTimestamp());
        assertEquals(1144.0, firstCol.getValues().get(0).getValue(), 0.0);
    }

    /**
     * A file as the CSV repository of the collector writes it for the source "log": the file of
     * one day, named by it, in which an aggregate has no value where no line matched.
     */
    @Test
    public void aColumnThatIsEmptyIsAValueThatIsMissing() throws Exception {
        RawSeriesData[] series = new CsvMetricsParserNG().parse(getCsvFile("collector72-log-events_2026-11-20.csv"));

        assertEquals(2, series.length);
        RawSeriesData logins = series[0];
        RawSeriesData slowest = series[1];
        assertEquals("logins", logins.getMetricName());
        assertEquals("slowest request ms", slowest.getMetricName());

        assertEquals("a count has a value for every interval", 5, logins.getValues().size());
        assertEquals(1795184460000L, logins.getValues().get(2).getTimestamp());
        assertEquals(1, logins.getValues().get(2).getValue(), 0.0);
        assertEquals(0, logins.getValues().get(0).getValue(), 0.0);

        assertEquals("the aggregate only where a line matched", 1, slowest.getValues().size());
        assertEquals(1795184460000L, slowest.getValues().get(0).getTimestamp());
        assertEquals(310, slowest.getValues().get(0).getValue(), 0.0);
    }

    /**
     * The header of a collector file ends with a separator. That is not a further column - and
     * no reason to leave out the last one.
     */
    @Test
    public void everyColumnOfTheHeaderIsASeries() throws Exception {
        RawSeriesData[] series = new CsvMetricsParserNG().parse(getCsvFile("unix-top-2014.csv"));

        String header = java.nio.file.Files.readAllLines(getCsvFile("unix-top-2014.csv").toPath()).get(0);
        String[] columns = header.split(";");
        assertEquals("Date", columns[0]);
        assertEquals(columns.length - 1, series.length);
        assertEquals("\\Process(java)\\RES", series[series.length - 1].getMetricName());
        assertFalse("the last column has its values", series[series.length - 1].getValues().isEmpty());
    }

    /**
     * The collector writes the group of the process into the header; the import makes it the
     * dimension of that name. What a later collector may add to the header is no reason to
     * reject the file.
     */
    @Test
    public void theGroupOfTheProcessIsADimension() throws Exception {
        RawSeriesData[] series = new CsvMetricsParserNG().parse(getCsvFile("collector-with-process-group_2026-11-21.csv"));

        assertEquals(2, series.length);
        for (RawSeriesData one : series) {
            assertEquals("app-servers", one.getLabel(FilterDimension.HOST_GROUP.name()));
            assertEquals("app-01", one.getLabel(FilterDimension.HOST.name()));
            assertEquals("app-jvm", one.getLabel(FilterDimension.PROCESS_GROUP.name()));
            assertEquals("Checkout API", one.getLabel(FilterDimension.PROCESS.name()));
            assertEquals("jvm", one.getLabel(FilterDimension.METRIC_GROUP.name()));
        }
        assertEquals("threads", series[1].getLabel(FilterDimension.METRIC_NAME.name()));
        assertEquals(41, series[1].getValues().get(1).getValue(), 0.0);

        // a file of a collector that did not write the group yet
        RawSeriesData[] older = new CsvMetricsParserNG().parse(getCsvFile("collector72-log-events_2026-11-20.csv"));
        assertEquals("checkout", older[0].getLabel(FilterDimension.PROCESS.name()));
        assertEquals(null, older[0].getLabel(FilterDimension.PROCESS_GROUP.name()));
    }

    private File getCsvFile(String fileName) throws Exception {
        return new File(CsvMetricsParserNGTest.class.getResource(CSV_TEST_RESOURCE_DIR + "/" + fileName).toURI());
    }
}
