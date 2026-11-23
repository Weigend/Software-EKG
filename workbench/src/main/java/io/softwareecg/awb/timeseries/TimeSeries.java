/*
  ______________________________________________________________________________
  <p>
  Project: Software EKG
  ______________________________________________________________________________
  <p>
  created by: f.lautenschlager
  creation date: 11.02.14 16:09
  description:
  ______________________________________________________________________________
  <p>
  Copyright: (c) Weigend AM, all rights reserved
  ______________________________________________________________________________
 */
package io.softwareecg.awb.timeseries;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the values of a specific metric.
 * Internally backed by primitive long[]/double[] arrays for high-performance processing.
 *
 */
public class TimeSeries {

    private static final long[] EMPTY_LONG = new long[0];
    private static final double[] EMPTY_DOUBLE = new double[0];
    private static final int INITIAL_CAPACITY = 256;

    // ----------------- classic filter dimension ------------------

    private String hostGroup;
    private String host;

    // -------------- cloud native filter dimension ----------------

    private String namespace;
    private String service;
    private String pod;
    private String container;

    // ----------------- generic filter dimension ------------------

    private String project;
    private String measurement;
    private String process;
    private String processGroup;
    private String metricGroup;
    private final String metricName;

    // ----------------- time series data ------------------

    private long[] timestamps = EMPTY_LONG;
    private double[] valuesArray = EMPTY_DOUBLE;
    private int size;

    private long startDate;
    private long endDate;
    private String remoteSeriesKey;

    // ==============================================================================================================
    //  constructors
    // ==============================================================================================================

    public TimeSeries(String metricName) {
        this.metricName = metricName;
    }

    public TimeSeries(final String project,
                      final String hostGroup,
                      final String host,
                      final String namespace,
                      final String service,
                      final String pod,
                      final String container,
                      final String measurement,
                      final String processGroup,
                      final String process,
                      final String metricGroup,
                      final String metricName) {

        this.project = project;
        this.hostGroup = hostGroup;
        this.host = host;
        this.namespace = namespace;
        this.service = service;
        this.pod = pod;
        this.container = container;
        this.measurement = measurement;
        this.processGroup = processGroup;
        this.process = process;
        this.metricGroup = metricGroup;
        this.metricName = metricName;
    }

    public TimeSeries(final String project,
                      final String hostGroup,
                      final String host,
                      final String namespace,
                      final String service,
                      final String pod,
                      final String container,
                      final String measurement,
                      final String processGroup,
                      final String process,
                      final String group,
                      final String metricName,
                      final List<Value> values) {

        this(project, hostGroup, host, namespace, service, pod, container,
                measurement, processGroup, process, group, metricName);
        addAll(values);
    }

    // ==============================================================================================================
    //  array-based data access (primary API)
    // ==============================================================================================================

    public long[] getTimestamps() {
        if (size == 0) return EMPTY_LONG;
        return (size == timestamps.length) ? timestamps : Arrays.copyOf(timestamps, size);
    }

    public double[] getValuesArray() {
        if (size == 0) return EMPTY_DOUBLE;
        return (size == valuesArray.length) ? valuesArray : Arrays.copyOf(valuesArray, size);
    }

    public int getPointCount() {
        return size;
    }

    /**
     * Sets pre-sorted primitive arrays as the data for this TimeSeries.
     * The arrays are used as-is (not copied) and must be sorted by timestamp ascending.
     */
    public void setSortedArrays(long[] timestamps, double[] values) {
        if (timestamps == null || values == null || timestamps.length == 0) {
            this.timestamps = EMPTY_LONG;
            this.valuesArray = EMPTY_DOUBLE;
            this.size = 0;
            return;
        }
        this.timestamps = timestamps;
        this.valuesArray = values;
        this.size = timestamps.length;
        this.startDate = timestamps[0];
        this.endDate = timestamps[size - 1];
    }

    // ==============================================================================================================
    //  List<Value> bridge (for callers not yet migrated to arrays)
    // ==============================================================================================================

    /**
     * Materializes the internal arrays as a new List of Value objects.
     * The returned list is a NEW instance — modifications do NOT affect this TimeSeries.
     */
    public List<Value> getValues() {
        List<Value> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new Value(timestamps[i], valuesArray[i]));
        }
        return result;
    }

    /**
     * Extracts timestamps and values from the list and stores them as arrays.
     */
    public void setSortedValues(List<Value> sortedValues) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            this.timestamps = EMPTY_LONG;
            this.valuesArray = EMPTY_DOUBLE;
            this.size = 0;
            return;
        }
        int n = sortedValues.size();
        long[] ts = new long[n];
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) {
            Value v = sortedValues.get(i);
            ts[i] = v.getTimestamp();
            vals[i] = v.getValue();
        }
        setSortedArrays(ts, vals);
    }

    // ==============================================================================================================
    //  mutating operations
    // ==============================================================================================================

    public final void addValue(Value value) {
        if (value == null) {
            return;
        }
        ensureCapacity(size + 1);
        timestamps[size] = value.getTimestamp();
        valuesArray[size] = value.getValue();
        updateStartEndDate(value.getTimestamp());
        size++;
    }

    /**
     * Merges the given sorted values into this time series using a sorted merge.
     * The result is always sorted by timestamp.
     */
    public final void addAll(List<Value> sortedValues) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return;
        }

        // Convert foreign list to arrays
        int foreignSize = sortedValues.size();
        long[] foreignTs = new long[foreignSize];
        double[] foreignVals = new double[foreignSize];
        for (int i = 0; i < foreignSize; i++) {
            Value v = sortedValues.get(i);
            foreignTs[i] = v.getTimestamp();
            foreignVals[i] = v.getValue();
        }

        mergeArrays(foreignTs, foreignVals);
    }

    /**
     * Appends arrays to this time series without sorting.
     * Call {@link #sortByTimestamp()} after all appends are done.
     * Uses amortized doubling for O(N) total cost across many appends.
     */
    public void appendArrays(long[] foreignTs, double[] foreignVals) {
        if (foreignTs == null || foreignTs.length == 0) {
            return;
        }
        int foreignLen = foreignTs.length;
        ensureCapacity(size + foreignLen);
        System.arraycopy(foreignTs, 0, timestamps, size, foreignLen);
        System.arraycopy(foreignVals, 0, valuesArray, size, foreignLen);
        size += foreignLen;
    }

    /**
     * Sorts the internal arrays by timestamp ascending and updates start/end dates.
     * Call once after all {@link #appendArrays} calls are done.
     */
    public void sortByTimestamp() {
        if (size <= 1) {
            if (size == 1) {
                startDate = timestamps[0];
                endDate = timestamps[0];
            }
            return;
        }

        // Create index array and sort by timestamp
        Integer[] indices = new Integer[size];
        for (int i = 0; i < size; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Long.compare(timestamps[a], timestamps[b]));

        // Check if already sorted — skip rearrangement
        boolean sorted = true;
        for (int i = 0; i < size; i++) {
            if (indices[i] != i) { sorted = false; break; }
        }
        if (sorted) {
            startDate = timestamps[0];
            endDate = timestamps[size - 1];
            return;
        }

        // Rearrange both arrays according to sorted order
        long[] sortedTs = new long[size];
        double[] sortedVals = new double[size];
        for (int i = 0; i < size; i++) {
            sortedTs[i] = timestamps[indices[i]];
            sortedVals[i] = valuesArray[indices[i]];
        }
        this.timestamps = sortedTs;
        this.valuesArray = sortedVals;
        this.startDate = sortedTs[0];
        this.endDate = sortedTs[size - 1];
    }

    /**
     * Merges the given sorted arrays into this time series using a sorted merge.
     * Both arrays must be sorted by timestamp ascending.
     */
    public void mergeArrays(long[] foreignTs, double[] foreignVals) {
        if (foreignTs == null || foreignTs.length == 0) {
            return;
        }

        if (size == 0) {
            this.timestamps = Arrays.copyOf(foreignTs, foreignTs.length);
            this.valuesArray = Arrays.copyOf(foreignVals, foreignVals.length);
            this.size = foreignTs.length;
            this.startDate = timestamps[0];
            this.endDate = timestamps[size - 1];
            return;
        }

        // Trim own arrays to actual size for clean merge
        long[] ownTs = (size == timestamps.length) ? timestamps : Arrays.copyOf(timestamps, size);
        double[] ownVals = (size == valuesArray.length) ? valuesArray : Arrays.copyOf(valuesArray, size);

        int newSize = size + foreignTs.length;
        long[] mergedTs = new long[newSize];
        double[] mergedVals = new double[newSize];

        // Sorted merge — both inputs are sorted by timestamp
        int i = 0, j = 0, k = 0;
        while (i < ownTs.length && j < foreignTs.length) {
            if (ownTs[i] <= foreignTs[j]) {
                mergedTs[k] = ownTs[i];
                mergedVals[k] = ownVals[i];
                i++;
            } else {
                mergedTs[k] = foreignTs[j];
                mergedVals[k] = foreignVals[j];
                j++;
            }
            k++;
        }
        while (i < ownTs.length) {
            mergedTs[k] = ownTs[i];
            mergedVals[k] = ownVals[i];
            i++;
            k++;
        }
        while (j < foreignTs.length) {
            mergedTs[k] = foreignTs[j];
            mergedVals[k] = foreignVals[j];
            j++;
            k++;
        }

        this.timestamps = mergedTs;
        this.valuesArray = mergedVals;
        this.size = newSize;
        this.startDate = mergedTs[0];
        this.endDate = mergedTs[newSize - 1];

        // Edge case: all values at the same timestamp → sum them
        if (startDate == endDate && newSize > 1) {
            double sum = 0;
            for (int idx = 0; idx < newSize; idx++) {
                sum += mergedVals[idx];
            }
            this.timestamps = new long[]{startDate};
            this.valuesArray = new double[]{sum};
            this.size = 1;
        }
    }

    public void clear() {
        this.timestamps = EMPTY_LONG;
        this.valuesArray = EMPTY_DOUBLE;
        this.size = 0;
        this.startDate = -1;
        this.endDate = -1;
    }

    // ==============================================================================================================
    //  metadata
    // ==============================================================================================================

    public String getGroupingKey(boolean excludeMeasurement) {
        return metricName + metricGroup + host + hostGroup + namespace + service + pod + container +
                process + processGroup + (excludeMeasurement ? "" : measurement);
    }

    public String getRemoteSeriesKey() {
        return remoteSeriesKey;
    }

    public void setRemoteSeriesKey(String remoteSeriesKey) {
        this.remoteSeriesKey = remoteSeriesKey;
    }

    public void mergeMetaData(TimeSeries timeSeries) {
        this.metricGroup = mergeMetaValue(this.metricGroup, timeSeries.metricGroup);
        this.host = mergeMetaValue(this.host, timeSeries.host);
        this.hostGroup = mergeMetaValue(this.hostGroup, timeSeries.hostGroup);
        this.namespace = mergeMetaValue(this.namespace, timeSeries.namespace);
        this.service = mergeMetaValue(this.service, timeSeries.service);
        this.pod = mergeMetaValue(this.pod, timeSeries.pod);
        this.container = mergeMetaValue(this.container, timeSeries.container);
        this.measurement = mergeMetaValue(this.measurement, timeSeries.measurement);
        this.process = mergeMetaValue(this.process, timeSeries.process);
        this.processGroup = mergeMetaValue(this.processGroup, timeSeries.processGroup);
        this.project = mergeMetaValue(this.project, timeSeries.project);
    }

    private String mergeMetaValue(String ownValue, String foreignValue) {
        if (StringUtils.isBlank(ownValue)) return foreignValue;
        if (StringUtils.isBlank(foreignValue)) return ownValue;
        return ownValue.equals(foreignValue) ? ownValue : "*";
    }

    public String getHostGroup() { return hostGroup; }
    public String getHost() { return host; }
    public String getNamespace() { return namespace; }
    public String getService() { return service; }
    public String getPod() { return pod; }
    public String getContainer() { return container; }
    public String getMetricName() { return metricName; }
    public String getProcess() { return process; }
    public String getProcessGroup() { return processGroup; }
    public void setProcessGroup(String processGroup) { this.processGroup = processGroup; }
    public String getMetricGroup() { return metricGroup; }
    public String getMeasurement() { return measurement; }
    public void setMeasurement(String measurement) { this.measurement = measurement; }
    public long getStartDate() { return startDate; }
    public long getEndDate() { return endDate; }
    public String getProject() { return project; }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        appendDimension(sb, getMeasurement());
        appendDimension(sb, getHost());
        appendDimension(sb, getHostGroup());
        appendDimension(sb, getNamespace());
        appendDimension(sb, getPod());
        appendDimension(sb, getProcess());
        appendDimension(sb, getMetricGroup());
        sb.append(getMetricName());
        return sb.toString();
    }

    private void appendDimension(StringBuilder sb, String value) {
        if (StringUtils.isNotBlank(value) && !"*".equals(value)) {
            sb.append(value).append(" | ");
        }
    }

    public long size() {
        return size;
    }

    public boolean empty() {
        return size == 0;
    }

    public Counter toCounter() {
        Counter counter = new Counter(metricName + "[" + process + "," + host + "]");
        counter.setValues(Collections.unmodifiableList(getValues()));
        return counter;
    }

    // ==============================================================================================================
    //  internal helpers
    // ==============================================================================================================

    private void ensureCapacity(int minCapacity) {
        if (timestamps == EMPTY_LONG || timestamps.length == 0) {
            int cap = Math.max(INITIAL_CAPACITY, minCapacity);
            timestamps = new long[cap];
            valuesArray = new double[cap];
        } else if (minCapacity > timestamps.length) {
            int newCap = Math.max(timestamps.length + (timestamps.length >> 1), minCapacity);
            timestamps = Arrays.copyOf(timestamps, newCap);
            valuesArray = Arrays.copyOf(valuesArray, newCap);
        }
    }

    private void updateStartEndDate(long timestamp) {
        if (size == 0) {
            startDate = timestamp;
            endDate = timestamp;
        } else {
            if (timestamp < startDate) startDate = timestamp;
            if (timestamp > endDate) endDate = timestamp;
        }
    }

    // ==============================================================================================================
    //  equals / hashCode / toString
    // ==============================================================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSeries that = (TimeSeries) o;
        return Objects.equals(hostGroup, that.hostGroup) &&
                Objects.equals(host, that.host) &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(service, that.service) &&
                Objects.equals(pod, that.pod) &&
                Objects.equals(container, that.container) &&
                Objects.equals(project, that.project) &&
                Objects.equals(measurement, that.measurement) &&
                Objects.equals(process, that.process) &&
                Objects.equals(processGroup, that.processGroup) &&
                Objects.equals(metricGroup, that.metricGroup) &&
                Objects.equals(metricName, that.metricName) &&
                Objects.equals(remoteSeriesKey, that.remoteSeriesKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostGroup, host, namespace, service, pod, container, project,
                measurement, process, processGroup, metricGroup, metricName, remoteSeriesKey);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("metricName", metricName)
                .append("host", host)
                .append("process", process)
                .append("processGroup", processGroup)
                .append("metricGroup", metricGroup)
                .append("measurement", measurement)
                .append("project", project)
                .append("size", size)
                .append("startDate", startDate)
                .append("endDate", endDate)
                .toString();
    }
}
