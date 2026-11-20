package io.softwareecg.collector.config;

import io.softwareecg.collector.dt.MetricDefinition;

/**
 * todo insert comment here
 *
 * @since 11.08.2018
 */
public interface CounterDescriptor {

    /**
     * Returns the source endpoint that provided the process metrics.
     *
     * This can be an URL, a file path or just a config token and
     * heavily depends on what the counter source type requires.
     *
     * @return the source endpoint
     */
    String getProcessMetricsSource();

    String getAdditionalArguments();

    String getProcessLabel();

    String getNodeName();

    String getNodeGroupId();

    /**
     * Return the endpoint address the collector will call to
     * retrieve the metrics for all defined processes.
     *
     * @return the address of the node the metrics should collect from.
     */
    String getNodeAddress();

    String getUsername();

    String getPassword();


    String getMetricGroupId();

    /**
     * Returns the fetch interval for new metric values in milliseconds.
     *
     * @return the fetch interval in millis
     */
    long getMetricFetchInterval();

    MetricDefinition[] getMetricDefinitions();

    void setProcessMetricsSource(String source);
}
