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

import java.io.Closeable;
import java.io.IOException;

/**
 * A <code>PortForwarder</code> forwards TCP/IP connections to a local
 * port via the secure tunnel to another host (which may or may not be identical
 * to the remote SSH-2 server). Checkout {@link io.softwareecg.collector.ssh.SshConnection#createPortForwarding(int, String, int)}
 * on how to create one.
 *
 * @since 18.12.13
 */
public class PortForwarder {

    private final Closeable forwarder;
    private final int localPort;
    private final String targetHost;
    private final int targetPort;

    /**
     * Constructs a PortForwarder instance with the given closeable forwarder.
     *
     * @param forwarder the closeable forwarder resource
     * @param localPort the local port being forwarded
     * @param targetHost the target host
     * @param targetPort the target port
     */
    public PortForwarder(Closeable forwarder, int localPort, String targetHost, int targetPort) {
        this.forwarder = forwarder;
        this.localPort = localPort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    /**
     * Returns the local port that is being forwarded.
     *
     * @return the local port number
     */
    public int getLocalPort() {
        return localPort;
    }

    /**
     * Returns the target host for the port forwarding.
     *
     * @return the target host
     */
    public String getTargetHost() {
        return targetHost;
    }

    /**
     * Returns the target port for the port forwarding.
     *
     * @return the target port number
     */
    public int getTargetPort() {
        return targetPort;
    }

    /**
     * Stop TCP/IP forwarding of newly arriving connections.
     *
     * @throws IOException thrown if the connection is dead
     */
    public final void close() throws IOException {
        forwarder.close();
    }
}
