package io.softwareecg.awb.timeseries.port;

import io.softwareecg.awb.timeseries.port.RepositoryException;
import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.stream.Stream;

/**
 * The TimeSeriesWriteService interface represents specialized services for
 * importers with a set of methods designed to write series data into the
 * EKG repository and query information's need to insert (delta) data.
 *
 * @since 31.01.2020
 */
public interface TimeSeriesWriteService {

    /**
     * Returns the stable identifier of the repository configured for
     * this TimeSeriesWriteService.
     *
     * @return the configured repository identifier
     */
    String getRepositoryId();

    /**
     * Uses the filled filter dimensions of the given series instance to search
     * for already existing ones in the repository. If exists timestamp of the
     * last series entry will returned.
     *
     * If no matching series exists -1 will returned instead.
     * This method doesn't change any data and no values required inside the given
     * series instance.
     *
     * @param seriesWithoutValues a TimeSeries instance that will used find similar oder series.
     * @return an UTC timestamp there the a existing matching series ends or -1 if no matching series exists.
     */
    long resolveLastUpdate(TimeSeries seriesWithoutValues) throws RepositoryException;

    /**
     * Add a single TimeSeries instance to the repository.
     *
     * @param series the series that should add to the repository.
     * @throws RepositoryException on errors accessing the repository (for example if it's down or on schema errors)
     */
    void add(TimeSeries series) throws RepositoryException;

    /**
     * Adds all {@link TimeSeries} instances that are provided as a stream to the series repository.
     * The service guaranteed thread-safety so both normal and parallel stream can be used to import the data.
     *
     * @param series a stream of TimeSeries that have to be imported
     * @throws RepositoryException on errors accessing the repository (for example if it's down or on schema errors)
     */
    void add(Stream<TimeSeries> series) throws RepositoryException;

    /**
     * Adds all {@link TimeSeries} instances that are provided as Iterable to the series repository.
     * The iterable will import sequential. Use parallel streams to get profit from better import performance.
     *
     * @param series set of TimeSeries provided as Iterable
     * @throws RepositoryException on errors accessing the repository (for example if it's down or on schema errors)
     */
    void add(Iterable<TimeSeries> series) throws RepositoryException;

    /**
     * Commits the whole data that were added before.
     * The caller has to ensure that adding data and commit it should happen at the same time.
     *
     * @throws RepositoryException on errors accessing the repository (for example if it's down or on schema errors)
     */
    void commit() throws RepositoryException;


}
