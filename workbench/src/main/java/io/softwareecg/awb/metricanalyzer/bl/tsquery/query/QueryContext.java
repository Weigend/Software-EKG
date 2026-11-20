package io.softwareecg.awb.metricanalyzer.bl.tsquery.query;

/**
 * todo insert comment here
 *
 * @since 30.08.2018
 */
public class QueryContext {

    private QueryFilterParams queryFilterParams;

    private QuerySourceParams querySourceParams;

    private QueryComputeParams queryComputeParams;


    public QueryFilterParams getQueryFilterParams() {
        return queryFilterParams;
    }

    public QuerySourceParams getQuerySourceParams() {
        return querySourceParams;
    }

    public QueryComputeParams getQueryComputeParams() {
        return queryComputeParams;
    }
}
