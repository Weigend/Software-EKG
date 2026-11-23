package io.softwareecg.awb.query.model;


/**
 * todo enter comment here
 *
 * @since 03.06.2018
 */
public class MetricQuery {

    private QuerySpec queryParams;

    public MetricQuery() {

    }

    public MetricQuery(QuerySpec queryParams) {
        this.queryParams = queryParams;
    }

    public QuerySpec getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(QuerySpec queryParams) {
        this.queryParams = queryParams;
    }
}
