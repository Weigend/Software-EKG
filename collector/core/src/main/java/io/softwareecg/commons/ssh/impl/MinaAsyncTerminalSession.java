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
package io.softwareecg.commons.ssh.impl;

import io.softwareecg.commons.ssh.SshConnection;
import io.softwareecg.commons.ssh.TerminalSession;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous TerminalSession implementation using Apache Mina SSHD.
 * Provides interactive shell access.
 *
 * @since 14.12.13
 */
public class MinaAsyncTerminalSession extends MinaAbstractSession implements TerminalSession {

    private static final Logger LOG = LoggerFactory.getLogger(MinaAsyncTerminalSession.class);

    private final ClientSession clientSession;
    private final SshReaderThreadManager threadManager;
    private ChannelShell shellChannel;
    private OutputStream shellInput;

    /**
     * Constructs a MinaAsyncTerminalSession.
     *
     * @param connection the underlying SSH connection
     * @param clientSession the Mina client session
     * @param threadManager the thread manager for async reading
     */
    public MinaAsyncTerminalSession(SshConnection connection, ClientSession clientSession,
                                     SshReaderThreadManager threadManager) {
        super(connection);
        this.clientSession = clientSession;
        this.threadManager = threadManager;
    }

    /**
     * Starts the shell on the remote server.
     *
     * @throws IOException if the shell cannot be started
     */
    public void startShell() throws IOException {
        shellChannel = clientSession.createShellChannel();
        this.channel = shellChannel;

        // Set up streams for async reading
        shellChannel.setOut(stdoutStream);
        shellChannel.setErr(stderrStream);

        // Request PTY
        shellChannel.setPtyType("vt100");
        shellChannel.setPtyColumns(2200);
        shellChannel.setPtyLines(0);
        shellChannel.setPtyWidth(2200);
        shellChannel.setPtyHeight(0);

        // Open the channel
        shellChannel.open().verify(10, TimeUnit.SECONDS);

        // Get the input stream for sending commands
        shellInput = shellChannel.getInvertedIn();
    }

    @Override
    public TerminalSession executeCommand(String command) throws IOException {
        if (shellInput == null) {
            throw new IOException("Shell not started. Call startShell() first.");
        }

        shellInput.write((command + "\n").getBytes());
        shellInput.flush();

        return this;
    }

    @Override
    public void close() {
        if (readerThread != null) {
            readerThread.releaseSession(this);
        }
        super.close();
    }

    @Override
    protected void readStreamMessage() throws IOException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
        readAvailableOutput();
    }
}
