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

import io.softwareecg.collector.connector.ICollectorConnector;
import org.glassfish.tyrus.server.Server;

import javax.websocket.DeploymentException;

/**
 * This class start the glassfish server and opens a websocket.
 * Clients can use the websocket connection to access ekg data.
 */
public class WebsocketConnector implements ICollectorConnector {

    /**
     * The port to listen.
     */
    private static final int PORT = 8025;

    /**
     * The instance to stop the server.
     */
    private Server server = null;

    @Override
    public void start() {

        server = new Server("localhost", PORT, "/collector", MemoryConnector.class);
        try {
            server.start();
        } catch (DeploymentException e) {
            throw new IllegalStateException("Can not start Websocket server at port: " + PORT, e);
        }
    }

    @Override
    public void stop() {
        server.stop();
    }
}
