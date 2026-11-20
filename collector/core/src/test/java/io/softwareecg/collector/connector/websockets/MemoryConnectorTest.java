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
package io.softwareecg.collector.connector.websockets;

import org.junit.Test;

/**
 * Created by johannes.weigend on 14.03.14.
 */
public class MemoryConnectorTest {

    @Test
    public void testOnOpen() throws Exception {
        MemoryConnector memoryConnector = new MemoryConnector();
        memoryConnector.onOpen(null);
    }

    @Test
    public void testOnClose() throws Exception {
        MemoryConnector memoryConnector = new MemoryConnector();
        memoryConnector.onClose(null);

    }

    @Test
    public void testMessage() throws Exception {
        MemoryConnector memoryConnector = new MemoryConnector();
        memoryConnector.message("abc", null);
    }
}
