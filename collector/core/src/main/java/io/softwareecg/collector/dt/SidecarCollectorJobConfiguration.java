package io.softwareecg.collector.dt;

import io.softwareecg.collector.config.RemoteCounterDescriptor;

import java.util.Arrays;

/**
 * todo insert comment here
 *
 * @since 09.08.2018
 */
public class SidecarCollectorJobConfiguration extends CollectorJobConfiguration implements RemoteCounterDescriptor {

    private PlatformProjectInfo platformProjectInfo;

    private PodInfo podInfo;

    private ContainerInfo containerInfo;

    private HostInfo hostInfo;


    //================================================================================================================
    // the different constructors that create classic or cloud based SidecarCollectorJobConfiguration instances
    //================================================================================================================

    /**
     * todo
     *
     * @param ekgProjectName the name of the EKG AWB project the captured time series belongs to (optional / nullable)
     * @param platformProjectInfo config that provides details about the cloud platform project the measured nodes belongs to
     * @param podInfo config details to the pod the metrics will captured from
     * @param containerInfo (optional / nullable)
     * @param processInfo
     * @param metricGroupInfo
     * @param metricDefinitions
     */
    public SidecarCollectorJobConfiguration(String ekgProjectName, PlatformProjectInfo platformProjectInfo,
                                            PodInfo podInfo, ContainerInfo containerInfo, ProcessInfo processInfo,
                                            MetricGroupInfo metricGroupInfo, MetricDefinition[] metricDefinitions) {

        super(ekgProjectName, containerInfo != null ? containerInfo : podInfo, processInfo, metricGroupInfo, metricDefinitions);

        this.platformProjectInfo = platformProjectInfo;
        this.podInfo = podInfo;
        this.containerInfo = containerInfo;
    }

    /**
     * todo
     *
     * @param ekgProjectName the name of the EKG AWB project the captured time series belongs to (optional / nullable)
     * @param nodeInfo
     * @param processInfo
     * @param metricGroupInfo
     * @param metricDefinitions
     */
    public SidecarCollectorJobConfiguration(String ekgProjectName, NodeInfo nodeInfo, ProcessInfo processInfo,
                                            MetricGroupInfo metricGroupInfo, MetricDefinition[] metricDefinitions) {

        super(ekgProjectName, nodeInfo, processInfo, metricGroupInfo, metricDefinitions);

        hostInfo = nodeInfo instanceof HostInfo ? (HostInfo) nodeInfo : null;
    }

    //================================================================================================================
    //  configuration API implementation of CollectorJobConfiguration type
    //================================================================================================================

    @Override
    public CollectorJobConfiguration copy() {

        // check that kind of job SidecarCollectorJobConfiguration need to copy
        if (hostInfo == null) {

            // cloud aware
            return new SidecarCollectorJobConfiguration(
                    this.ekgProjectName,
                    this.platformProjectInfo.copy(),
                    this.podInfo.copy(),
                    this.containerInfo != null ? this.containerInfo.copy() : null,
                    this.processInfo.copy(),
                    this.metricGroupInfo.copy(),
                    Arrays.copyOf(this.metricDefinitions, this.metricDefinitions.length)
            );
        } else {

            // classic
            return new SidecarCollectorJobConfiguration(
                    this.ekgProjectName,
                    this.hostInfo.copy(),
                    this.processInfo.copy(),
                    this.metricGroupInfo.copy(),
                    Arrays.copyOf(this.metricDefinitions, this.metricDefinitions.length)
            );
        }
    }


    @Override
    public TimeSeriesHeader getTimeSeriesHeader() {

        TimeSeriesHeader timeSeriesHeader = new TimeSeriesHeader();

        // cloud properties
        timeSeriesHeader.setCloudProjectName(platformProjectInfo != null ? platformProjectInfo.getProjectName() : null);
        timeSeriesHeader.setNamespace(podInfo != null ? podInfo.getNamespace() : null);
        timeSeriesHeader.setService(podInfo != null ? podInfo.getServiceName() : null);
        timeSeriesHeader.setContainerId(containerInfo != null ? containerInfo.getNodeName() : null);
        timeSeriesHeader.setPodName(podInfo != null ? podInfo.getNodeName() : null);

        String defaultHostGroupId = podInfo != null ? null : "*";
        String defaultHostName = podInfo != null ? null : "localhost";

        // classic properties
        timeSeriesHeader.setHostGroupId(hostInfo != null ? hostInfo.getNodeGroupId() : defaultHostGroupId);
        timeSeriesHeader.setHostName(hostInfo != null ? hostInfo.getNodeName() : defaultHostName);

        // common properties
        timeSeriesHeader.setEkgProjectName(ekgProjectName);
        timeSeriesHeader.setMetricGroupId(getMetricGroupId());
        timeSeriesHeader.setProcessLabel(getProcessLabel());
        timeSeriesHeader.setProcessGroupId(getProcessGroupId());

        return timeSeriesHeader;
    }


}
