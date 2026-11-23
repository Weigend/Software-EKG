package io.softwareecg.awb.repository.spi;

import io.softwareecg.awb.repository.port.TimeSeriesAdmin;
import io.softwareecg.awb.repository.port.TimeSeriesRepository;

/**
 * Provider that creates and manages {@link TimeSeriesRepository}s and {@link TimeSeriesAdmin}s.
 *
 * @since 13.02.2018
 */
public interface TimeSeriesAdminProvider {

    /**
     * Returns a {@link TimeSeriesRepository} bound to the given collection. Make sure to
     * {@link TimeSeriesRepository#close()} the client after usage.
     *
     * @param collection the collection
     * @return the {@link TimeSeriesRepository}
     */
    TimeSeriesRepository getClient(String collection);

    /**
     * Returns a {@link TimeSeriesAdmin}. Make sure to {@link TimeSeriesAdmin#close()} the client after
     * usage.
     *
     * @return the {@link TimeSeriesAdmin}.
     */
    TimeSeriesAdmin getAdminClient();
}
