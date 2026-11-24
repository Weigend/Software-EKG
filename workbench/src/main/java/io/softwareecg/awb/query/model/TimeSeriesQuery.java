package io.softwareecg.awb.query.model;

/**
 * Asks for a page of the parts of the series that are stored.
 *
 * @since 02.06.2018
 */
public class TimeSeriesQuery {

    public static final String INITIAL_CURSOR_ID = "*";

    private final QuerySpec queryParams;

    private final int maxMetricLimit;

    private String cursorId = null;

    public TimeSeriesQuery(QuerySpec queryParams, int maxMetricLimit) {
        this.queryParams = queryParams;
        this.maxMetricLimit = maxMetricLimit;
    }

    public QuerySpec getQueryParams() {
        return queryParams;
    }

    public void setCursorId(String cursorId) {
        this.cursorId = cursorId;
    }

    public String getCursorId() {
        return cursorId;
    }

    public int getMaxMetricLimit() {
        return maxMetricLimit;
    }
}
