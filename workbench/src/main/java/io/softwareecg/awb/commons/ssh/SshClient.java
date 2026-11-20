package io.softwareecg.awb.commons.ssh;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal SSH client for synchronous command execution on remote hosts.
 * Supports password and key-based authentication via Apache SSHD.
 * <p>
 * Usage:
 * <pre>
 *   try (SshClient client = new SshClient(host, 22, user, keyFile, null)) {
 *       client.connect();
 *       SshResult result = client.execute("uname -a");
 *       System.out.println(result.stdout());
 *   }
 * </pre>
 *
 */
public class SshClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SshClient.class);

    private static final int CONNECTION_TIMEOUT_MS = 10_000;
    private static final long EXEC_TIMEOUT_MS = 60_000;

    private final String host;
    private final int port;
    private final String userName;
    private final String password;
    private final File privateKey;
    private final String keyPassphrase;

    private org.apache.sshd.client.SshClient sshClient;
    private ClientSession session;
    private final AtomicBoolean authenticated = new AtomicBoolean(false);

    /**
     * Creates an SSH client with key-based authentication.
     *
     * @param host          the hostname or IP
     * @param port          the SSH port (typically 22)
     * @param userName      the username
     * @param privateKey    path to the private key file
     * @param keyPassphrase passphrase for the key (may be null)
     */
    public SshClient(String host, int port, String userName, File privateKey, String keyPassphrase) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = null;
        this.privateKey = privateKey;
        this.keyPassphrase = keyPassphrase;
    }

    /**
     * Creates an SSH client with password authentication.
     *
     * @param host     the hostname or IP
     * @param port     the SSH port (typically 22)
     * @param userName the username
     * @param password the password
     */
    public SshClient(String host, int port, String userName, String password) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.privateKey = null;
        this.keyPassphrase = null;
    }

    /**
     * Connects to the SSH server and authenticates.
     *
     * @throws SshException if connection or authentication fails
     */
    public void connect() throws SshException {
        try {
            sshClient = org.apache.sshd.client.SshClient.setUpDefaultClient();
            sshClient.start();

            String effectiveUser = userName != null ? userName : "";
            session = sshClient.connect(effectiveUser, host, port)
                    .verify(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .getSession();

            if (privateKey != null) {
                loadPrivateKey();
            } else if (password != null && !password.isEmpty()) {
                session.addPasswordIdentity(password);
            }

            session.auth().verify(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            authenticated.set(session.isAuthenticated());

        } catch (SshException e) {
            throw e;
        } catch (IOException e) {
            throw new SshException("SSH connection failed to " + host + ":" + port, e);
        }
    }

    /**
     * Executes a command synchronously and returns the result.
     *
     * @param command the shell command to execute
     * @return the execution result with stdout and stderr
     * @throws SshException if the command cannot be executed
     */
    public SshResult execute(String command) throws SshException {
        if (session == null || session.isClosed()) {
            throw new SshException("Not connected. Call connect() first.");
        }

        try {
            ChannelExec channel = session.createExecChannel(command);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            channel.setOut(stdout);
            channel.setErr(stderr);

            channel.setPtyType("vt100");
            channel.setPtyColumns(2200);
            channel.setPtyLines(0);
            channel.setPtyWidth(2200);
            channel.setPtyHeight(0);

            channel.open().verify(EXEC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            channel.waitFor(
                    EnumSet.of(ClientChannelEvent.CLOSED, ClientChannelEvent.EOF),
                    EXEC_TIMEOUT_MS);

            String stdoutStr = stdout.toString();
            String stderrStr = stderr.toString();

            try {
                channel.close(false).await(5000L);
            } catch (IOException e) {
                LOG.warn("Error closing SSH channel", e);
            }

            return new SshResult(stdoutStr, stderrStr);

        } catch (IOException e) {
            throw new SshException("SSH command execution failed: " + command, e);
        }
    }

    /**
     * Returns whether the connection is authenticated.
     */
    public boolean isAuthenticated() {
        return authenticated.get();
    }

    @Override
    public void close() {
        authenticated.set(false);
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                LOG.warn("Error closing SSH session", e);
            }
        }
        if (sshClient != null) {
            sshClient.stop();
        }
    }

    private void loadPrivateKey() throws SshException {
        if (!privateKey.exists()) {
            throw new SshException("Private key file does not exist: " + privateKey.getAbsolutePath());
        }
        try {
            SecurityUtils.loadKeyPairIdentities(
                    null,
                    new PathResource(privateKey.toPath()),
                    new FileInputStream(privateKey),
                    (s, resourceKey, retryIndex) -> keyPassphrase
            ).forEach(session::addPublicKeyIdentity);
        } catch (GeneralSecurityException | IOException e) {
            throw new SshException("Failed to load private key: " + privateKey.getAbsolutePath(), e);
        }
    }
}
