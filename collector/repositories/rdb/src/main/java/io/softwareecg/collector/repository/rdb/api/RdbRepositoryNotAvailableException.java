package io.softwareecg.collector.repository.rdb.api;

/**
 * Specialized type of the {@link RdbRepositoryException} that will thrown
 * than the repository that is the base for a query/request isn't reachable
 * at the moment.
 *
 * @since 18.10.2018
 */
public class RdbRepositoryNotAvailableException extends RdbRepositoryException {

    /**
     * Constructs a new instance of RdbRepositoryNotAvailableException
     *
     * @param cause the root cause like java.net.ConnectException
     */
    public RdbRepositoryNotAvailableException(Throwable cause) {
        super(cause);
    }
}
