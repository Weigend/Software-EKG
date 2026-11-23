package io.softwareecg.awb.infrastructure.ssh;

import java.io.IOException;

/**
 * Exception for SSH connection and authentication failures.
 *
 */
public class SshException extends IOException {

    public SshException(String message) {
        super(message);
    }

    public SshException(String message, Throwable cause) {
        super(message, cause);
    }

    public SshException(Throwable cause) {
        super(cause);
    }
}
