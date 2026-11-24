package io.softwareecg.collector.config;

import io.softwareecg.collector.dt.ContainerInfo;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.dt.MetricGroupInfo;
import io.softwareecg.collector.dt.PlatformProjectInfo;
import io.softwareecg.collector.dt.PodInfo;
import io.softwareecg.collector.dt.ProcessInfo;
import io.softwareecg.collector.dt.TimeSeriesHeader;
/**
 * todo insert comment here
 *
 * @since 09.08.2018
 */
public class CloudCollectorJobConfiguration extends CollectorJobConfiguration {

    private final PlatformProjectInfo platformProjectInfo;

    private final PodInfo podInfo;

    private final ContainerInfo containerInfo;

    /**
     *
     * @param ekgProjectName
     * @param platformProjectInfo
     * @param podInfo
     * @param containerInfo
     * @param processInfo
     * @param metricGroupInfo
     * @param metricDefinitions
     */
    public CloudCollectorJobConfiguration(String ekgProjectName, PlatformProjectInfo platformProjectInfo, PodInfo podInfo,
                                          ContainerInfo containerInfo, ProcessInfo processInfo,
                                          MetricGroupInfo metricGroupInfo, MetricDefinition[] metricDefinitions) {

        super(ekgProjectName, containerInfo != null ? containerInfo : podInfo, processInfo, metricGroupInfo, metricDefinitions);

        this.platformProjectInfo = platformProjectInfo;
        this.podInfo = podInfo;
        this.containerInfo = containerInfo;
    }

    //================================================================================================================
    //  configuration API implementation of CollectorJobConfiguration type
    //================================================================================================================

    @Override
    public CollectorJobConfiguration copy() {
        return null;
    }

    @Override
    public TimeSeriesHeader getTimeSeriesHeader() {
        return null;
    }

}
