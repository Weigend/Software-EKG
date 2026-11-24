package io.softwareecg.awb.query.api;

import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.ArrayList;
import java.util.List;

/**
 * todo insert comment here
 *
 * @since 05.11.2018
 */
public class ComputedTimeSeriesResponse {

    private long totalSeries;

    private final int usedMaxSeriesLimit;

    private boolean requestAborted = false;

    private Throwable occurredError = null;

    /**
     * Number of additional series that were omitted because the display limit was reached.
     * 0 means no series were truncated.
     */
    private int truncatedSeriesCount = 0;

    List<TimeSeries> timeSeries = new ArrayList<>();

    public ComputedTimeSeriesResponse(long totalHits, int maxMetricLimit) {
        this.usedMaxSeriesLimit = maxMetricLimit;
        this.totalSeries = totalHits;
        this.requestAborted = true;
    }

    public ComputedTimeSeriesResponse(int maxMetricLimit, RepositoryException exception) {
        this.usedMaxSeriesLimit = maxMetricLimit;
        this.totalSeries = 0;
        this.requestAborted = true;
        this.occurredError = exception;
    }

    public ComputedTimeSeriesResponse(boolean requestAborted, int maxMetricLimit) {
        this.requestAborted = requestAborted;
        this.usedMaxSeriesLimit = maxMetricLimit;
    }

    public ComputedTimeSeriesResponse(List<TimeSeries> timeSeries, int maxMetricLimit) {
        this.timeSeries = timeSeries;
        this.usedMaxSeriesLimit = maxMetricLimit;
        this.totalSeries = timeSeries.size();
    }

    public Throwable getOccurredError() {
        return occurredError;
    }

    public long getTotalHits() {
        return totalSeries;
    }

    public int getUsedMaxSeriesLimit() {
        return usedMaxSeriesLimit;
    }

    public boolean isRequestAborted() {
        return requestAborted || isMaxSeriesLimitExceeded();
    }

    public boolean isMaxSeriesLimitExceeded() {
        return totalSeries > usedMaxSeriesLimit;
    }

    public List<TimeSeries> getTimeSeries() {
        return timeSeries;
    }

    public boolean hasErrorPayload() {
        return occurredError != null;
    }

    /**
     * Returns the number of series that were omitted because the display limit was reached.
     *
     * @return number of truncated series (0 if none)
     */
    public int getTruncatedSeriesCount() {
        return truncatedSeriesCount;
    }

    /**
     * Sets the number of series that were omitted due to the display limit.
     *
     * @param truncatedSeriesCount the number of omitted series
     */
    public void setTruncatedSeriesCount(int truncatedSeriesCount) {
        this.truncatedSeriesCount = truncatedSeriesCount;
    }

    /**
     * Returns true if some series were omitted due to the display limit.
     *
     * @return true if series were truncated
     */
    public boolean hasSeriesTruncated() {
        return truncatedSeriesCount > 0;
    }
}
