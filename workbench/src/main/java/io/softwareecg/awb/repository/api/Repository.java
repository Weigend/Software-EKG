package io.softwareecg.awb.repository.api;

import io.softwareecg.awb.repository.connection.EkgRepository;


/**
 * A repository as the other components see it: who it is, what it answers, and the services that
 * work on its data. How it is connected - address, credentials, client - is in
 * {@link EkgRepository} and stays inside the component.
 *
 * @since 30.01.2020
 */
public interface Repository {

    /**
     * Get the unique types id within the currently used types config storage.
     *
     * @return The unique types id.
     */
    String getId();

    /**
     * Returns the name of this types.
     * <p>
     * In some cases the name may be user defined.
     *
     * @return the types name.
     */
    String getRepositoryName();

    /**
     * @return whether this is the repository inside the workbench
     */
    boolean isEmbedded();

    /**
     * Returns a service that works on the data of this repository, and on no other.
     *
     * @param serviceInterface the service
     * @return the service, or {@code null} if the repository cannot be reached
     */
    <T> T getBoundedService(Class<T> serviceInterface);

    /**
     * Asks the repository whether it answers.
     */
    boolean isReachable();

    /**
     * @return the number of values stored in the repository
     * @throws RepositoryException if the repository cannot tell
     */
    long countValues() throws RepositoryException;

    /**
     * @return the number of values of a project that are stored in the repository
     * @throws RepositoryException if the repository cannot tell
     */
    long countValuesOf(String projectName) throws RepositoryException;

    /**
     * @return the series that are stored in the repository
     */
    SeriesSearch series();

    /**
     * Deletes the time series of a project. The project itself stays.
     *
     * @throws RepositoryException if the repository could not delete them
     */
    void deleteTimeSeriesOf(String projectName) throws RepositoryException;
}
