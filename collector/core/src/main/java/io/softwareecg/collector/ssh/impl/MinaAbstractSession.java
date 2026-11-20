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

import io.softwareecg.collector.ssh.FrameBounce;
import io.softwareecg.collector.ssh.FrameDetector;
import io.softwareecg.collector.ssh.Session;
import io.softwareecg.collector.ssh.SshConnection;
import org.apache.sshd.client.channel.ClientChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Base class for all Mina-based Session implementations.
 *
 * @since 10.03.13
 */
public abstract class MinaAbstractSession implements Session {

    protected final SshConnection underlyingConnection;
    protected ClientChannel channel;

    protected String stdOutputLastComplete;
    protected String stdOutputFrameDiff = "";
    protected String errorOut;

    protected boolean isClosed;
    protected FrameDetector frameDetector;
    protected int maxCapturedLineLimit = MAX_LINES_DEFAULT_LIMIT;

    @SuppressWarnings("PMD.LooseCoupling")
    protected final LinkedList<String> capturedLines = new LinkedList<>();

    protected SshStreamReaderThread readerThread;
    protected String command;

    // Streams for reading output
    protected ByteArrayOutputStream stdoutStream;
    protected ByteArrayOutputStream stderrStream;

    /**
     * Constructs a MinaAbstractSession with the given SshConnection.
     *
     * @param connection the SshConnection this session belongs to
     */
    public MinaAbstractSession(SshConnection connection) {
        this.underlyingConnection = connection;
        this.stdoutStream = new ByteArrayOutputStream();
        this.stderrStream = new ByteArrayOutputStream();
    }

    @Override
    public String getFullStdOutput() {
        return stdOutputLastComplete;
    }

    @Override
    public Collection<String> getLineBasedStdOutput() {
        synchronized (capturedLines) {
            ArrayList<String> capturedLinesCopy = new ArrayList<>(capturedLines);
            capturedLines.clear();
            return capturedLinesCopy;
        }
    }

    @Override
    public void setMaxCapturedLines(int maxCapturedLines) {
        this.maxCapturedLineLimit = maxCapturedLines;
    }

    @Override
    public String getErrorOutput() {
        return errorOut;
    }

    @Override
    public boolean hasErrorOutput() {
        return errorOut != null && !errorOut.trim().isEmpty();
    }

    @Override
    public boolean isOpen() {
        if (isClosed) {
            return false;
        }
        // If channel is not yet created (before executeCommand), session is still "open"
        if (channel == null) {
            return true;
        }
        return channel.isOpen();
    }

    @Override
    public void close() {
        if (!isClosed) {
            underlyingConnection.release(this);
            if (channel != null) {
                try {
                    // Use graceful close (false) to send SSH_MSG_CHANNEL_CLOSE to the server.
                    // The default close() uses close(true) which skips sending the protocol
                    // message, causing the server to accumulate zombie channels until MaxSessions.
                    channel.close(false).await(5000L);
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
            stdOutputLastComplete = null;
            errorOut = null;
            isClosed = true;
        }
    }

    @Override
    public void setFrameDetector(FrameDetector frameDetector) {
        this.frameDetector = frameDetector;
    }

    @Override
    public void waitForData(int timeoutInMillis) throws IOException {
        int maxCycles = timeoutInMillis / 10;

        for (int cycle = 0; cycle < maxCycles; cycle++) {
            if (stdOutputLastComplete != null && !stdOutputLastComplete.trim().isEmpty()) {
                return;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for data", e);
            }
        }

        throw new IOException("Received no data during the given timeout");
    }

    /**
     * Sets the owner thread that manages the streams.
     *
     * @param readerThread the stream reader thread
     */
    protected void setOwnerThread(SshStreamReaderThread readerThread) {
        this.readerThread = readerThread;
    }

    /**
     * Reads stream messages and processes them.
     *
     * @throws IOException if reading fails
     */
    protected void readStreamMessage() throws IOException {
        readAvailableOutput();
    }

    /**
     * Reads all available output from the streams.
     */
    protected void readAvailableOutput() {
        // Read stdout
        if (stdoutStream.size() > 0) {
            String tmp = stdoutStream.toString();
            stdoutStream.reset();

            if (frameDetector == null) {
                stdOutputLastComplete = tmp;
            } else {
                processWithFrameDetector(tmp);
            }

            // Add to captured lines
            for (String line : tmp.split("(\r\n|\n)")) {
                appendCapturedLine(line);
            }
        }

        // Read stderr
        if (stderrStream.size() > 0) {
            errorOut = stderrStream.toString();
            stderrStream.reset();
        }
    }

    /**
     * Process output with frame detector.
     */
    private void processWithFrameDetector(String tmp) {
        String concatedTmp = stdOutputFrameDiff + tmp;
        FrameBounce frameBounce = frameDetector.detectFrameBounces(concatedTmp);

        if (frameBounce.getLastCompleteFrameStart() > -1 && frameBounce.getLastFrameEnd() > -1) {
            stdOutputLastComplete = concatedTmp.substring(
                    frameBounce.getLastCompleteFrameStart(), 
                    frameBounce.getLastFrameEnd());
            stdOutputFrameDiff = concatedTmp.substring(frameBounce.getLastFrameEnd());
        } else if (frameBounce.getLastCompleteFrameStart() > -1) {
            stdOutputFrameDiff = concatedTmp.substring(frameBounce.getLastCompleteFrameStart());
        } else if (frameBounce.getLastFrameEnd() > -1) {
            stdOutputFrameDiff = concatedTmp.substring(frameBounce.getLastFrameEnd());
        } else {
            stdOutputFrameDiff += tmp;
        }
    }

    /**
     * Appends a line to the captured lines buffer.
     *
     * @param line the line to append
     */
    protected void appendCapturedLine(String line) {
        synchronized (capturedLines) {
            if (capturedLines.size() >= maxCapturedLineLimit) {
                capturedLines.removeFirst();
            }
            capturedLines.add(line);
        }
    }

    /**
     * Read all available bytes from an input stream into a byte array.
     *
     * @param inputStream the input stream to read from
     * @return the bytes read
     * @throws IOException if reading fails
     */
    protected byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int available = inputStream.available();
        if (available > 0) {
            byte[] data = new byte[available];
            int bytesRead = inputStream.read(data);
            if (bytesRead > 0) {
                buffer.write(data, 0, bytesRead);
            }
        }
        return buffer.toByteArray();
    }
}
