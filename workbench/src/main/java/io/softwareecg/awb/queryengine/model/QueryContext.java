package io.softwareecg.awb.queryengine.model;

import io.softwareecg.awb.investigation.model.QuerySpec;

import io.softwareecg.awb.investigation.model.ComputationSpec;

/**
 * todo insert comment here
 *
 * @since 30.08.2018
 */
public class QueryContext {

    private QuerySpec queryFilterParams;

    private QuerySourceParams querySourceParams;

    private ComputationSpec queryComputeParams;


    public QuerySpec getQueryFilterParams() {
        return queryFilterParams;
    }

    public QuerySourceParams getQuerySourceParams() {
        return querySourceParams;
    }

    public ComputationSpec getQueryComputeParams() {
        return queryComputeParams;
    }
}
