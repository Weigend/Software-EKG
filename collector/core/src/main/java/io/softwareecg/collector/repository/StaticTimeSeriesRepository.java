//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.repository;


import io.softwareecg.collector.dt.TimeSeriesHeader;

import java.util.Date;


/**
 * Interface that represents various kinds of storage's that persist the metric data
 * of the EKG collector using different output channels (files, databases, ....)
 *
 * @since 13.08.2018
 */
public interface StaticTimeSeriesRepository {

    /**
     * Creates a new measurement context insides the log storage that will use to fulfill and write the time series
     * data. The context will be active until the whole live of the storage or this method is called again.
     *
     * The time series data will added step by step every time the collector passes a fetch interval for metrics.
     * The metric labels must match the added time series data in amount and order of the values.
     * See addValueSetToMeasurement(Date, Double[]) method for further information.
     *
     * @param timeSeriesHeader bean that represents parts of the collector config that specify the time series metadata
     * @param metricLabels an array of metric label used for metric name in the persisted time series
     */
    void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels);

    /**
     * Adds a set of metric values that are collected during the fetch cycle.
     * The mount and order of the metric values given by the metricValues parameter must match to the metricLabels
     * specified before calling the createMeasurement(TimeSeriesHeader, String[]) method.
     *
     * The value are not primitives. So a single value in the array can be null if it couldn't fetched in the last
     * cycle. In this case it's on the StaticTimeSeriesRepository to decide how to store this information in the persistence layer.
     *
     * @param captureTime the datetime the metrics where captured in UTC time
     * @param metricValues array of metric values with null values inside if one or more metrics couldn't captured
     */
    void addValueSetToMeasurement(Date captureTime, Double[] metricValues);

    /**
     * Executes a flush of all data that are hold by the storage but not already written
     * to the persistence layer.
     *
     * This method should be called if the storage is no longer required, for example amount
     * of metric labels changes, but already captured data should persisted consistently.
     */
    void flush();

    /**
     * Executes repository specific cleanup task to finalize the data / states before
     * the instance will catch by the GarbageCollector.
     * This method should only called once and after all other action otherwise it can result
     * in unexpected errors.
     */
    /**
     * The time of the newest value set this storage already holds for the measurement created
     * with {@link #createMeasurement}. A job that collects from a source with a history of its
     * own continues from there, so a storage that outlives the collector yields one gap-free
     * series across restarts.
     *
     * @return that time, or {@code null} if the storage holds no values of the measurement yet
     * @throws RuntimeException if the storage holds values but cannot tell where they end at the
     *                          moment. Returning {@code null} then would make the job fill in
     *                          from the start again and store values twice.
     */
    default Date getLastCaptureTime() {
        return null;
    }

    default void cleanUp() {
        // do nothing
    }
}
