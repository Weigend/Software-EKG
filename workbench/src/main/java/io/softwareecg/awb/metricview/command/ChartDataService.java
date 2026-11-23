package io.softwareecg.awb.metricview.command;

import io.softwareecg.awb.investigation.model.ComputationSpec;
import io.softwareecg.awb.investigation.model.QuerySpec;
import javafx.concurrent.Service;

/**
 * A JavaFX service base class that should derived by classes
 * that are focused on reloading chart data from the persistence layer.
 *
 * For this the base class provides some accsessors to set and retrieve
 * the query and post compute parameters
 *
 * @since 31.08.2018
 */
@SuppressWarnings("ALL")
public abstract class ChartDataService extends Service {

    private QuerySpec filterParams;

    private ComputationSpec computeParams;

    private boolean resetChartAxis;

    /**
     * Returns a boolean flag that control if the chart axis and
     * it's bounds should be reset or not
     *
     * @return true if the chart axis should reset, otherwise false
     */
    public boolean doResetChartAxis() {
        return resetChartAxis;
    }

    /**
     * Sets a boolean flag that control if the chart axis and it's
     * bounds should be reset or not
     *
     * @param resetChartAxis true if the chart axis should reset, otherwise false
     */
    public void setResetChartAxis(boolean resetChartAxis) {
        this.resetChartAxis = resetChartAxis;
    }

    /**
     * Sets a bean that contains 0-* filter parameters used
     * by the the query layer to filter time series data before
     * it will returned.
     *
     * @param params a bean with query parameters
     */
    public void setQueryFilterParams(QuerySpec params) {
        this.filterParams = params;
    }

    /**
     * Sets a bean that contains a couple of setttings that control how
     * to post process the retunred time series data from the peristence
     * layer. For example if the series should combinded or not.
     *
     * @param params a bean with settings for the post processing actions
     */
    public void setQueryComputeParams(ComputationSpec params) {
        this.computeParams = params;
    }

    /**
     * Returns a bean that contains 0-* filter parameters used
     * by the the query layer to filter time series data before
     * it will returned.
     *
     * @return the filter parameters used for the database query
     */
    protected QuerySpec getFilterParams() {
        return filterParams;
    }

    /**
     * Returns a bean that contains a couple of setttings that control how
     * to post process the retunred time series data from the peristence
     * layer. For example if the series should combinded or not.
     *
     * @return the settings that contols the logic behavoir of the post processing
     */
    protected ComputationSpec getComputeParams() {
        return computeParams;
    }
}
