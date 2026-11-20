package io.softwareecg.awb.importer.csv.bl;

import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import org.junit.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CsvMetricsParserNGTest {

    private static final String CSV_TEST_RESOURCE_DIR = "/io/softwareecg/awb/importer/csv/bl";

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

    private File getCsvFile(String fileName) throws Exception {
        return new File(CsvMetricsParserNGTest.class.getResource(CSV_TEST_RESOURCE_DIR + "/" + fileName).toURI());
    }
}
