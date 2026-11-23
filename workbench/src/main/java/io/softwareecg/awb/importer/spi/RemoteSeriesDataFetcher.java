package io.softwareecg.awb.importer.spi;

import io.softwareecg.awb.repository.model.TimeSeriesImportSource;
import io.softwareecg.awb.timeseries.Value;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface that represents data access objects that fetch time series data
 * from a none Software-EKG remote types using unique keys that matching to
 * the series in the remote data source.
 *
 * @since 01.08.2018
 */
@SuppressWarnings("unused") // this is an SDK API. Not all methods need to be implemented/used at the moment
public interface RemoteSeriesDataFetcher {

    /**
     * Retrieve the time series payload (timestamp, measurement value tuples) matching
     * the specified series key from a none EKG remote data source.
     *
     * @param remoteSeriesKey the series key that can be a simple string or complex serialized object
     * @param start a timestamp that specifies the start of a time interval the series is requested for
     *              negative values can use to advice the Fetcher ignore the start parameter
     * @param end a timestamp that specifies the end of a time interval the series is requested for
     *            negative values can use to advice the Fetcher ignore the start parameter
     *
     * @return the time series data (may a empty array if not available) that matches to the key and time interval
     */
    Value[] fetchSeriesData(String remoteSeriesKey, long start, long end);

    /**
     * Retrieve the time series payload (timestamp, measurement value tuples) matching
     * the specified list of expressions from a none EKG remote data source.
     *
     * @param searchLabelExp a map with label/attribute as key and a search expression as value
     * @param start a timestamp that specifies the start of a time interval the series is requested for
     *                    negative values can use to advice the Fetcher ignore the start parameter
     * @param end a timestamp that specifies the end of a time interval the series is requested for
     *                  negative values can use to advice the Fetcher ignore the start parameter
     * @return the time series data (may a empty array if not available) that matches to the key and time interval
     */
    Value[] fetchSeriesData(Map<String, String> searchLabelExp, long start, long end);

    /**
     * Retrieve a couple of time series payloads (timestamp, measurement value tuples) for each series
     * specified with the series key in the collection.
     * The series data will returned as map with series-key as key and the data array as value
     *
     * @param remoteSeriesKeys the series key that can be a simple string or complex serialized object
     * @param start a timestamp that specifies the start of a time interval the series is requested for
     *              negative values can use to advice the Fetcher ignore the start parameter
     * @param end a timestamp that specifies the end of a time interval the series is requested for
     *            negative values can use to advice the Fetcher ignore the start parameter
     *
     * @return the time series data (may a empty array if not available) that matches to the key and time interval
     */
    default Map<String, Value[]> bulkFetchSeriesData(Collection<String> remoteSeriesKeys, long start, long end) {
        Map<String, Value[]> responseMap = new HashMap<>();

        for (String seriesKey : remoteSeriesKeys) {
            responseMap.put(seriesKey, fetchSeriesData(seriesKey, start, end));
        }

        return responseMap;
    }

    /**
     * Set the data-source context this fetcher should use to fetch the data.
     *
     * @param sourceRepository the remote type definition that provides address and credentials if necessary
     */
    void setSourceRepositoryDefinition(TimeSeriesImportSource sourceRepository);
}
