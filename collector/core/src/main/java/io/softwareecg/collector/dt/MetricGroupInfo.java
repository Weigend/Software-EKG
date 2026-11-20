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
package io.softwareecg.collector.dt;

/**
 * This class contains all information for a counter group.
 */
public class MetricGroupInfo {

    //----------------------------------------- start member section ---------------------------------------------------

    /**
     * the unique id of the metric group that will used to identify the specific metric group
     * than referencing it in the Collector config section
     */
    private final String metricGroupId;

    /**
     * The fetch interval for new metric values in milliseconds.
     */
    private final long metricFetchInterval;

    /**
     * a time or repeating date (like day of week) the metrics should fetched.
     * This property will overrule the metric fetch interval.
     */
    private final String metricFetchAt;

    /**
     * the technical type of the metrics combined in the group like
     * JMX, Unix low-level metrics or Windows Performance Counter
     */
    private final String metricGroupType;

    /**
     * the time interval in millisecond the collector waits before it resolves the wildcards again
     * to look if new metrics that matches on the wildcards are available.
     * NULL -> don't refresh wildcards.
     */
    private final Long refreshWildcardsInterval;

    //------------------------------------------ end member section ----------------------------------------------------

    /**
     * Constructs a new MetricGroupInfo instance with all information's
     * the bean should provide to others.
     *
     * @param metricGroupId       the unique id of the metric group
     * @param metricGroupType     the technical type of the metrics combined in the group like JMX or WPC
     * @param refreshWildcardsInterval    the refresh wildcards flag for cyclic wildcard detection.
     * @param metricFetchInterval the wait time interval for the next collector cycle in ms.
     * @param metricFetchAt        a concrete time than the metrics should be fetched again
     */
    public MetricGroupInfo(String metricGroupId, String metricGroupType,
                           Long refreshWildcardsInterval, long metricFetchInterval, String metricFetchAt) {

        this.metricFetchInterval = metricFetchInterval;
        this.metricGroupId = metricGroupId;
        this.metricGroupType = metricGroupType;
        this.refreshWildcardsInterval = refreshWildcardsInterval;
        this.metricFetchAt = metricFetchAt;
    }

    public String getMetricFetchAt() {
        return metricFetchAt;
    }

    public long getMetricFetchInterval() {
        return metricFetchInterval;
    }

    /**
     * Returns the unique id of the metric group.
     *
     * @return the id of the metric group
     */
    public String getMetricGroupId() {
        return metricGroupId;
    }

    /**
     * Returns the technical type of the metrics combined in the group
     * like JMX, Unix low-level metrics or Windows Performance Counter
     *
     * @return the counter type all metrics in the group belongs to
     */
    public String getMetricGroupType() {
        return metricGroupType;
    }

    /**
     * Returns the interval in milliseconds the collector will wait until
     * it re-expand the wildcard again to see if there are new metrics available
     *
     * @return the refresh interval in millis
     */
    public Long getWildcardExpandRefreshInterval() {
        return refreshWildcardsInterval;
    }

    public MetricGroupInfo copy() {
        return new MetricGroupInfo(metricGroupId, metricGroupType,
                refreshWildcardsInterval, metricFetchInterval, metricFetchAt);
    }
}