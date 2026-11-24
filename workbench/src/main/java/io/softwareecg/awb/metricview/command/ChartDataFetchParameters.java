package io.softwareecg.awb.metricview.command;

import io.softwareecg.awb.query.api.ComputationSpec;
import io.softwareecg.awb.query.api.QuerySpec;

/**
 * A container that will used to transport some beans with filters,
 * compute settings and further control properties as single object instance
 * that can also serialized to used it in bookmarks.
 *
 * The container data itself is focused to control the data loading action
 * that will fetch the time series data for the chart.
 *
 * @since 31.08.2018
 */
public class ChartDataFetchParameters {

    private ComputationSpec queryComputeParams = null;

    private QuerySpec queryFilterParams;

    private boolean resetChartAxis = true;

    //=================================================================================================================
    // constructors for various use cases
    //=================================================================================================================

    public ChartDataFetchParameters() {
        queryFilterParams = new QuerySpec();
    }

    public ChartDataFetchParameters(ComputationSpec queryComputeParams) {
        this();
        this.queryComputeParams = queryComputeParams;
    }

    public ChartDataFetchParameters(QuerySpec queryFilterParams, ComputationSpec queryComputeParams) {
        this.queryFilterParams = queryFilterParams;
        this.queryComputeParams = queryComputeParams;
    }

    public ChartDataFetchParameters(QuerySpec queryFilterParams,
                                    ComputationSpec queryComputeParams, boolean resetChartAxis) {
        this.queryFilterParams = queryFilterParams;
        this.queryComputeParams = queryComputeParams;
        this.resetChartAxis = resetChartAxis;
    }

    //=================================================================================================================
    // accessor API
    //=================================================================================================================

    public boolean doResetChartAxis() {
        return resetChartAxis;
    }

    public ComputationSpec getQueryComputeParams() {
        return queryComputeParams;
    }

    public QuerySpec getQueryFilterParams() {
        return queryFilterParams;
    }

    public void setQueryComputeParams(ComputationSpec queryComputeParams) {
        this.queryComputeParams = queryComputeParams;
    }

    public void setQueryFilterParams(QuerySpec queryFilterParams) {
        this.queryFilterParams = queryFilterParams;
    }
}
