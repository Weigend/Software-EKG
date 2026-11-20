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
package io.softwareecg.collector.connector.nop;

import io.softwareecg.collector.connector.ICollectorConnector;
import org.junit.Test;

/**
 * Created by johannes.weigend on 14.03.14.
 */
public class NOPCollectorConnectorTest {

    @Test
    public void test() {
        ICollectorConnector connector = new NOPCollectorConnector();
        connector.start();
        connector.start();
    }
}
