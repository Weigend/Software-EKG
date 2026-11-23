package io.softwareecg.awb.importer.adapter.prometheus.client.model.query;

import io.softwareecg.awb.importer.adapter.prometheus.client.model.ApiResult;

import java.util.Arrays;

/**
 * API result entity that stores a list of series that will returned
 * by the Prometheus v1 API than the client requests series using the
 * api/v1/series endpoint.
 *
 * @since 13.07.2018
 */
public class ListSeriesResult extends ApiResult {

    private final SeriesDefinition[] data = new SeriesDefinition[0];

    /**
     * Returns an array of SeriesDefinition instances that
     * contains the metric name and all labels that belongs to it.
     *
     * @return an array of SeriesDefinition instances returned by the Prometheus API
     */
    public SeriesDefinition[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ListSeriesResult{" +
                "data=" + Arrays.toString(data) +
                "} " + super.toString();
    }
}
