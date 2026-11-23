package io.softwareecg.awb.metricview;

import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;
import io.softwareecg.awb.investigation.model.ComputationSpec;
import io.softwareecg.awb.investigation.model.QuerySpec;
import io.softwareecg.awb.queryengine.ComputedTimeSeriesResponse;
import io.softwareecg.awb.queryengine.MetricQueryService;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.runtime.events.AwbErrorEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import org.slf4j.Logger;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;

/**
 * Task that will fetch computed time series data from types,
 * convert and it to JavaFX series data and write it directly to the chart data model
 *
 * @since 30.08.2018
 */
public class FillChartDataTask extends Task<List<XYChart.Series<Long, Double>>> {

    /**
     * Logger to protocol errors and special events
     */
    private static final Logger LOGGER = EkgLogger.get();

    /**
     * One day in milliseconds (factors: 1sec, 1min, 1 hour, 1 day)
     */
    private static final long ONE_DAY_IN_MS = 1000 * 60 * 60 * 24;

    /**
     * The maximum amount of time series returned to the caller of fetching data from the EKG repository.
     * If exceeded the fetching and compute logic stops and return an empty array (and notifies about it).
     */
    static final int MAX_ALLOWED_SERIES_EKG_REPO = 250;

    /**
     * The maximum amount of time series returned to the caller in case of split-source projects that
     * fetches the data from an remote repository. If exceeded the fetching and compute logic stops
     * and return an empty array (and notifies about it).
     */
    private static final int MAX_ALLOWED_SERIES_REMOTE_REPO = 50;

    /**
     * the query parameter that will use to fetch the time series
     */
    private final QuerySpec filterParams;

    /**
     * the compute parameters that define various setting for the post computing
     * steps on the fetched time series data like sampling or aggregation settings
     */
    private final ComputationSpec computeParams;

    /**
     * boolean flag that controls if the chart axis should reset than new data will loaded or not.
     * To to reset axis will need for use cases like zooming that will request new data but wan't
     * display it with auto ranging axis.
     */
    private final boolean resetChartAxis;

    /**
     * the EKG repository that stores the data to fetch
     */
    private final EkgRepository repository;

    /**
     * the target of all fetched and computed data
     */
    private final ZoomableStackedChart zoomableStackedChart;

    /**
     * Constructs a new instance of FillChartDataTask which will use
     * the given setup parameters for fetching and postprocessing the data.
     *
     * @param filterParams the query parameter that define which metrics in which time range should be fetched
     * @param computeParams the compute parameters that define various setting for the post computing steps on the
     *                    fetched time series data like sampling or aggregation settings
     * @param resetChartAxis do reset chart axis on load or not
     * @param repository the concrete EKG repository that stores the data to fetch
     * @param zoomableStackedChart the target chart of the data
     */
    public FillChartDataTask(QuerySpec filterParams, ComputationSpec computeParams, boolean resetChartAxis,
                             EkgRepository repository, ZoomableStackedChart zoomableStackedChart) {

        this.filterParams = filterParams;
        this.computeParams = computeParams;
        this.resetChartAxis = resetChartAxis;
        this.repository = repository;
        this.zoomableStackedChart = zoomableStackedChart;
    }

    @Override
    protected List<XYChart.Series<Long, Double>> call() {

        long startTime = System.currentTimeMillis();
        LOGGER.info("Start with chart data fetching & post-processing");

        try {
            // the query service that belongs to the types we use
            MetricQueryService service = repository.getBoundedService(MetricQueryService.class);

            // list as collector for the results
            List<XYChart.Series<Long, Double>> result = new ArrayList<>();

            // be aware the filter also is used then the user span a zoom rectangle
            // see getZoomStart(), we need to add on day to the end, otherwise the user didn't get the expected range
            if (filterParams.getEnd() > 0) {
                filterParams.setEnd(filterParams.getEnd() + ONE_DAY_IN_MS);
            }

            boolean isSplitSourceProject = filterParams.getProject().useSplitSource();
            int metricLimit;

            if (isSplitSourceProject) {
                metricLimit = MAX_ALLOWED_SERIES_REMOTE_REPO;
            } else {
                metricLimit = MAX_ALLOWED_SERIES_EKG_REPO;
            }

            // query counters from database and retrieve a parallel stream as result
            ComputedTimeSeriesResponse response = service.getComputedTimeSeries(filterParams, computeParams, metricLimit);

            if (response.isRequestAborted()) {

                if (response.isMaxSeriesLimitExceeded()) {
                    LOGGER.info("Retrieved a max series limit exceeded error after {}ms.",
                            (System.currentTimeMillis() - startTime));
                    showMaxSeriesExceededDialog(response.getTotalHits(), metricLimit, isSplitSourceProject);

                }else if (response.hasErrorPayload()) {
                    LOGGER.info("The computation of the request has aborted as result of an error. Exception message: {} ",
                            response.getOccurredError().getMessage());
                } else {
                    LOGGER.info("The computation of the request has aborted as result of an interruption. Compute time: {}ms.",
                            (System.currentTimeMillis() - startTime));
                }

                return new ArrayList<>();
            }

            LOGGER.info("Retrieved time series data after {}ms", (System.currentTimeMillis() - startTime));

            if (response.getTimeSeries().size() > MAX_ALLOWED_SERIES_EKG_REPO) {
                zoomableStackedChart.clearBase(resetChartAxis);
                zoomableStackedChart.setBaseYAxisLabel(filterParams.getFullQualifiedDisplayName());
                LOGGER.warn("The result has more than '" + MAX_ALLOWED_SERIES_EKG_REPO + "' series. Return empty array to protect the UI.");
                return new ArrayList<>();
            }

            super.updateMessage("Processing " + response.getTimeSeries().size() + " series");
            result.addAll(toChartSeries(response));

            // show "And More..." hint when series were truncated due to display limit
            if (response.hasSeriesTruncated()) {
                String truncatedInfo = "... and " + response.getTruncatedSeriesCount() + " more series";
                XYChart.Series<Long, Double> placeholderSeries = new XYChart.Series<>(FXCollections.observableArrayList());
                placeholderSeries.nameProperty().setValue(truncatedInfo);
                result.add(placeholderSeries);

                super.updateMessage("Loaded " + (result.size() - 1) + " series. " + truncatedInfo + " not displayed.");
            } else {
                super.updateMessage("Successfully loaded " + result.size() + " series.");
            }

            // Show all series in a single UI update. The series that were shown before keep what the
            // user set for them - a load is often only the same chart with newer values.
            zoomableStackedChart.setBaseYAxisLabel(filterParams.getFullQualifiedDisplayName());
            zoomableStackedChart.replaceBaseSeries(result, resetChartAxis);
            LOGGER.info("Calculate chart data in " + (System.currentTimeMillis() - startTime) + "ms");

            return result;

        } catch (Exception e) {

            if (e instanceof IllegalStateException && e.getCause() instanceof InterruptedException) {
                return new ArrayList<>();
            }

            LOGGER.error("Exception occurred during fetch of time series data for the AWB chart.", e);
            EventBus<EventObject> bus = Lookup.lookup(EventBus.class);
            bus.publish(new AwbErrorEvent(this, e));
            throw e;
        }
    }

    /**
     * Turns the series of a result into series of a chart.
     *
     * @param response the result of a query
     * @return one chart series per time series, named as the series is displayed
     */
    static List<XYChart.Series<Long, Double>> toChartSeries(ComputedTimeSeriesResponse response) {
        List<XYChart.Series<Long, Double>> result = new ArrayList<>();
        response.getTimeSeries().forEach(series -> {

            // we do not sort metric values because this should happen at the import process!!
            ObservableList<XYChart.Data<Long, Double>> chartSeriesData = FXCollections.observableArrayList();

            // convert primitive arrays directly to XYChart.Data points — no Value allocation
            long[] ts = series.getTimestamps();
            double[] vals = series.getValuesArray();
            for (int i = 0; i < series.getPointCount(); i++) {
                chartSeriesData.add(new XYChart.Data<>(ts[i], vals[i]));
            }

            // create new JavaFX series and addAndSum to list that will returned
            XYChart.Series<Long, Double> chartSeries = new XYChart.Series<>(chartSeriesData);
            chartSeries.nameProperty().setValue(series.getDisplayName());

            result.add(chartSeries);
        });
        return result;
    }

    private void showMaxSeriesExceededDialog(long totalHitTimeSeries, int seriesLimit, boolean isSplitSourceProject) {
        Platform.runLater(() -> {

            String errorDetailSplitSource = "\nWhen using split-source projects, the limit " +
                    "for the\nmaximum number of dedicated metrics is ";

            String errorDetailStandard = "That should be too much even for you!\n\nThe limit is ";

            String errorDetail = isSplitSourceProject ? errorDetailSplitSource : errorDetailStandard;

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Maximum Series Limit Exceeded");
            alert.setHeaderText("Maximum allowed number of result records exceeded!");
            alert.setContentText("\nThe query with the selected filter returned "
                    + NumberFormat.getInstance(Locale.GERMAN).format(totalHitTimeSeries)
                    + " different time series. " + errorDetail +
                    + seriesLimit + " entries. \nNarrow down the filters or select 'Combine Metrics' " +
                    "to visualize data.");

            alert.setHeight(300);

            alert.showAndWait();
        });
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        /*
        try {
            List<XYChart.Series<Long, Double>> seriesList = super.get();
            model.updateChart(seriesList);
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.error("", ex);
        } finally {
            model.setUpdateStatusToFinished();
            super.updateProgress(0, 0); // make sure the progress bar resets
        }*/
    }

    @Override
    protected void failed() {
        super.failed();
        /*
        super.updateProgress(0, 0); // make sure the progress bar resets
        super.updateMessage("Error: " + super.getException());
        model.setUpdateStatusToFinished();
        */
    }
}
