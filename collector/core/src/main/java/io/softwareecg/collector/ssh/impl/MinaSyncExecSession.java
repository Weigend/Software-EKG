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
package io.softwareecg.collector.ssh.impl;

import io.softwareecg.collector.ssh.ExecSession;
import io.softwareecg.collector.ssh.FrameDetector;
import io.softwareecg.collector.ssh.SshConnection;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Synchronous ExecSession implementation using Apache Mina SSHD.
 * Executes commands and blocks until completion.
 *
 * @since 10.03.13
 */
public class MinaSyncExecSession extends MinaAbstractSession implements ExecSession {

    private static final Logger LOG = LoggerFactory.getLogger(MinaSyncExecSession.class);

    private static final long DEFAULT_TIMEOUT_MS = 60000;

    private final ClientSession clientSession;
    private long executionTimeout = 5000;

    /**
     * Constructs a MinaSyncExecSession.
     *
     * @param command the command to execute
     * @param connection the underlying SSH connection
     * @param clientSession the Mina client session
     */
    public MinaSyncExecSession(String command, SshConnection connection, ClientSession clientSession) {
        super(connection);
        this.command = command;
        this.clientSession = clientSession;
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
            ChannelExec execChannel = clientSession.createExecChannel(command);
            this.channel = execChannel;

            // Set up streams
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            execChannel.setOut(stdout);
            execChannel.setErr(stderr);

            // Request PTY for terminal-like behavior
            execChannel.setPtyType("vt100");
            execChannel.setPtyColumns(2200);
            execChannel.setPtyLines(0);
            execChannel.setPtyWidth(2200);
            execChannel.setPtyHeight(0);

            // Open the channel
            execChannel.open().verify(executionTimeout, TimeUnit.MILLISECONDS);

            // Wait for the channel to close (command completion)
            Set<ClientChannelEvent> events = execChannel.waitFor(
                    EnumSet.of(ClientChannelEvent.CLOSED, ClientChannelEvent.EOF),
                    executionTimeout);

            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                throw new IOException("Unable to receive terminal data within the time limit.");
            }

            // Read the output
            stdOutputLastComplete = stdout.toString();
            errorOut = stderr.toString();

            // Add lines to captured lines
            if (stdOutputLastComplete != null) {
                for (String line : stdOutputLastComplete.split("(\r\n|\n)")) {
                    appendCapturedLine(line);
                }
            }

            commandExecuted = true;
            return this;

        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Sets the execution timeout in milliseconds.
     *
     * @param timeoutInMillis the timeout in milliseconds
     */
    public void setExecutionTimeout(long timeoutInMillis) {
        this.executionTimeout = timeoutInMillis;
    }

    @Override
    public void setFrameDetector(FrameDetector frameDetector) {
        // Disable frame detector for synchronous calls - it doesn't make sense
    }
}
