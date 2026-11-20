//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       description:   <<TODO>>
//______________________________________________________________________________
//
//        Copyright:    Weigend AM 2014
//______________________________________________________________________________

package io.softwareecg.collector.connector.websockets;

import io.softwareecg.collector.connector.ICollectorConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by johannes.weigend on 14.03.14.
 */
public class WebsocketConnectorTest {

    private ICollectorConnector connector;

    @Before
    public void testStart() throws Exception {
        connector = new WebsocketConnector();
        connector.start();
        // wait until server is alive and responding
    }

    @After
    public void testStop() throws Exception {
        connector.stop();
    }

    @Test
    public void test() {
        // send rest
    }
}
