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

import io.softwareecg.collector.common.LoggerFactory;
import org.slf4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class will be implemented in a future release.
 * It should be a REST Interface to the ekg memory.
 */
@ServerEndpoint(value = "/memory")
public class MemoryConnector {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The connected peers.
     */
    private final Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());

    /**
     * Called when a peer connects.
     *
     * @param peer the peer.
     */
    @OnOpen
    public void onOpen(Session peer) {
        peers.add(peer);
    }

    /**
     * Called when a peer disconnects.
     *
     * @param peer the peer.
     */
    @OnClose
    public void onClose(Session peer) {
        peers.remove(peer);
    }

    /**
     * Called when a peer sends a message.
     *
     * @param message the message.
     * @param client  the client peer.
     */
    @OnMessage
    public void message(String message, Session client) {

        LOGGER.debug("Got Message: " + message);
        for (Session peer : peers) {
            try {
                peer.getBasicRemote().sendObject(message);
            } catch (IOException | EncodeException e) {
                LOGGER.debug("Can not send message to remote client", e);
            }
        }
    }
}

