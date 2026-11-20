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
package io.softwareecg.collector.control.monitor;

import io.softwareecg.collector.control.BaseIntegrationTest;
import org.junit.Test;

/**
 * Created by johannes.weigend on 10.04.14.
 */
public class CollectorCoreMonitorTest extends BaseIntegrationTest {

    @Test
    public void testRun() throws Exception {
        super.startCollector();
        Thread.sleep(1000);
        super.stopCollector();
    }
}
