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

import java.io.IOException;
import java.util.Collection;

/**
 * A <code>Session</code> is a remote execution of a program. "Program" means
 * in this context either a shell, an application or a system command. The
 * program may or may not have a tty. Only one single program can be started on
 * a session. However, multiple sessions can be active simultaneously.
 *
 * @since 04.12.13
 */
public interface Session {

    /**
     * The default limit for the max amount of lines the
     * session capture before overwriting the oldest ones
     */
    int MAX_LINES_DEFAULT_LIMIT = 1000;

    /**
     * Returns the standard output of the last command execution.
     * In case of asynchronous calls this method my return empty or unexpected content.
     * In this case you should use a FrameDetector instance to ensure the return value
     * contains nothing or the a complete screen-output (frame).
     *
     * @return the output from the command at the time of the call
     */
    String getFullStdOutput();

    /**
     * Returns all unread lines fetched from standard output after
     * the command execution.
     * Returning unread lines means the delta output since command execution and
     * the last call of this method. It can compared with an non-blocking tail command.
     * To optimize performance and preventing memory leaks the maximum amount of
     * captures lines is set to 1000 entries. This default limit can modified by
     * setMaxCapturesLines(). If the limit is reached, the oldest lines will overwritten
     * by newer ones.
     *
     * @return a collection of lines from standard output
     */
    Collection<String> getLineBasedStdOutput();

    /**
     * Sets the max amount of line the session will capture from stream before
     * overwriting the oldest ones. Be caution: to increase the default limit
     * can cause memory leaks!
     *
     * @param maxCapturedLines the maximum amount of line that will captured
     */
    void setMaxCapturedLines(int maxCapturedLines);

    /**
     * Returns the error output of the last command execution if available.
     * If no the command doesn't write any content int error out, this method
     * will return null
     *
     * @return the error output or <code>null</code>
     */
    String getErrorOutput();

    /**
     * Return a boolean value if the called command wrote something
     * to error stream or not.
     *
     * @return <code>true</code> if the executed command has produced error output. Otherwise <code>false</code>
     */
    boolean hasErrorOutput();

    /**
     * Checks if the underlying connection and this session is still open and
     * ready to execute commands.
     *
     * @return <code>true</code> if the SSH connection is still alive and the session is open. Otherwise <code>false</code>
     */
    boolean isOpen();

    /**
     * Close the current session without any effect to the underlying session
     * After closing the session no command could execute anymore. If the session is
     * managed by a stream reader thread, it will released.
     */
    void close();

    /**
     * Set the FrameDetector instance that will used to find the correct beginning and ends of
     * the wanted output frames. If no FrameDetector set, the StreamReader writes command output
     * in the order of the incoming stream data without any awareness for fragmentation
     *
     * @param frameDetector a FrameDetector implementation that scan the stream for start end ends of fames
     */
    void setFrameDetector(FrameDetector frameDetector);

    /**
     * Wait the given time in milliseconds for the execution of the commands and returning
     * terminal output. This method is only required using AsyncSessions. It will block during
     * wait for output. If no output available in the given time an IOException will thrown.
     *
     * @param timeoutInMillis the timeout to wait for output in milliseconds
     * @throws IOException thrown if no data available
     */
    void waitForData(int timeoutInMillis) throws IOException;
}
