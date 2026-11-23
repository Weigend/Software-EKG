package io.softwareecg.awb.timeseries.dimension;

import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * todo enter comment here
 *
 * @since 09.07.2018
 */
public class ClassicTimeSeries extends TimeSeries {

    /**
     * Constructs a time series
     *
     * @param project - the name of the project the time-series belongs to
     * @param hostGroup - the host group / cluster filter dimension (classic dimension)
     * @param host - the host filter dimension (classic filter dimension)
     * @param measurement - the measurement filter dimension (overall dimension)
     * @param process - the process filter dimension (overall dimension)
     * @param metricGroup - the metric group filter dimension (overall dimension)
     * @param metricName - the host filter dimension (overall dimension)
     * @param values - a tuple list of values and timestamps that represents the time-series
     */
    public ClassicTimeSeries(final String project,
                           final String hostGroup,
                           final String host,
                           final String measurement,
                           final String process,
                           final String metricGroup,
                           final String metricName,
                           final List<Value> values) {

        super(project, hostGroup, host, "", "", "", "", measurement, null,
                process, metricGroup, metricName, values);
    }

    /**
     * Constructs a time series
     *
     * @param project - the name of the project the time-series belongs to
     * @param hostGroup - the host group / cluster filter dimension (classic dimension)
     * @param host - the host filter dimension (classic filter dimension)
     * @param measurement - the measurement filter dimension (overall dimension)
     * @param process - the process filter dimension (overall dimension)
     * @param metricGroup - the metric group filter dimension (overall dimension)
     * @param metricName - the host filter dimension (overall dimension)
     */
    public ClassicTimeSeries(final String project,
                             final String hostGroup,
                             final String host,
                             final String measurement,
                             final String process,
                             final String metricGroup,
                             final String metricName) {

        super(project, hostGroup, host, "", "", "", "", measurement, null,
                process, metricGroup, metricName, new ArrayList<>());
    }
}
