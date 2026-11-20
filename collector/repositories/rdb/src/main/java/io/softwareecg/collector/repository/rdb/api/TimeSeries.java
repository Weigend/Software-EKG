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
package io.softwareecg.collector.repository.rdb.api;

import io.softwareecg.collector.common.DateTimeUtil;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.rdb.api.schema.DocType;
import io.softwareecg.collector.repository.rdb.api.schema.EkgSchemaField;
import io.softwareecg.collector.repository.rdb.api.schema.PersistedField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static io.softwareecg.collector.repository.rdb.api.schema.EkgSchemaField.TS_DATA_AMOUNT_VALUES;
import static io.softwareecg.collector.repository.rdb.api.schema.EkgSchemaField.TS_GROUP_KEY_HASH;

/**
 * The entity for TimeSeries. Used to store in solr.
 *
 * Attention: Changes to this class has an impact to the awb read functionality
 *
 * @since 12.01.2017
 */
@SuppressWarnings("unused")
public class TimeSeries {

    /**
     * The unique id of the time series record in
     * the EKG repository.
     * (normally a UUID)
     */
    @PersistedField(EkgSchemaField.ID)
    private String id;

    /**
     * The doc type of the record in Solr. The EKG Collector always use
     * 'TIME_SERIES' because the collector doesn't create projects, bookmarks
     * or other types.
     */
    @PersistedField(EkgSchemaField.DOC_TYPE)
    private String type = DocType.TIME_SERIES.name();

    /**
     * The name of the Software-EKG AWB project the time series
     * belongs to
     */
    @PersistedField(EkgSchemaField.PROJECT_NAME)
    private String projectName;

    // ----------------- classic filter dimension ------------------

    /**
     * The name of the host group
     * (cluster or logical group of physical / virtual hosts)
     */
    @PersistedField(EkgSchemaField.TS_HOST_GROUP_NAME)
    private String hostGroupName;

    /**
     * The name of the physical or virtual (VM) host
     */
    @PersistedField(EkgSchemaField.TS_HOST_NAME)
    private String hostName;


    // -------------- cloud native filter dimension ----------------

    /**
     * The name of the namespace that is used in the cloud platform.
     * In some platforms like OpenShift this is equal to the project name.
     */
    @PersistedField(EkgSchemaField.TS_NAMESPACE_NAME)
    private String namespace;

    /**
     * The name of the service that is implemented by
     * one or more pods.
     */
    @PersistedField(EkgSchemaField.TS_SERVICE_NAME)
    private String service;

    /**
     * The name of the pod, that is a deployment unit
     * in popular cloud platforms like Kubernetes or OpenShift
     */
    @PersistedField(EkgSchemaField.TS_POD_NAME)
    private String pod;

    /**
     * The name of the (docker) container as part of a pod.
     */
    @PersistedField(EkgSchemaField.TS_CONTAINER_NAME)
    private String container;



    // ----------------- generic filter dimension ------------------

    /**
     * The name of the process the measured metric belongs to.
     * For example 'Java' or 'Payara Micro'
     */
    @PersistedField(EkgSchemaField.TS_PROCESS_NAME)
    private String processName;

    /**
     * The name of a logical group that groups metrics that belongs
     * to together. For example all JMX metrics or Jetty http stats.
     */
    @PersistedField(EkgSchemaField.TS_METRIC_GROUP)
    private String metricGroupId;

    /**
     * The name of the measurement that is an identifier for the
     * current measurements session. (will changed than the Collector restarts or
     * recreate it's repository or source proxies.
     */
    @PersistedField(EkgSchemaField.TS_MEASUREMENT)
    private String measurement;

    /**
     * The name of the metric of this time series.
     */
    @PersistedField(EkgSchemaField.TS_METRIC_NAME)
    private String metric;

    /**
     * The datetime that represents the last time that
     * is covered by a single point in the time series.
     */
    @PersistedField(EkgSchemaField.TS_START)
    private Date start;

    /**
     * The datetime that represents the last time that
     * is covered by a single point in the time series.
     */
    @PersistedField(EkgSchemaField.TS_STOP)
    private Date end;

    /**
     * The datetime the time series was imported to the repository
     * or overwritten the last time.
     */
    @PersistedField(EkgSchemaField.TS_IMPORT_DATE)
    private Date importDatetime;

    /**
     * Field to store aggregated values that are computed
     * by the collector itself.
     */
    @PersistedField(EkgSchemaField.TS_AGGREGATION_LEVEL)
    private String ag;

    /**
     * A simple or complex (serialized) key that stores information's
     * the Software-EKG AWB needs to fetch the correct time series from
     * remote repositories.
     * In the EKG Collector this attribute is normally not need because it
     * write the complete time series values in the EKG repository.
     */
    @PersistedField(EkgSchemaField.TS_REMOTE_SERIES_KEY)
    private String remoteSeriesKey;

    /**
     * A boolean flag that indicates if the time series record
     * is already merged (compact) by the collector or not.
     * Compact means that the collector merges multiple records of the
     * same KPI to optimize the amount of records in the repository.
     */
    @PersistedField(EkgSchemaField.TS_IS_MERGED)
    private boolean isMerged = false;

    /**
     * A byte array that stores the timestamp (8 bytes long) and the
     * corresponding value (8 bytes double) list of 16bytes tuples that
     * represents each point of the time series.
     */
    @PersistedField(EkgSchemaField.TS_DATA)
    private byte[] data;

    /**
     * The amount of time series points in the whole
     * time series stored in the field 'data'
     */
    @PersistedField(TS_DATA_AMOUNT_VALUES)
    private Integer amountOfSeriesValues;

    /**
     * An hash value that identifies all series records that have
     * similar filter dimension values (pod, service, host, ...) including
     * the metric name but not the times.
     * It can use to search for all time series records that measured exactly
     * the same but to different times with a single filter criteria.
     */
    @PersistedField(TS_GROUP_KEY_HASH)
    private Integer groupKeyHash;

    /**
     * Default constructor that creates a series entity
     * with only defined UUID.
     */
    public TimeSeries() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Parameterized constructor that initialized the new created
     * TimeSeries instance with the filter data given with the
     * various parameters.
     *
     * @param measurement the measurement that identifies the current 'session'
     * @param timeSeriesHeader an instance of {@link TimeSeriesHeader} with the metadata of the series
     * @param metricName the metric name of the time series.
     */
    public TimeSeries(String measurement, TimeSeriesHeader timeSeriesHeader, String metricName) {
        this.id = UUID.randomUUID().toString();

        setEkgProjectName(timeSeriesHeader.getEkgProjectName());
        setHostGroupName(timeSeriesHeader.getHostGroupId());
        setHostName(timeSeriesHeader.getHostName());
        setPod(timeSeriesHeader.getPodName());

        if (StringUtils.isNotBlank(getPod())) {
            setService(timeSeriesHeader.getService() != null ? timeSeriesHeader.getService() : "*");
            setNamespace(timeSeriesHeader.getNamespace() != null ? timeSeriesHeader.getNamespace() : "*");
        }

        setContainerId(timeSeriesHeader.getContainerId());
        setProcessLabel(timeSeriesHeader.getProcessLabel());
        setMeasurement(measurement);
        setMetricGroupId(timeSeriesHeader.getMetricGroupId());
        setMetric(metricName);
        setStart(DateTimeUtil.getNowAsUtcDate());
        setEnd(DateTimeUtil.getNowAsUtcDate());

        setImportDatetime(new Date());

        updateGroupHashKey();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getGroupKeyHash() {
        return groupKeyHash;
    }

    public Integer getAmountOfSeriesValues() {
        return amountOfSeriesValues;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getProjectName() {
        return projectName;
    }

    public void setEkgProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public void setHostGroupName(String hostGroupName) {
        this.hostGroupName = hostGroupName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
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

    public String getPod() {
        return pod;
    }

    public void setPod(String pod) {
        this.pod = pod;
    }

    public String getContainer() {
        return container;
    }

    public boolean isMerged() {
        return isMerged;
    }

    public void setMerged(boolean merged) {
        isMerged = merged;
    }

    public void setContainerId(String container) {
        this.container = container;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessLabel(String processName) {
        this.processName = processName;
    }

    public String getMetricGroupId() {
        return metricGroupId;
    }

    public void setMetricGroupId(String metricGroupId) {
        this.metricGroupId = metricGroupId;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public void setImportDatetime(Date importDatetime) {
        this.importDatetime = importDatetime;
    }

    public void setEnd(Date end) {

        if (start != null && start.getTime() > end.getTime()) {
            return;
        }

        this.end = end;
    }

    public String getRemoteSeriesKey() {
        return remoteSeriesKey;
    }

    public void setRemoteSeriesKey(String remoteSeriesKey) {
        this.remoteSeriesKey = remoteSeriesKey;
    }

    public byte[] getData() {
        if (data == null) {
            return new byte[0];
        }

        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        this.amountOfSeriesValues = data.length / 16;
    }

    public int size() {
        return getData().length / 16;
    }

    /**
     * Get the start timestamp of this timeseries.
     *
     * @return the start timestamp.
     */
    public Instant getStart() {
        return start.toInstant();
    }

    /**
     * Returns the time than the series was imported
     * for the first time or the last time than it is updated
     *
     * @return the datetime of the import
     */
    public Instant getImportDatetime() {
        return importDatetime.toInstant();
    }

    /**
     * Get the start timestamp of this timeseries.
     *
     * @param start the start timestamp.
     */
    public void setStart(Instant start) {
        this.start = Date.from(start);
    }

    /**
     * Get the end timestamp of this timeseries.
     *
     * @return the end timestamp.
     */
    public Instant getEnd() {
        return end.toInstant();
    }

    /**
     * Set the end timestamp of this timeseries.
     *
     * @param end the end timestamp.
     */
    public void setEnd(Instant end) {
        this.end = Date.from(end);
    }

    public String getAg() {
        return ag;
    }

    public void setAg(String ag) {
        this.ag = ag;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TimeSeries that = (TimeSeries) o;
        return Objects.equals(projectName, that.projectName) &&
                Objects.equals(hostGroupName, that.hostGroupName) &&
                Objects.equals(hostName, that.hostName) &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(service, that.service) &&
                Objects.equals(pod, that.pod) &&
                Objects.equals(container, that.container) &&
                Objects.equals(processName, that.processName) &&
                Objects.equals(metricGroupId, that.metricGroupId) &&
                Objects.equals(measurement, that.measurement) &&
                Objects.equals(metric, that.metric) &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end) &&
                Objects.equals(ag, that.ag) &&
                Objects.equals(remoteSeriesKey, that.remoteSeriesKey) &&
                Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), projectName, hostGroupName, hostName, namespace, service, pod,
                container, processName, metricGroupId, measurement, metric, start, end, ag, remoteSeriesKey);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("projectName", projectName)
                .append("hostGroupName", hostGroupName)
                .append("hostName", hostName)
                .append("namespace", namespace)
                .append("service", service)
                .append("pod", pod)
                .append("container", container)
                .append("processName", processName)
                .append("metricGroupId", metricGroupId)
                .append("measurement", measurement)
                .append("metric", metric)
                .append("start", start)
                .append("end", end)
                .append("ag", ag)
                .append("remoteSeriesKey", remoteSeriesKey)
                .append("data", data)
                .toString();
    }

    public static int calculateGroupHashKey(String project, String hostGroupName, String hostName, String namespace,
                                               String service, String pod, String container, String process,
                                               String measurement,String metricGroupId, String metric) {

        String sb = (project != null ? project : "") +
                (hostGroupName != null ? hostGroupName : "") +
                (hostName != null ? hostName : "") +
                (namespace != null ? namespace : "") +
                (service != null ? service : "") +
                (pod != null ? pod : "") +
                (container != null ? container : "") +
                (process != null ? process : "") +
                // (measurement != null ? measurement : "") +  <-- we can't use measurement because the groupHashKey has
                // to be the same each day, but the measurement changes from time to time. Let it here in code to
                // be aware of it.
                (metricGroupId != null ? metricGroupId : "") +
                (metric != null ? metric : "");

        return sb.hashCode();
    }


    private void updateGroupHashKey() {

        this.groupKeyHash = calculateGroupHashKey(
          projectName,
          hostGroupName,
          hostName,
          namespace,
          service,
          pod,
          container,
          processName,
          measurement,
          metricGroupId,
          metric);
    }
}
