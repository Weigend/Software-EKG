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
package io.softwareecg.collector.source.wincount;


import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * Tests the low level WPC DLL.
 * Created by johannes.weigend on 31.03.14.
 */
public class WindowsPerformanceMetricDefinitionAccessTest {

    @Test
    public void testGetCounter() throws Exception {

        WindowsCounters.assumeAvailable();

        WindowsPerformanceCounterAccess wpc = new WindowsPerformanceCounterAccess();

        // the counters have to be prefixed with the hostname.
        String hostname = InetAddress.getLocalHost().getHostName();

        // A complete list of all performance counters are given by the command: "typeperf -q"
        double value = wpc.getCounter("\\\\" + hostname + "\\System\\Threads");
        Assert.assertTrue(value > 0);
    }

    /**
     * Expands a wildcard and reads all its counters with one query, the way the collector does.
     * The object "System" has the same name on an English and on a German Windows.
     */
    @Test
    public void testResolveWildcardsAndQueryCounter() throws Exception {

        WindowsCounters.assumeAvailable();

        WindowsPerformanceCounterAccess wpc = new WindowsPerformanceCounterAccess();
        String prefix = "\\\\" + InetAddress.getLocalHost().getHostName() + "\\System\\";

        String[] counters = wpc.resolveWildcards(prefix + "*");
        Assert.assertNotNull(counters);
        Assert.assertTrue("only " + counters.length + " counters of the object System", counters.length > 1);
        for (String counter : counters) {
            Assert.assertTrue(counter, counter.startsWith(prefix));
        }

        double[] values = new double[counters.length];
        long handle = wpc.queryCounter(0, counters, values);
        Assert.assertNotEquals(0, handle);
        // a second query: rates have a value only from then on, and the handle is used again
        Assert.assertEquals(handle, wpc.queryCounter(handle, counters, values));
        Assert.assertTrue("no counter of the object System has a value: " + Arrays.toString(values),
                Arrays.stream(values).anyMatch(value -> value > 0));
    }

    @Test
    public void testResolveWildcardsOfUnknownObject() throws Exception {

        WindowsCounters.assumeAvailable();

        WindowsPerformanceCounterAccess wpc = new WindowsPerformanceCounterAccess();
        String path = "\\\\" + InetAddress.getLocalHost().getHostName() + "\\NoSuchObjectOfSoftwareEkg\\*";
        try {
            // nothing to expand: no result or an error, but no crash
            String[] counters = wpc.resolveWildcards(path);
            Assert.assertTrue(counters == null || counters.length == 0);
        } catch (RuntimeException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    /**
     * The names of the counters are known in English and in the language of this Windows, so a
     * counter written in English is read whatever that language is.
     */
    @Test
    public void testEnglishNamesAreReadOnThisWindows() throws Exception {

        WindowsCounters.assumeAvailable();

        WindowsPerformanceCounterAccess wpc = new WindowsPerformanceCounterAccess();
        String[] english = wpc.englishNames();
        Assert.assertNotNull(english);
        java.util.List<String> names = Arrays.asList(english);
        Assert.assertTrue("an even list of numbers and names: " + english.length, english.length > 1000 && english.length % 2 == 0);
        int processor = names.indexOf("Processor");
        Assert.assertTrue("the list has no 'Processor'", processor > 0);
        String local = wpc.lookupName(Integer.parseInt(english[processor - 1]));
        Assert.assertNotNull("the processor has no name on this Windows", local);
        Assert.assertNull(wpc.lookupName(Integer.MAX_VALUE));

        Assert.assertTrue(WindowsCounterProbe.isAvailable());
        double[] values = WindowsCounterProbe.read("localhost", java.util.List.of(
                "\\Memory\\Available Bytes", "\\System\\Threads", "\\No Such Object\\No Such Counter"));
        Assert.assertTrue("available memory: " + Arrays.toString(values), values[0] > 0);
        Assert.assertTrue("threads: " + Arrays.toString(values), values[1] > 0);
        Assert.assertEquals("a counter that is not there has no value", -1, values[2], 0);
    }

    /**
     * What the setup of a collection asks: which processes run here? This test runs in one.
     */
    @Test
    public void testProbeFindsTheProcesses() throws Exception {

        WindowsCounters.assumeAvailable();

        java.util.List<String> counters = WindowsCounterProbe.expand("localhost", "\\Process(*)\\Working Set");
        java.util.List<String> processes = counters.stream().map(WindowsCounterProbe::instanceOf)
                .collect(java.util.stream.Collectors.toList());
        Assert.assertTrue("no java among " + processes, processes.stream().anyMatch(name -> name.startsWith("java")));

        double[] memory = WindowsCounterProbe.readPaths(counters);
        Assert.assertEquals(counters.size(), memory.length);
        Assert.assertTrue("no process uses memory: " + Arrays.toString(memory),
                Arrays.stream(memory).anyMatch(value -> value > 0));
    }
}
