package io.softwareecg.awb.repository.api;

import io.softwareecg.awb.timeseries.dimension.FilterDimension;

import java.util.List;

/**
 * Finds the series that are stored in a repository.
 */
public interface SeriesSearch {

    /**
     * Reads the stored parts of the matching series page by page, ordered by their start.
     *
     * @param filter    which series
     * @param cursor    where the page before ended; {@code null} for the first page
     * @param maxSeries if more parts match, the page holds none of them
     * @return the page
     * @throws RepositoryException if the repository cannot be searched
     */
    SeriesPage find(SeriesFilter filter, String cursor, long maxSeries) throws RepositoryException;

    /**
     * @param dimension the dimension
     * @param filter    which series
     * @return the values the dimension has in the matching series, in alphabetical order
     * @throws RepositoryException if the repository cannot be searched
     */
    List<String> valuesOf(FilterDimension dimension, SeriesFilter filter) throws RepositoryException;

    /**
     * @param filter which series
     * @return the time of the newest stored value in milliseconds; 0 if nothing is stored
     * @throws RepositoryException if the repository cannot tell; that is not "nothing stored"
     */
    long endOf(SeriesFilter filter) throws RepositoryException;
}
