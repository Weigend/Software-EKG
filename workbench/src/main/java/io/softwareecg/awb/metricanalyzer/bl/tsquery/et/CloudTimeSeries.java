package io.softwareecg.awb.metricanalyzer.bl.tsquery.et;

import io.softwareecg.awb.sdk.datamodel.TimeSeries;
import io.softwareecg.awb.sdk.datamodel.Value;

import java.util.List;

/**
 * A specialized version of TimeSeries that represents TimeSeries
 * of cloud-native only environments
 *
 * @since 09.07.2018
 */
public class CloudTimeSeries extends TimeSeries {

    /**
     * Constructs a time series
     *
     * @param project - the name of the project the time-series belongs to
     * @param namespace - the namespace filter dimension (cloud native dimension)
     * @param service - the service filter dimension (cloud native dimension)
     * @param pod - the pod filter dimension (cloud native dimension)
     * @param container - the container filter dimension (cloud native dimension)
     * @param measurement - the measurement filter dimension (overall dimension)
     * @param process - the process filter dimension (overall dimension)
     * @param metricGroup - the metric group filter dimension (overall dimension)
     * @param metricName - the host filter dimension (overall dimension)
     * @param values - a tuple list of values and timestamps that represents the time-series
     */
    public CloudTimeSeries(final String project,
                      final String namespace,
                      final String service,
                      final String pod,
                      final String container,
                      final String measurement,
                      final String process,
                      final String metricGroup,
                      final String metricName,
                      final List<Value> values) {

        super(project, "", "",  namespace, service, pod, container, measurement, null,
                process, metricGroup, metricName, values);
    }
}
