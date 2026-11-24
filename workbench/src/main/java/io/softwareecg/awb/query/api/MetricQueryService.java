package io.softwareecg.awb.query.api;

import io.softwareecg.awb.query.model.ComputationSpec;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.repository.spi.RepositoryClientAware;

/**
 * This interface represents series that can use to fetch and compute time series from the repositories.
 * Compute means TimeSeries post processing with vectorization, aggregation and smoothing.
 * Which data will fetched and how it will post computed can controlled via call parameters.
 *
 * @since 01.06.2018
 */
public interface MetricQueryService extends RepositoryClientAware {

    /**
     * Gets all counters by the given criteria.
     *
     * @param filterParams the query parameter that define which metrics in which time range should be fetched
     * @param computeParams the compute parameters that define various setting for the post computing steps on the
     *                    fetched time series data like
     * @param metricLimit the maximum amount of metrics that should be fetched.
     *                    If more metrics matches to the query, it will aborted.
     * @return a response object that contains metadata to number of series, it's limits and the TimeSeries payload as list
     */
    ComputedTimeSeriesResponse getComputedTimeSeries(QuerySpec filterParams,
                                                     ComputationSpec computeParams, int metricLimit);

}
