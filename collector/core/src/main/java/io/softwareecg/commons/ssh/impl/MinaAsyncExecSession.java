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

import io.softwareecg.commons.ssh.ExecSession;
import io.softwareecg.commons.ssh.SshConnection;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous ExecSession implementation using Apache Mina SSHD.
 * Executes commands without blocking.
 *
 * @since 10.03.13
 */
public class MinaAsyncExecSession extends MinaAbstractSession implements ExecSession {

    private static final Logger LOG = LoggerFactory.getLogger(MinaAsyncExecSession.class);

    private final ClientSession clientSession;
    private final SshReaderThreadManager threadManager;
    private ChannelExec execChannel;

    /**
     * Constructs a MinaAsyncExecSession.
     *
     * @param command the command to execute
     * @param connection the underlying SSH connection
     * @param clientSession the Mina client session
     * @param threadManager the thread manager for async reading
     */
    public MinaAsyncExecSession(String command, SshConnection connection, ClientSession clientSession,
                                 SshReaderThreadManager threadManager) {
        super(connection);
        this.command = command;
        this.clientSession = clientSession;
        this.threadManager = threadManager;
    }

    private boolean commandExecuted = false;

    @Override
    public ExecSession executeCommand() throws IOException {
        if (isClosed) {
            throw new IOException("Session is closed");
        }
        if (commandExecuted) {
            throw new IOException("Command has already been executed");
        }
        try {
            execChannel = clientSession.createExecChannel(command);
            this.channel = execChannel;

            // Set up streams for async reading
            execChannel.setOut(stdoutStream);
            execChannel.setErr(stderrStream);

            // Request PTY
            execChannel.setPtyType("vt100");
            execChannel.setPtyColumns(2200);
            execChannel.setPtyLines(0);
            execChannel.setPtyWidth(2200);
            execChannel.setPtyHeight(0);

            // Open the channel (non-blocking)
            execChannel.open().verify(10, TimeUnit.SECONDS);

            commandExecuted = true;
            return this;

        } catch (IOException e) {
            close();
            throw e;
        }
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
