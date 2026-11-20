package io.softwareecg.awb.metricanalyzer.bl.tsquery.query;

/**
 * todo enter comment here
 *
 * @since 03.06.2018
 */
public class MetricQuery {

    private QueryFilterParams queryParams;

    public MetricQuery() {

    }

    public MetricQuery(QueryFilterParams queryParams) {
        this.queryParams = queryParams;
    }

    public QueryFilterParams getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(QueryFilterParams queryParams) {
        this.queryParams = queryParams;
    }
}
