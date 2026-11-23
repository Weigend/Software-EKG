package io.softwareecg.awb.datasource.model;

import io.softwareecg.awb.timeseries.port.RepositoryException;

/**
 * Tells where the stored values of one series end. Collecting resumes there, series by series:
 * a series that is new starts at the beginning while the ones beside it go on where they are.
 */
@FunctionalInterface
public interface SeriesEnds {

    /**
     * @param projectName the project the series belongs to
     * @param measurement the measurement the series is stored under
     * @param series      the series
     * @return the time of the newest stored value in milliseconds, or a value of 0 or less if
     *         nothing is stored for the series
     * @throws RepositoryException if the repository cannot tell; that is not "nothing stored"
     */
    long endOf(String projectName, String measurement, SeriesDimensions series) throws RepositoryException;
}
