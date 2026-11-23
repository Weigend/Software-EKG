package io.softwareecg.awb.repository.api;

/**
 * Signals errors with accessing the search index.
 *
 * @since 30.08.2015
 */
public class RepositoryException extends Exception {

    /**
     * Constructs a new {@link RepositoryException} with the specified cause.
     * See {@link Exception#Exception(Throwable)}.
     *
     * @param cause the cause
     */
    public RepositoryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@link RepositoryException} with the specified message.
     * See {@link Exception#Exception(String)}.
     *
     * @param message the message
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link RepositoryException} with the specified message.
     * See {@link Exception#Exception(String, Throwable)}.
     *
     * @param message the message
     * @param cause   the cause
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
