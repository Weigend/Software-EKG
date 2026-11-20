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
package io.softwareecg.commons.ssh;

/**
 * The SshConnection interface definition defines operations to work with hosts via SSH.
 *
 */
public interface SshConnection {
    /**
     * Creates a new {@link io.softwareecg.commons.ssh.PortForwarder}. A <code>PortForwarder</code> forwards TCP/IP
     * connections that arrive at a local port via the secure tunnel to another host (which may or may
     * not be identical to the remote SSH-2 server).
     * <p/>
     * This method must only be called after one has passed successfully the authentication step.
     * There is no limit on the number of concurrent forwarding's.
     *
     * @param localPort      the local port the LocalPortForwarder shall bind to.
     * @param targetHostIp   target address (IP or hostname)
     * @param targetHostPort target port
     * @return A {@link io.softwareecg.commons.ssh.PortForwarder} object.
     * @throws io.softwareecg.commons.ssh.NoConnectionException thrown if the connection isn't connected anymore
     */
    PortForwarder createPortForwarding(int localPort, String targetHostIp, int targetHostPort) throws NoConnectionException;

    /**
     * Returns a session that can used to execute a command directly on target host without
     * open a (pseudo) shell. In difference to terminal session the output during command
     * execution includes only the standard output from the application or script.
     * <p/>
     * Synchronous ExecSession's will block on executeCommand() invocations until the remote
     * process has finished.
     * Use that method to get a Session that should use top execute process that have a defined
     * end (like "dir" or "top -X"). To call infinity running programs use openAsynchronousExecSession()
     *
     * @param command the command including parameters that should execute on executeCommand() invocation
     * @return an instance of ExecSession that can use to execute the binded command
     * @throws io.softwareecg.commons.ssh.NoConnectionException thrown if the underlying connection isn't able to connect or isn't connected anymore
     */
    ExecSession openSynchronousExecSession(String command) throws NoConnectionException;

    /**
     * Returns a simple ScpClient that can use to fetch files from remote server or put files to it.
     * You have to initialize a authenticated connection before you can use this client.
     *
     * @return a ScpClient to proceed file transfers
     */
    ScpClient createScpClient();

    /**
     * Returns an ExecSession that can used to execute a command directly on target system without
     * open a (pseudo) shell. In difference to TerminalSessions the output after command
     * execution includes only the standard output from the application or script itself.
     * <p/>
     * Asynchronous ExecSession's will not block on executeCommand() invocations. You can continue
     * the program flow and poll to getFullStdOutput() to get the results of the execution.
     * Use that method to get a Session that should use top execute process that runs infinity
     * like "top" or "iostat"
     *
     * @param command the command including parameters that should execute on executeCommand() invocation
     * @return an instance of ExecSession that can use to execute asynchronously the binded command
     * @throws io.softwareecg.commons.ssh.NoConnectionException thrown if the underlying connection isn't able to connect or isn't connected anymore
     */
    ExecSession openAsynchronousExecSession(String command) throws NoConnectionException;

    /**
     * Returns a TerminalSession that can used to execute multiple command in the shell of the target system.
     * The session will open a Pseudo-Shell automatically. In difference to ExecSessions the output after command
     * execution includes shell outputs (like current path) and the standard output from the application
     * or script itself.
     * <p/>
     * Asynchronous TerminalSession's will not block on executeCommand() invocations. You can continue
     * the program flow and poll to getFullStdOutput() to get the results of the execution. Synchronous
     * TerminalSession won't work because the output from remote shell always blocks infinity
     * (or until connection closed)
     *
     * @return an instance of TerminalSession that can use to execute the commands in a fluent way
     * @throws io.softwareecg.commons.ssh.NoConnectionException thrown if the underlying connection isn't connected anymore
     */
    TerminalSession openAsynchronousTerminalSession() throws NoConnectionException;

    /**
     * Returns a boolean flag if the connection was successfully
     * authenticated or not.
     *
     * @return return <code>true</code> if the connection is authenticated. Otherwise <code>false</code>
     */
    boolean isAuthenticated();

    /**
     * Connect to configured SSH server and try to authenticate the connection.
     * If the connection or authentication fails a NoConnectionException will thrown
     * that provides further details
     *
     * @throws io.softwareecg.commons.ssh.NoConnectionException         if connection fails
     * @throws io.softwareecg.commons.ssh.AuthenticationFailedException if authentication fails
     */
    void connect() throws NoConnectionException, AuthenticationFailedException;

    /**
     * Close all open session and than the connection to the SSH server.
     * During closing the sessions all outputs will reset.
     * After calling this method you have to create a new connection before
     * you will able to open new session or execute a command.
     */
    void close();

    /**
     * Removes the session from the managed session
     *
     * @param session the session to release
     */
    void release(Session session);
}
