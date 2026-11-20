package io.softwareecg.awb.sdk.datamodel;


import java.util.List;

/**
 * The SeriesData is a identically to the {@link TimeSeries} bean that
 * stores the meta data of a time series and the series self as {@link Value}
 * list. In addition to the super class this one provides additional
 * constructors that allows to instantiate the class in different use cases
 * with less clue code.
 *
 * @since 14.07.2018
 */
public class SeriesData extends TimeSeries {

    /**
     * Constructs a time series
     *
     * @param project - the name of the project the time-series belongs to
     * @param hostGroup - the host group / cluster filter dimension (classic dimension)
     * @param host - the host filter dimension (classic dimension)
     * @param namespace - the namespace filter dimension (cloud native dimension)
     * @param service - the service filter dimension (cloud native dimension)
     * @param pod - the pod filter dimension (cloud native dimension)
     * @param container - the container filter dimension (cloud native dimension)
     * @param measurement - the measurement filter dimension (overall dimension)
     * @param processGroup - the process group filter dimension (overall dimension)
     * @param process - the process filter dimension (overall dimension)
     * @param group - the metric group filter dimension (overall dimension)
     * @param metricName - the host filter dimension (overall dimension)
     */
    public SeriesData(String project,
                      String hostGroup,
                      String host,
                      String namespace,
                      String service,
                      String pod,
                      String container,
                      String measurement,
                      String processGroup,
                      String process,
                      String group,
                      String metricName) {

        super(project, hostGroup, host, namespace, service, pod, container, measurement, processGroup, process, group, metricName);
    }

    /**
     * Constructs a time series
     *
     * @param project - the name of the project the time-series belongs to
     * @param hostGroup - the host group / cluster filter dimension (classic dimension)
     * @param host - the host filter dimension (classic dimension)
     * @param namespace - the namespace filter dimension (cloud native dimension)
     * @param service - the service filter dimension (cloud native dimension)
     * @param pod - the pod filter dimension (cloud native dimension)
     * @param container - the container filter dimension (cloud native dimension)
     * @param measurement - the measurement filter dimension (overall dimension)
     * @param processGroup - the process group filter dimension (overall dimension)
     * @param process - the process filter dimension (overall dimension)
     * @param metricGroup - the metric group filter dimension (overall dimension)
     * @param metricName - the host filter dimension (overall dimension)
     * @param values - a tuple list of values and timestamps that represents the time-series
     */
    public SeriesData(String project,
                      String hostGroup,
                      String host,
                      String namespace,
                      String service,
                      String pod,
                      String container,
                      String measurement,
                      String processGroup,
                      String process,
                      String metricGroup,
                      String metricName,
                      List<Value> values) {

        super(project, hostGroup, host, namespace, service, pod, container, measurement, processGroup, process, metricGroup, metricName, values);
    }
}
