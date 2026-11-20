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
package io.softwareecg.collector.repository.common;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CSVUtilTest {

    @Test
    public void testGetValidCounterName() {
        String validCounterName = "JVM/Heap/Memory/used/";
        assertThat(CSVUtil.escapeCsvColumnName(validCounterName), is("JVM/Heap/Memory/used/"));
    }


    @Test
    public void testGetInvalidCounterName() {
        String invalidCounterName = "JVM;Heap;Memory;used/";
        assertThat(CSVUtil.escapeCsvColumnName(invalidCounterName), is("JVM/Heap/Memory/used/"));
    }

    @Test
    public void testEmptyCounterName() {
        String invalidCounterName = "";
        assertThat(CSVUtil.escapeCsvColumnName(invalidCounterName), is(""));
    }

    @Test
    public void testNullCounterName() {
        String invalidCounterName = null;
        assertThat(CSVUtil.escapeCsvColumnName(invalidCounterName), nullValue());
    }
}
