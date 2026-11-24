package io.softwareecg.awb.query.spi;

import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.timeseries.Value;

/**
 * Where the values of a remote project are: only the description of its series is stored in the
 * repository, the values stay where they come from. Implemented by the importers that make such
 * projects; a bean of this type is found by the query.
 */
public interface RemoteSeriesSource {

    /**
     * @return the id of the importer whose projects this one serves
     */
    String getImporterID();

    /**
     * Reads the values of a series.
     *
     * @param source    the server the project was imported from; one importer serves many of them
     * @param seriesKey what the importer stored to find the series again
     * @param start     the start of the interval in milliseconds
     * @param end       the end of the interval in milliseconds
     * @return the values; {@code null} if the server cannot be reached
     */
    Value[] fetchSeries(TimeSeriesImportSource source, String seriesKey, long start, long end);
}
