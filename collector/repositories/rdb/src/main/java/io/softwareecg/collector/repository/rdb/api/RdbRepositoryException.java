package io.softwareecg.collector.repository.rdb.api;

/**
 * Signals errors with accessing the search index.
 *
 * @since 30.08.2015
 */
public class RdbRepositoryException extends Exception {

    /**
     * Constructs a new {@link RdbRepositoryException} with the specified cause.
     * See {@link Exception#Exception(Throwable)}.
     *
     * @param cause the cause
     */
    public RdbRepositoryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@link RdbRepositoryException} with the specified message.
     * See {@link Exception#Exception(String)}.
     *
     * @param message the message
     */
    public RdbRepositoryException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link RdbRepositoryException} with the specified message.
     * See {@link Exception#Exception(String, Throwable)}.
     *
     * @param message the message
     * @param cause   the cause
     */
    public RdbRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
