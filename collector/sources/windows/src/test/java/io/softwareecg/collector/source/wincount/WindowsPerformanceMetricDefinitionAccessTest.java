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
import org.junit.Assume;
import org.junit.Test;

import java.net.InetAddress;

/**
 * Tests the low level WPC DLL.
 * Created by johannes.weigend on 31.03.14.
 */
public class WindowsPerformanceMetricDefinitionAccessTest {

    @Test
    public void testGetCounter() throws Exception {

        Assume.assumeTrue(System.getProperty("os.name").startsWith("Win"));

        WindowsPerformanceCounterAccess wpc = new WindowsPerformanceCounterAccess();

        // the counters have to be prefixed with the hostname.
        String hostname = InetAddress.getLocalHost().getHostName();

        // A complete list of all performance counters are given by the command: "typeperf -q"
        double value = wpc.getCounter("\\\\" + hostname + "\\System\\Threads");
        Assert.assertTrue(value > 0);
    }
}
