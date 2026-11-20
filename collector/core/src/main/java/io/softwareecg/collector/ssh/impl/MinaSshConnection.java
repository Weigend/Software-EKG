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

import io.softwareecg.collector.ssh.*;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A <code>SshConnection</code> is used to establish an encrypted TCP/IP connection to a SSH-2 server.
 * Based on an open SshConnection you can open synchronous or asynchronous sessions or port forwardings.
 * <p/>
 * Uses Apache Mina SSHD as the underlying SSH implementation.
 *
 * @since 10.03.13
 */
public class MinaSshConnection implements SshConnection {

    private static final Logger LOG = LoggerFactory.getLogger(MinaSshConnection.class);

    private static final int CONNECTION_TIMEOUT_MS = 10000;

    private final SshClient sshClient;
    private ClientSession session;

    private final String host;
    private final int port;
    private final String userName;
    private final String password;
    private final File privateKey;
    private final String keyPassphrase;

    private final AtomicBoolean isAuthenticated = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    private final Collection<Session> managedSessions = new HashSet<>();
    private final SshReaderThreadManager threadManager;

    /**
     * Constructs a MinaSshConnection with password authentication.
     *
     * @param threadManager a SshReaderThreadManager instance for managing async connections
     * @param host the host where the SSH service is running
     * @param port the port where the SSH service is listening
     * @param userName the username to authenticate
     * @param password the password to authenticate
     */
    public MinaSshConnection(SshReaderThreadManager threadManager, String host, int port, String userName, String password) {
        this.threadManager = threadManager;
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.privateKey = null;
        this.keyPassphrase = null;

        this.sshClient = SshClient.setUpDefaultClient();
        this.sshClient.start();
    }

    /**
     * Constructs a MinaSshConnection with key-based authentication.
     *
     * @param threadManager a SshReaderThreadManager instance for managing async connections
     * @param host the host where the SSH service is running
     * @param port the port where the SSH service is listening
     * @param userName the username to authenticate
     * @param privateKey the private key file for authentication
     * @param keyPassphrase the passphrase for the private key (may be null)
     */
    public MinaSshConnection(SshReaderThreadManager threadManager, String host, int port, String userName, 
                             File privateKey, String keyPassphrase) {
        this.threadManager = threadManager;
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = null;
        this.privateKey = privateKey;
        this.keyPassphrase = keyPassphrase;

        this.sshClient = SshClient.setUpDefaultClient();
        this.sshClient.start();
    }

    @Override
    public PortForwarder createPortForwarding(int localPort, String targetHostIp, int targetHostPort) 
            throws NoConnectionException {
        try {
            ensureConnection();
            SshdSocketAddress local = new SshdSocketAddress("127.0.0.1", localPort);
            SshdSocketAddress remote = new SshdSocketAddress(targetHostIp, targetHostPort);
            SshdSocketAddress bound = session.startLocalPortForwarding(local, remote);
            
            // Store reference to bound address for stopping
            SshdSocketAddress boundLocal = new SshdSocketAddress("127.0.0.1", bound.getPort());
            
            // Create a closeable that stops the port forwarding
            Closeable forwarder = () -> {
                try {
                    session.stopLocalPortForwarding(boundLocal);
                } catch (IOException e) {
                    LOG.warn("Error stopping port forwarding", e);
                }
            };
            return new PortForwarder(forwarder, bound.getPort(), targetHostIp, targetHostPort);
        } catch (IOException e) {
            throw new NoConnectionException(e);
        }
    }

    @Override
    public ExecSession openSynchronousExecSession(String command) throws NoConnectionException {
        try {
            checkCommandAndConnection(command);
            MinaSyncExecSession execSession = new MinaSyncExecSession(command, this, session);
            managedSessions.add(execSession);
            return execSession;
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid command", e);
            throw e;
        } catch (IOException e) {
            close();
            LOG.error("Failed to open exec session", e);
            throw new NoConnectionException(e);
        }
    }

    @Override
    public ScpClient createScpClient() {
        try {
            ensureConnection();
            return new ScpClient(session);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create SCP client", e);
        }
    }

    @Override
    public ExecSession openAsynchronousExecSession(String command) throws NoConnectionException {
        try {
            checkCommandAndConnection(command);
            MinaAsyncExecSession execSession = new MinaAsyncExecSession(command, this, session, threadManager);
            managedSessions.add(execSession);
            threadManager.getReaderThread().addAsyncSession(execSession);
            return execSession;
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid command", e);
            throw e;
        } catch (Exception e) {
            throw new NoConnectionException(e);
        }
    }

    @Override
    public TerminalSession openAsynchronousTerminalSession() throws NoConnectionException {
        try {
            ensureConnection();
            MinaAsyncTerminalSession terminalSession = new MinaAsyncTerminalSession(this, session, threadManager);
            terminalSession.startShell();
            managedSessions.add(terminalSession);
            threadManager.getReaderThread().addAsyncSession(terminalSession);
            return terminalSession;
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid terminal session", e);
            throw e;
        } catch (Exception e) {
            throw new NoConnectionException(e);
        }
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated.get();
    }

    @Override
    public void connect() throws NoConnectionException, AuthenticationFailedException {
        try {
            // Reinitialize SshClient if it was closed or not started
            if (sshClient.isClosed() || !sshClient.isStarted()) {
                // Can't restart a closed client, but we can continue if it's just not started
                if (!sshClient.isClosed() && !sshClient.isStarted()) {
                    sshClient.start();
                }
            }
            
            // Validate username - Apache Mina SSHD requires it
            String effectiveUserName = userName != null ? userName : "";
            
            session = sshClient.connect(effectiveUserName, host, port)
                    .verify(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .getSession();

            if (privateKey != null) {
                // Key-based authentication
                if (!privateKey.exists()) {
                    throw new AuthenticationFailedException("Private key file does not exist: " + privateKey.getAbsolutePath());
                }
                try {
                    org.apache.sshd.common.util.security.SecurityUtils.loadKeyPairIdentities(
                            null,
                            (org.apache.sshd.common.util.io.resource.PathResource) new org.apache.sshd.common.util.io.resource.PathResource(privateKey.toPath()),
                            new java.io.FileInputStream(privateKey),
                            (session1, resourceKey, retryIndex) -> keyPassphrase
                    ).forEach(session::addPublicKeyIdentity);
                } catch (GeneralSecurityException | java.io.FileNotFoundException e) {
                    throw new AuthenticationFailedException("Failed to load private key", e);
                }
            } else if (password != null && !password.isEmpty()) {
                // Password authentication
                session.addPasswordIdentity(password);
            }
            
            // Track whether we have credentials to authenticate
            boolean hasCredentials = (privateKey != null) || (password != null && !password.isEmpty());

            try {
                session.auth().verify(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                // Only mark as authenticated if we actually provided credentials
                isAuthenticated.set(hasCredentials && session.isAuthenticated());
            } catch (IOException authError) {
                // Authentication failed, but connection established
                isAuthenticated.set(false);
            }
            isConnected.set(true);

        } catch (AuthenticationFailedException e) {
            throw e;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("auth")) {
                throw new AuthenticationFailedException(e);
            }
            throw new NoConnectionException("Initializing connection failed", e);
        }
    }

    @Override
    public void close() {
        // Create a copy to avoid ConcurrentModificationException
        Collection<Session> sessionsToClose = new HashSet<>(managedSessions);
        for (Session managedSession : sessionsToClose) {
            managedSession.close();
        }
        managedSessions.clear();

        isConnected.set(false);
        isAuthenticated.set(false);

        try {
            if (session != null) {
                session.close();
            }
        } catch (IOException e) {
            LOG.warn("Error closing session", e);
        }

        sshClient.stop();
    }

    @Override
    public void release(Session managedSession) {
        managedSessions.remove(managedSession);
    }

    /**
     * Returns the underlying ClientSession.
     *
     * @return the ClientSession
     */
    ClientSession getSession() {
        return session;
    }

    private void checkCommandAndConnection(String command) throws NoConnectionException, AuthenticationFailedException {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("The given command is blank");
        }
        ensureConnection();
    }

    private void ensureConnection() throws NoConnectionException, AuthenticationFailedException {
        if (!isConnected.get()) {
            connect();
        }
    }
}
