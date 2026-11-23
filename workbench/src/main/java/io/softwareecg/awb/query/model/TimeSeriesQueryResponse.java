package io.softwareecg.awb.query.model;

import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * todo enter comment here
 *
 * @since 02.06.2018
 */
public class TimeSeriesQueryResponse {

    private final String cursorId;

    private final long totalHits;

    private boolean requestAborted;

    private final Collection<TimeSeries> responseData = Collections.synchronizedList(new ArrayList<>());

    /**
     *
     * @param cursorId
     */
    public TimeSeriesQueryResponse(String cursorId, long totalHits, boolean requestAborted) {
        this.cursorId = cursorId;
        this.totalHits = totalHits;
        this.requestAborted = requestAborted;
    }

    /**
     *
     * @return
     */
    public boolean isRequestAborted() {
        return requestAborted;
    }

    public void setRequestAborted(boolean requestAborted) {
        this.requestAborted = requestAborted;
    }

    /**
     *
     * @return
     */
    public long getTotalHits() {
        return totalHits;
    }

    /**
     *
     * @return
     */
    public boolean isConsumed() {
       return cursorId == null || responseData == null || responseData.isEmpty();
    }

    /**
     *
     * @return
     */
    public Collection<TimeSeries> getData() {
       return responseData;
    }

    /**
     *
     * @return
     */
    public String getCursorId() {
        return cursorId;
    }

    public void addTimeSeries(TimeSeries timeSeries) {
        responseData.add(timeSeries);
    }
}
