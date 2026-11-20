package io.softwareecg.collector.dt;

import io.softwareecg.collector.config.RemoteCounterDescriptor;

import java.util.Arrays;

/**
 * This class is a specialized variant of CollectorJobConfiguration that represents
 * configuration for an collector that run as single instance to monitor a single or
 * multiple nodes.
 * The configuration only provide classic physical layer data. No logical domain model
 * properties are supported. Use {@link SidecarCollectorJobConfiguration} or
 * {@link CloudCollectorJobConfiguration} if you need configurations for cloud monitoring
 * use-cases.
 *
 * @since 09.08.2018
 */
@SuppressWarnings("unused")
public class ClassicCollectorJobConfiguration extends CollectorJobConfiguration implements RemoteCounterDescriptor {

    /**
     * An very simple container that store information's
     * about the physical or visualized host.
     */
    private final HostInfo hostInfo;


    //================================================================================================================
    //  Constructor
    //================================================================================================================

    /**
     * Constructs a new instance of ClassicCollectorJobConfiguration with the
     * configuration values that will provided by this config class to the
     * further business logic.
     *
     * @param ekgProjectName the name of the EKG project used in the Software-EKG AWB to display the data
     * @param hostInfo a simple container that store information's about the physical or visualized host.
     * @param processInfo a config bean that store process details like name or it's API endpoints to fetch metrics
     * @param metricGroupInfo a config bean that stores settings of the metric processing fetch intervals or the parser
     * @param metricDefinitions a config bean that store details about the metrics and how to process them
     */
    public ClassicCollectorJobConfiguration(String ekgProjectName, HostInfo hostInfo, ProcessInfo processInfo,
                                               MetricGroupInfo metricGroupInfo, MetricDefinition[] metricDefinitions) {

        super(ekgProjectName, hostInfo, processInfo, metricGroupInfo, metricDefinitions);
        this.hostInfo = hostInfo;
    }


    //================================================================================================================
    //  configuration API implementation of CollectorJobConfiguration type
    //================================================================================================================


    @Override
    public CollectorJobConfiguration copy() {

        return new ClassicCollectorJobConfiguration(
                ekgProjectName,
                hostInfo.copy(),
                processInfo.copy(),
                metricGroupInfo.copy(),
                Arrays.copyOf(metricDefinitions, metricDefinitions.length)
        );
    }

    @Override
    public TimeSeriesHeader getTimeSeriesHeader() {

        TimeSeriesHeader timeSeriesHeader = new TimeSeriesHeader();

        // classic properties
        timeSeriesHeader.setHostGroupId(hostInfo.getNodeGroupId());
        timeSeriesHeader.setHostName(hostInfo.getNodeName());

        // common properties
        timeSeriesHeader.setEkgProjectName(ekgProjectName);
        timeSeriesHeader.setMetricGroupId(getMetricGroupId());
        timeSeriesHeader.setProcessLabel(getProcessLabel());

        return timeSeriesHeader;
    }

    //================================================================================================================
    //  Accessor API of ClassicCollectorJobConfiguration type
    //================================================================================================================

    /**
     * Returns IP address or absolute URL both with
     * or without port number
     *
     * @return Value for property 'hostIp'.
     */
    public String getHostAddress() {
        return getNodeAddress();
    }

    /**
     * Returns the value of the property 'hostName'.
     *
     * @return Value for property 'hostName'.
     */
    public String getHostName() {
        return getNodeName();
    }

    public String getHostGroup() {
        return getNodeName();
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

}
