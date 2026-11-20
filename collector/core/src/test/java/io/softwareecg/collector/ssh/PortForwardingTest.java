/*

 * Copyright (C) 2013 Weigend AM
 * *
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
*/
package io.softwareecg.collector.ssh;

import org.junit.Test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @since 22.12.13
 */
public class PortForwardingTest extends AbstractSshTest {

    private static final int FORWARDING_LOCAL_PORT = 19291;
    private static final int FORWARDING_REMOTE_PORT = 19292;

    @Test
    public void testPortForwarding() throws Exception {
        connectAndAuthenticate();

        assertPortIsNotReachable(FORWARDING_LOCAL_PORT);
        assertPortIsNotReachable(FORWARDING_REMOTE_PORT);

        PortForwarder forwarder = connection.createPortForwarding(FORWARDING_LOCAL_PORT, "127.0.0.1", FORWARDING_REMOTE_PORT);

        assertPortIsReachable(FORWARDING_LOCAL_PORT);
        assertPortIsNotReachable(FORWARDING_REMOTE_PORT);

        Server server = new Server();
        server.start();

        Thread.sleep(1000);

        assertPortIsReachable(FORWARDING_LOCAL_PORT);
        assertPortIsReachable(FORWARDING_REMOTE_PORT);
        assertForwardingWorks("testmessage", FORWARDING_LOCAL_PORT, server);

        forwarder.close();
        server.stop();

        // Give the system more time to fully close the port
        Thread.sleep(2000);

        assertPortIsNotReachable(FORWARDING_LOCAL_PORT);
        assertPortIsNotReachable(FORWARDING_REMOTE_PORT);
    }

    private void assertForwardingWorks(String message, int port, Server server) {
        try {
            Socket socket = new Socket("127.0.0.1", port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(message);
            Thread.sleep(2000);
            assertTrue(server.isConnectionReceived());
            socket.close();
        } catch (Exception e) {
            assertThat(e, is(nullValue()));
        }
    }

    private void assertPortIsNotReachable(int port) {
        try {
            Socket socket = new Socket("127.0.0.1", port);
            socket.close();
            fail("the port " + port + " is reachable");
        } catch (Exception e) {
        }
    }

    private void assertPortIsReachable(int port) {
        try {
            Socket socket = new Socket("127.0.0.1", port);
            socket.close();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public static class Server {

        private Thread socketListener;

        private ServerSocket serverSocket;

        private boolean receiveConnection;

        public void start() throws IOException {
            socketListener = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        serverSocket = new ServerSocket(FORWARDING_REMOTE_PORT);
                        serverSocket.setSoTimeout(5000);

                        Socket socket = serverSocket.accept();
                        receiveConnection = true;
                        socket.close();

                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });

            socketListener.start();
        }

        public boolean isConnectionReceived() {
            return receiveConnection;
        }

        public void stop() {
            try {
                socketListener.interrupt();
                serverSocket.close();
                socketListener.join(5000);
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
