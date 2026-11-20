package io.softwareecg.awb.metricanalyzer.ui.chartng;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryComputeParams;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.chartng.command.ChartDataFetchParameters;
import io.softwareecg.awb.repository.api.EkgRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * todo insert comment here
 *
 * @since 31.08.2018
 */
public class ChartPanelModel {

    private EkgRepository ekgRepository;

    private final Map<String, ChartDataFetchParameters> queryParamsMap = new HashMap<>();

    private final ChartDataFetchParameters baseChartQueryParams = new ChartDataFetchParameters(new QueryComputeParams());

    private boolean suppressEventHandling;

    //=================================================================================================================
    // ChartPanelModel accessor API
    //=================================================================================================================

    /**
     * Returns the QueryComputeParams hold by the model especially for the
     * base chart.
     *
     * @return the compute parameters that define settings for metric data post processing
     */
    public QueryComputeParams getBaseChartComputeParams() {
        return baseChartQueryParams.getQueryComputeParams();
    }

    /**
     * Sets/overwrites the base chart QueryComputeParams hold by the model
     * with the given one.
     *
     * @param queryComputeParams the compute parameters that define settings for metric data post processing
     */
    public void setBaseChartComputeParams(QueryComputeParams queryComputeParams) {
        baseChartQueryParams.setQueryComputeParams(queryComputeParams);
    }


    /**
     * Returns the QueryFilterParams for the base chart that defines all filter used to fetch and
     * pre-filter the series data at persistence layer.
     *
     * @return the query filters as QueryFilterParams
     */
    public QueryFilterParams getBaseChartFilterParams() {
        return baseChartQueryParams.getQueryFilterParams();
    }

    /**
     * Sets/overwrites the QueryFilterParams for the base chart that defines all filter used to fetch and
     * pre-filter the series data at persistence layer.
     *
     * @param filterParams the query filters as QueryFilterParams
     */
    public void setBaseChartFilterParams(QueryFilterParams filterParams) {
        baseChartQueryParams.setQueryFilterParams(filterParams);
    }


    public QueryFilterParams getFilterParamsForChartId(String chartId) {

        if (!queryParamsMap.containsKey(chartId)) {
            return null;
        }

        return queryParamsMap.get(chartId).getQueryFilterParams();
    }

    public EkgRepository getEkgRepository() {
        return ekgRepository;
    }

    public void setEkgRepository(EkgRepository ekgRepository) {
        this.ekgRepository = ekgRepository;
    }

    public boolean isSuppressEventHandling() {
        return suppressEventHandling;
    }

    public void suppressEventHandling(boolean suppress) {
        suppressEventHandling = suppress;
    }

    //=================================================================================================================
    // internal model logic
    //=================================================================================================================


    private void registerFilterParam(String chartId, QueryFilterParams filterParams) {

        if (!queryParamsMap.containsKey(chartId)) {
            queryParamsMap.put(chartId, new ChartDataFetchParameters());
        }

        queryParamsMap.get(chartId).setQueryFilterParams(filterParams);
    }
}
