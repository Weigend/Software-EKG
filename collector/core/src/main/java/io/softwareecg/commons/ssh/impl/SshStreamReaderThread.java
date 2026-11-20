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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Thread that manages reading from multiple async SSH sessions.
 *
 * @since 10.03.13
 */
public class SshStreamReaderThread extends Thread {

    private final Set<MinaAbstractSession> asyncSessions = new HashSet<>();

    private final Set<MinaAbstractSession> sessionsToRelease = new HashSet<>();

    /**
     * Default Constructor
     */
    public SshStreamReaderThread() {
        // nothing to do here
    }

    /**
     * Adds an async session to be managed by this thread.
     *
     * @param session the session to manage
     */
    public final void addAsyncSession(MinaAbstractSession session) {
        asyncSessions.add(session);
        session.setOwnerThread(this);
    }

    /**
     * Removes a session from management. The removal is deferred to avoid
     * concurrent modification issues.
     *
     * @param session the session to release
     */
    public final void releaseSession(MinaAbstractSession session) {
        synchronized (sessionsToRelease) {
            sessionsToRelease.add(session);
        }
    }

    @Override
    public final void run() {
        while (true) {
            if (isInterrupted()) {
                return;
            }

            synchronized (sessionsToRelease) {
                if (!sessionsToRelease.isEmpty()) {
                    asyncSessions.removeAll(sessionsToRelease);
                    sessionsToRelease.clear();
                }
            }

            for (Iterator<MinaAbstractSession> sessionIterator = asyncSessions.iterator(); sessionIterator.hasNext();) {
                MinaAbstractSession session = sessionIterator.next();
                try {
                    session.readStreamMessage();
                } catch (IOException e) {
                    if (!session.isOpen()) {
                        sessionIterator.remove();
                    }
                }
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
