package io.softwareecg.collector.repository.rdb.api;

/**
 * todo insert comment here
 *
 * @since 30.10.2018
 */
public interface FlexTimeSeriesStorage {

    /**
     *
     * @param timeSeries
     */
    void addTimeSeries(TimeSeries timeSeries);

}
