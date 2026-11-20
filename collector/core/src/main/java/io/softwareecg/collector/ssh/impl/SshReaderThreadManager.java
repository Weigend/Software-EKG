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

/**
 * @since 11.03.13
 */
public class SshReaderThreadManager {

    private final SshStreamReaderThread readerThread = new SshStreamReaderThread();

    private boolean isStarted;

    /**
     * Returns the SshStreamReaderThread that manage the streams
     * of async SSH connections
     *
     * @return an instance of SshStreamReaderThread
     */
    public final SshStreamReaderThread getReaderThread() {

        if (!isStarted) {
            isStarted = true;
            readerThread.start();
            readerThread.setName("SshStreamReaderThread_#1");
        }

        return readerThread;
    }

    /**
     * todo implement
     * 0 = unlimited (each Session gets it's own thread)
     * 1-n = the max pool size
     *
     * @param size the thread pool size
     */
    public void setThreadPoolSize(int size) {

    }
}
