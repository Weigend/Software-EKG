package io.softwareecg.awb.timeseries.port;

import io.softwareecg.awb.timeseries.port.RepositoryException;

/**
 * Specialized type of the {@link RepositoryException} that will thrown
 * than the repository that is the base for a query/request isn't reachable
 * at the moment.
 *
 * @since 18.10.2018
 */
public class RepositoryNotAvailableException extends RepositoryException {

    private final Cause cause;

    /**
     * Constructs a new instance of RepositoryNotAvailableException
     *
     * @param cause an enum that details the cause why the repository isn't available
     * @param rootException the root cause like java.net.ConnectException
     */
    public RepositoryNotAvailableException(Cause cause, Throwable rootException) {
        super(rootException);

        this.cause = cause;
    }

    public Cause getNotAvailableCause() {
        return cause;
    }

    @Override
    public String getMessage() {
        if (cause == Cause.INDEX_NOT_AVAILABLE) {
            return "Repository not found.";
        } else {
            return super.getMessage();
        }
    }


    public enum Cause {

        HOST_NOT_AVAILABLE,

        INDEX_NOT_AVAILABLE
    }
}
