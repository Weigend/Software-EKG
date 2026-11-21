package io.softwareecg.collector.dt;

import java.util.StringJoiner;

/**
 * A bean that represents parts of the collector configuration stored as part of the recorded time series.
 * The class is is very similar to the {@link CollectorJobConfiguration} but provides exactly the field the
 * EKG AWB needs to filter the metrics and no ones to control the collector itself.
 *
 * The header also doesn't contain any payload data and the metric name itself because this will resolved
 * dynamically during the capture process.
 *
 * Looking comparing EKG filter dimensions also the measurement property is missing. This attribute has a special
 * role because it isn't part of the configuration and will defined by the collector storage at runtime based on the
 * collector strategy and the implementation the storage.
 *
 * @since 13.08.2018
 */
public class TimeSeriesHeader {

    // ----------------- common properties ------------------

    /**
     * the name of the EKG project the time series belongs to
     */
    private String ekgProjectName;

    /**
     * the label (or name of label not available) of the process
     * that provides the captured metrics
     */
    private String processLabel;

    /**
     * the unique id of the process group the process belongs to in the configuration
     */
    private String processGroupId;

    /**
     * the unique id of the metric group used in the configuration
     */
    private String metricGroupId;

    // --------------- classic env properties ---------------

    /**
     * the unique id of the host group that is in use
     */
    private String hostGroupId;

    /**
     * the name of the host the metrics are fetched from
     */
    private String hostName;


    // --------------- cloud env properties ----------------

    /**
     * the name of the project in the the cloud native platform
     * all the pods and container belongs to.
     */
    private String cloudProjectName;

    /**
     * the name of the Kubernetes/OpenShift namespace
     */
    private String namespace;

    /**
     * the name of the service that the pod is backing
     */
    private String service;

    /**
     * the name/label of the pod the metrics are from
     */
    private String podName;

    /**
     * the id of the container in the cloud platform
     */
    private String containerId;


    //===============================================================================================================
    //  accessors of the bean
    //===============================================================================================================

    public String getEkgProjectName() {
        return ekgProjectName;
    }

    public void setEkgProjectName(String ekgProjectName) {
        this.ekgProjectName = ekgProjectName;
    }

    public String getProcessLabel() {
        return processLabel;
    }

    public void setProcessLabel(String processLabel) {
        this.processLabel = processLabel;
    }

    public String getProcessGroupId() {
        return processGroupId;
    }

    public void setProcessGroupId(String processGroupId) {
        this.processGroupId = processGroupId;
    }

    public String getMetricGroupId() {
        return metricGroupId;
    }

    public void setMetricGroupId(String metricGroupId) {
        this.metricGroupId = metricGroupId;
    }

    public String getHostGroupId() {
        return hostGroupId;
    }

    public void setHostGroupId(String hostGroupId) {
        this.hostGroupId = hostGroupId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getCloudProjectName() {
        return cloudProjectName;
    }

    public void setCloudProjectName(String cloudProjectName) {
        this.cloudProjectName = cloudProjectName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String pod) {
        this.podName = pod;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getCombinedKey() {
        return toString();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TimeSeriesHeader.class.getSimpleName() + "[", "]")
                .add("ekgProjectName='" + ekgProjectName + "'")
                .add("processGroupId='" + processGroupId + "'")
                .add("processLabel='" + processLabel + "'")
                .add("metricGroupId='" + metricGroupId + "'")
                .add("hostGroupId='" + hostGroupId + "'")
                .add("hostName='" + hostName + "'")
                .add("cloudProjectName='" + cloudProjectName + "'")
                .add("namespace='" + namespace + "'")
                .add("service='" + service + "'")
                .add("podName='" + podName + "'")
                .add("containerId='" + containerId + "'")
                .toString();
    }
}
