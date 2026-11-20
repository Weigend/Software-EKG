//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.importer.csv.fileutil;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 */
public class CsvFormatTest {

    /**
     * Accesses the class' resources and tries to return a file. Although asserts that the file exists.
     *
     * @param resourceString the path in the resource folder
     * @return a File
     */
    private File getFileFromResource(String resourceString) {
        URL url = getClass().getResource(resourceString);
        File file = new File(url.getFile());
        assertTrue(file.exists());
        return file;
    }

    /**
     * Tests the getDelimiter for INSTANT_FORMAT
     */
    @Test
    public void testGetDelimiterForInstantFormat() {
        CsvFormat csvFormat = CsvFormat.INSTANT_FORMAT;
        String formatString = csvFormat.getDelimiter();

        assertEquals(";", formatString);
    }

    /**
     * Tests the getDateFormatString for DEFAULT_EKG_FORMAT
     */
    @Test
    public void testGetDelimiterForDefaultEkgFormat() {
        CsvFormat csvFormat = CsvFormat.DEFAULT_EKG_FORMAT;
        String formatString = csvFormat.getDelimiter();

        assertEquals(";", formatString);
    }

    /**
     * Tests the getDateFormatString for EKG_OLD_FORMAT
     */
    @Test
    public void testGetDelimiterForEkgOldFormat() {
        CsvFormat csvFormat = CsvFormat.EKG_OLD_FORMAT;
        String formatString = csvFormat.getDelimiter();

        assertEquals(";", formatString);
    }

    /**
     * Tests the getDateFormatString for MICROSOFT_PERFORMANCE_DATA_FORMAT
     */
    @Test
    public void testGetDelimiterForMsPerformanceFormat() {
        CsvFormat csvFormat = CsvFormat.MICROSOFT_PERFORMANCE_DATA_FORMAT;
        String formatString = csvFormat.getDelimiter();

        assertEquals(";\"", formatString);
    }

    /**
     * Tests the getDateFormatString for INSTANT_FORMAT
     */
    @Test
    public void testGetDateFormatStringForInstantFormat() {
        CsvFormat csvFormat = CsvFormat.INSTANT_FORMAT;
        String formatString = csvFormat.getDateFormatString();

        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS", formatString);
    }

    /**
     * Tests the getDateFormatString for DEFAULT_EKG_FORMAT
     */
    @Test
    public void testGetDateFormatStringForDefaultEkgFormat() {
        CsvFormat csvFormat = CsvFormat.DEFAULT_EKG_FORMAT;
        String formatString = csvFormat.getDateFormatString();

        assertEquals("dd.MM.yyyy HH:mm:ss.SSS", formatString);
    }

    /**
     * Tests the getDateFormatString for EKG_OLD_FORMAT
     */
    @Test
    public void testGetDateFormatStringForEkgOldFormat() {
        CsvFormat csvFormat = CsvFormat.EKG_OLD_FORMAT;
        String formatString = csvFormat.getDateFormatString();

        assertEquals("dd.MM.yyyy HH:mm:ss", formatString);
    }

    /**
     * Tests the getDateFormatString for MICROSOFT_PERFORMANCE_DATA_FORMAT
     */
    @Test
    public void testGetDateFormatStringForMsPerformanceFormat() {
        CsvFormat csvFormat = CsvFormat.MICROSOFT_PERFORMANCE_DATA_FORMAT;
        String formatString = csvFormat.getDateFormatString();

        assertEquals("MM/dd/yyyy HH:mm:ss.SSS", formatString);
    }



    /**
     * Tests the all format auto detection.
     */
    @Test
    public void testAutoDetection() {
        assertAutoDetectFor(CsvFormat.INSTANT_FORMAT);
        assertAutoDetectFor(CsvFormat.DEFAULT_EKG_FORMAT);
        assertAutoDetectFor(CsvFormat.EKG_OLD_FORMAT);
        assertAutoDetectFor(CsvFormat.MICROSOFT_PERFORMANCE_DATA_FORMAT);
    }

    /**
     * Tests for the MS format by the first line beginning
     */
    @Test
    public void testAutodetectForMsPerformanceFormatByPDHCSV() {
        File file = getFileFromResource(CsvFormat.MICROSOFT_PERFORMANCE_DATA_FORMAT + "_byPDH-CSV.csv");
        CsvFormat detectedFormat = CsvFormat.autodetect(file);

        assertEquals(CsvFormat.MICROSOFT_PERFORMANCE_DATA_FORMAT, detectedFormat);
    }

    /**
     * Tests successful detection without number format detection. Expecting Locale.US as default.
     */
    @Test
    public void testForSuccessfulDetectionWithoutNumberFormatDetection() {
        File file = getFileFromResource(CsvFormat.INSTANT_FORMAT + "_invalid_NumberFormat.csv");
        CsvFormat detectedFormat = CsvFormat.autodetect(file);

        assertEquals(CsvFormat.INSTANT_FORMAT, detectedFormat);
        assertEquals(DecimalFormat.getInstance(Locale.US), detectedFormat.getNumberFormat());
    }

    /**
     * Tests that autodetect falls back to DEFAULT_EKG_FORMAT when file has no recognizable last line.
     */
    @Test
    public void testForNoLastLine() {
        File file = getFileFromResource(CsvFormat.INSTANT_FORMAT + "_no_last_line.csv");
        CsvFormat detectedFormat = CsvFormat.autodetect(file);

        // autodetect now falls back to DEFAULT_EKG_FORMAT instead of returning null
        assertEquals(CsvFormat.DEFAULT_EKG_FORMAT, detectedFormat);
    }

    /**
     * Tests that autodetect falls back to DEFAULT_EKG_FORMAT when file has an unrecognized date format.
     */
    @Test
    public void testForWrongDateFormat() {
        File file = getFileFromResource(CsvFormat.INSTANT_FORMAT + "_wrongDateFormat.csv");
        CsvFormat detectedFormat = CsvFormat.autodetect(file);

        // autodetect now falls back to a format instead of returning null
        assertNotNull(detectedFormat);
    }

    /**
     * Tests for null when file no lines at all.
     */
    @Test
    public void testForEmptyCsv() {
        File file = getFileFromResource("CsvFormat_no_lines.csv");
        CsvFormat detectedFormat = CsvFormat.autodetect(file);

        assertNull(detectedFormat);
    }


    /**
     * Tests for null when file has no semicolon or comma in last line.
     */
    @Test
    public void testForLastLineHasNoSemicolon() {
        File file = getFileFromResource(CsvFormat.INSTANT_FORMAT + "_no_semicolon_last_line.csv");
        CsvFormat detectedFormat = CsvFormat.autodetect(file);

        assertNull(detectedFormat);
    }

    /**
     * Helper for auto detecting format.
     *
     * @param csvFormatToLookFor the CsvFormat to expect
     */
    private void assertAutoDetectFor(CsvFormat csvFormatToLookFor) {
        File file = getFileFromResource(csvFormatToLookFor + "_bydate.csv");
        CsvFormat detectedFormat = CsvFormat.autodetect(file);

        assertEquals(csvFormatToLookFor, detectedFormat);
    }
}
