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

package io.softwareecg.collector.common;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

/**
 * Created by johannes.weigend on 06.03.14.
 */
public class TextTableTest {

    private static final List<String> ROW_LABELS = Arrays.asList("Row1", "Row2", "Row3");
    private static final List<String> HEADER_LABELS = Arrays.asList("C", "C");
    private static final List<List<String>> DATA = Arrays.asList(
            Arrays.asList("1", "1"),
            Arrays.asList("2", "2"),
            Arrays.asList("3", "3")
    );

    @Test
    public void testToString() throws Exception {
        TextTable table1 = new TextTable(ROW_LABELS, HEADER_LABELS, DATA);
        String tableString1 = table1.toString();
        Assert.assertFalse(tableString1.isEmpty());
        // testSymmetry(tableString1);

        TextTable table2 = new TextTable(HEADER_LABELS, DATA);
        String tableString2 = table2.toString();
        Assert.assertFalse(tableString2.isEmpty());
        testSymmetry(tableString2);
    }

    @Test
    public void testSetHorizontalInset() throws Exception {
        TextTable table1 = new TextTable(HEADER_LABELS, DATA);
        table1.setHorizontalInset(10);
        testSymmetry(table1.toString());

        TextTable table2 = new TextTable(HEADER_LABELS, DATA);
        table2.setHorizontalInset(0);
        Assert.assertTrue(table1.toString().length() > table2.toString().length());

    }

    private void testSymmetry(String tableString1) {
        StringReader reader = new StringReader(tableString1);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                StringBuilder reverseLine = new StringBuilder(line).reverse();
                Assert.assertEquals(line, reverseLine.toString());
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
    }

}
