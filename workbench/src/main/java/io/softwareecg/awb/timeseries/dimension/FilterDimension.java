package io.softwareecg.awb.timeseries.dimension;

/**
 * An enumeration that represents the fixed set of filter dimensions
 * the EKG data model support for time series.
 *
 * This filter dimension can also use for other domain specific data like using the
 * METRIC_NAME to store the account name for time tracking.
 * In this case the importer is responsible to provide the information
 * to clarify which dimension stores which kind of data through an alias mapping
 *
 * The dimension are divided into 3 groups. That are shown and hide by the filter panel in the metric workbench
 * depending with view flavor the user has selected.
 *
 * physical view dimensions: filter dimensions that are only visible than the user selects the physical (classic) view
 * cloud view dimensions: filter dimensions that are only visible than the user selects the cloud (logical) view
 * generic view dimensions: filter dimensions that are always shown in the filter panel
 *
 * @since 13.07.2018
 */
public enum FilterDimension {

    // ----------------------- physical view dimensions --------------------

    /**
     * A group of hosts
     * (for example a specific physical cluster that multiple hosts)
     */
    HOST_GROUP,

    /**
     * A single host like a physical node or VM
     */
    HOST,

    // ------------------ cloud (logical) view dimensions --------------------

    /**
     * The namespace inside the cloud platform. This can also the overlay
     * network name in Docker Swarm or something similar that defines a
     * unit of services belonging together/project inside a cloud environment
     */
    NAMESPACE,

    /**
     * A (logical) service. A service can use multiple instances (replicas)
     * that run as independent containers.
     */
    SERVICE,

    /**
     * A unit that orchestrate 1-n containers in platforms like Kubernetes
     * or OpenShift.
     */
    POD,

    /**
     * The atomic deployment unit in the cloud platforms
     */
    CONTAINER,

    // ---------------------- generic view dimensions ------------------------

    /**
     * A specific measurement the time series belongs to. This is the only
     * attribute the normally will not provided by the time series source.
     * If not used in another domain context the meaning a group of time series
     * that are measured together a specific context. For example in an A/B comparision
     * contains measurement "A" all data of the first measurement. And measurement "B"
     * the ones of the second.
     */
    MEASUREMENT,

    /**
     * A group of processes. For example, all instances of the same application or service type.
     * In Prometheus context, this typically corresponds to the 'job' label.
     */
    PROCESS_GROUP,

    /**
     * A physical process runs inside a container (cloud) or on a physical node.
     * Examples are the JVM (or more specific like Solr or Payara Micro) or the dockerd process
     * in cases the cloud platform itself will analyzed.
     */
    PROCESS,

    /**
     * A group of metrics like all JMX metrics or all OS provided metrics.
     * This can use also in another domain context for example
     * main account vs. sub-account at time tracking.
     */
    METRIC_GROUP,

    /**
     * The concrete metric that is captured to be analyzed like Java Heap or
     * a specific time tracking account.
     */
    METRIC_NAME
}
