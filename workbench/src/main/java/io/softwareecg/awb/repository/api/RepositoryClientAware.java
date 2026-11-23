package io.softwareecg.awb.repository.api;

import io.softwareecg.awb.timeseries.port.TimeSeriesRepository;

/**
 * Interface implemented by all services that need an EKG repository client
 * to work properly.
 *
 * @since 18.08.2018
 */
public interface RepositoryClientAware {

    void initializeService(TimeSeriesRepository client);

    /**
     * Sets the repository ID this service is bound to.
     * Override in implementations that need to filter by repository.
     *
     * @param repositoryId the unique repository ID
     */
    default void setRepositoryId(String repositoryId) {
        // default no-op
    }
}
