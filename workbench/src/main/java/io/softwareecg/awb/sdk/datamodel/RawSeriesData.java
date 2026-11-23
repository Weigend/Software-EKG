package io.softwareecg.awb.sdk.datamodel;



import io.softwareecg.awb.timeseries.dimension.FilterDimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This RawSeriesData represents a pre version of the SeriesData entity.
 * In contrast to strong typed SeriesData/TimeSeries with it's fixed filter dimensions
 * this bean stores generic data.
 * It will used as indeterminate data structure to collect source data and transport it
 * inside the import modules before it get mapped to the SeriesData.
 *
 * @since 14.07.2018
 */
@SuppressWarnings("UnusedReturnValue")
public class RawSeriesData {

    /**
     * A map with all fetched series label (metadata) that are
     * loaded by the importer from the source interface.
     */
    private Map<String, String> seriesLabels = new HashMap<>();

    // -------------------- payload ---------------------

    /**
     * A list of values that represents each point (timestamp/value tuple)
     * in the time series.
     */
    private List<Value> values = new ArrayList<>();

    /**
     * A timestamp that represents the date and time of the first
     * point in the time series  (= the millisecond than the series starts).
     */
    private long startDate = -1;

    /**
     * A timestamp that represents the date and time of the last
     * point in the time series (= the millisecond than the series ends).
     */
    private long endDate = -1;

    //================================================================================================================
    // constructors
    //================================================================================================================

    /**
     * Default constructor for reflection usage or
     * if no data at instance time is available.
     */
    public RawSeriesData() {
        // no op
    }

    /**
     * Parameterized constructor that will create a
     * new RawSeriesData pre-initialized with the metric name .
     */
    public RawSeriesData(String metricName) {
        this.seriesLabels.put(FilterDimension.METRIC_NAME.name(), metricName);
    }

    //================================================================================================================
    // accessor API of the RawSeriesData class
    //================================================================================================================

    public String getMetricName() {

        if (seriesLabels.containsKey("__name__")) {
            return seriesLabels.get("__name__");
        }

        if (seriesLabels.containsKey(FilterDimension.METRIC_NAME.name())) {
            return seriesLabels.get(FilterDimension.METRIC_NAME.name());
        }

        return null;
    }


    public Map<String, String> getSeriesLabels() {
        return seriesLabels;
    }

    public void setSeriesLabels(Map<String, String> seriesLabels) {
        this.seriesLabels = seriesLabels;
    }

    public RawSeriesData addLabel(String labelName, String labelValue) {
        seriesLabels.put(labelName, labelValue);
        return this;
    }

    public RawSeriesData addLabel(FilterDimension dimension, String labelValue) {
        seriesLabels.put(dimension.name(), labelValue);
        return this;
    }

    public void addValue(Value value) {
        this.values.add(value);
    }

    public List<Value> getValues() {
        return values;
    }

    public String getLabel(String seriesLabelKey) {
        return seriesLabels.get(seriesLabelKey);
    }

    public void setValues(List<Value> values) {
        this.values = values;
    }

    public long getStartDate() {
        return startDate;
    }

    @SuppressWarnings("unused")
    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public RawSeriesData createCopy() {
        RawSeriesData copy = new RawSeriesData();
        copy.setSeriesLabels(new HashMap<>(this.getSeriesLabels()));
        copy.setEndDate(this.getEndDate());
        copy.setEndDate(this.getStartDate());
        copy.setValues(new ArrayList<>(this.getValues()));

        return copy;
    }

    @SuppressWarnings("unused")
    public RawSeriesData createEmptyCopy() {
        RawSeriesData copy = new RawSeriesData();
        copy.setSeriesLabels(new HashMap<>(this.getSeriesLabels()));
        copy.setEndDate(this.getEndDate());
        copy.setEndDate(this.getStartDate());
        copy.setValues(new ArrayList<>());

        return copy;
    }
}
