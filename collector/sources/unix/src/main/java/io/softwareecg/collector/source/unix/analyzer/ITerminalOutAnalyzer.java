package io.softwareecg.collector.source.unix.analyzer;

import io.softwareecg.collector.dt.MetricDefinition;

import java.util.Collection;

/**
 * @since 06.03.13
 */
public interface ITerminalOutAnalyzer {

    /**
     * Returns a collection of counters that replace the given one. If the given
     * MetricDefinition hasn't a wildcard, the collection only contains the given one.
     * Otherwise it will contain one or more Counters matching to the wildcard.
     *
     * @param metricDefinition the MetricDefinition to investigate
     * @return a collection with replacement counters
     */
    Collection<? extends MetricDefinition> expandCounter(MetricDefinition metricDefinition);

    /**
     * Get the counter.
     *
     * @param name a valid counter.
     * @return a double.
     */
    double getCounter(String name);

    /**
     * Specialized variant of getCounter(java.lang.String) that will use the postFilter expression of the given
     * metric definition to find the correct process in case of ambiguous results.
     *
     * For example if the user specifies "\Process(java)\PID" and more than one java processes running,
     * the result will ambiguous. The use can specify java#1 that will return the oldest one but how know
     * which process it will be. Using the postFilter expression the java process can matched by it's arguments.
     *
     * If the filter isn't selective or blank or the metric isn't a process metric the method acts
     * like getCounter(java.lang.String)
     *
     * @param metricName the name of the metric the caller requests the metric value for
     * @param regexPostFilter the name of the metric the caller requests the metric value for
     *
     * @return the resolved metric value for the current time
     */
    default double getFilteredCounter(String metricName, String regexPostFilter) {
        return getCounter(metricName);
    }

    /**
     * Sets the output from the terminal.
     *
     * @param topOutput the new top output
     */
    void setTerminalOutput(String[] topOutput);
}
