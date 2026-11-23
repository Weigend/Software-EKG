package io.softwareecg.awb.metricview;

import io.softwareecg.awb.query.model.ComputationSpec;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.metricview.command.ChartDataFetchParameters;
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

    private final ChartDataFetchParameters baseChartQueryParams = new ChartDataFetchParameters(new ComputationSpec());

    private boolean suppressEventHandling;

    //=================================================================================================================
    // ChartPanelModel accessor API
    //=================================================================================================================

    /**
     * Returns the ComputationSpec hold by the model especially for the
     * base chart.
     *
     * @return the compute parameters that define settings for metric data post processing
     */
    public ComputationSpec getBaseChartComputeParams() {
        return baseChartQueryParams.getQueryComputeParams();
    }

    /**
     * Sets/overwrites the base chart ComputationSpec hold by the model
     * with the given one.
     *
     * @param queryComputeParams the compute parameters that define settings for metric data post processing
     */
    public void setBaseChartComputeParams(ComputationSpec queryComputeParams) {
        baseChartQueryParams.setQueryComputeParams(queryComputeParams);
    }


    /**
     * Returns the QuerySpec for the base chart that defines all filter used to fetch and
     * pre-filter the series data at persistence layer.
     *
     * @return the query filters as QuerySpec
     */
    public QuerySpec getBaseChartFilterParams() {
        return baseChartQueryParams.getQueryFilterParams();
    }

    /**
     * Sets/overwrites the QuerySpec for the base chart that defines all filter used to fetch and
     * pre-filter the series data at persistence layer.
     *
     * @param filterParams the query filters as QuerySpec
     */
    public void setBaseChartFilterParams(QuerySpec filterParams) {
        baseChartQueryParams.setQueryFilterParams(filterParams);
    }


    public QuerySpec getFilterParamsForChartId(String chartId) {

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


    private void registerFilterParam(String chartId, QuerySpec filterParams) {

        if (!queryParamsMap.containsKey(chartId)) {
            queryParamsMap.put(chartId, new ChartDataFetchParameters());
        }

        queryParamsMap.get(chartId).setQueryFilterParams(filterParams);
    }
}
