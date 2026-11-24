//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.importer.adapter.csv.io;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for the CSV File Helper.
 *
 */
public class CsvFileHelperTest {

    private static final String TEST_FILE = "dummy.csv";
    private static final String EMPTY_FILE = "empty.csv";
    private static final String INVALID_FILE = "invalid.csv";
    private static final String BIG_FILE = "big.csv";
    private static final String LONG_LAST = "longLastLine.csv";

    @Test
    public void testGetFirstLine() throws Exception {
        String firstLine = CsvFileHelper.getFirstLine(
                new File(
                        getClass().getResource(TEST_FILE).getFile()));

        String dummyTest = "A;B;C;D;E;";
        assertEquals(dummyTest, firstLine);
    }

    @Test
    public void testGetLastLine() throws Exception {
        String lastLine = CsvFileHelper.getLastLine(
                new File(
                        getClass().getResource(TEST_FILE).getFile()));

        String dummyTest = "11;12;13;14;17;";
        assertEquals(">"+dummyTest+"<", ">"+lastLine+"<");
    }

    /**
     * Note: A 'big' file is here every file with more than
     * 255 byte.
     * In practice this case will occur nearly always.
     */
    @Test
    public void testGetLastLineCaseBigFile() throws Exception {
        String lastLine = CsvFileHelper.getLastLine(
                new File(
                        getClass().getResource(BIG_FILE).getFile()));

        String dummyTest = "11;12;13;14;17;";
        assertEquals(">"+dummyTest+"<", ">"+lastLine+"<");
    }

    @Test
    public void testGetLastLineCaseLongLastLine() throws Exception {
        String lastLine = CsvFileHelper.getLastLine(
                new File(
                        getClass().getResource(LONG_LAST).getFile()));
        System.err.println(lastLine);
    }

    @Test
    public void testGetLastLineCaseEmptyFile() throws Exception {
        String lastLine = CsvFileHelper.getLastLine(
                new File(
                        getClass().getResource(EMPTY_FILE).getFile()
                )
        );
        assertNull("null returned for empty file", lastLine);
    }

    // Get Last Line works with Files less than 3 lines as well
    public void testGetLastLineCaseInvalidFile() throws Exception {
        String lastLine = CsvFileHelper.getLastLine(
                new File(
                        getClass().getResource(INVALID_FILE).getFile()
                )
        );
        assertNull("return null from invalid source", lastLine);
    }
}
