package io.softwareecg.awb.metricview;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.SeriesDataWrittenEvent;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import javafx.scene.chart.ValueAxis;
import java.util.Objects;
import io.softwareecg.awb.uitoolkit.chart.ChartType;
import io.softwareecg.awb.uitoolkit.chart.ColoredChart;
import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;
import io.softwareecg.awb.uitoolkit.chart.axis.DateAxis;
import io.softwareecg.awb.uitoolkit.chart.axis.MetricAxis;
import io.softwareecg.awb.uitoolkit.chart.legend.LegendActionCallbacks;
import io.softwareecg.awb.uitoolkit.chart.legend.ChartLegendPort;
import io.softwareecg.awb.uitoolkit.chart.zoom.Zoomable;
import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import io.softwareecg.awb.query.api.SeriesCombineMode;
import io.softwareecg.awb.query.api.SeriesSmoothingGranularity;
import io.softwareecg.awb.query.api.SeriesSmoothingType;
import io.softwareecg.awb.bookmark.api.BookmarkContent;
import io.softwareecg.awb.bookmark.api.BookmarkDialogs;
import io.softwareecg.awb.bookmark.api.MetricsBookmarkService;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.query.api.OpenInvestigationCommand;
import io.softwareecg.awb.query.api.ComputationSpec;
import io.softwareecg.awb.query.api.QuerySpec;
import io.softwareecg.awb.bookmark.api.BookmarkEvent;
import io.softwareecg.awb.evidence.export.ExcelExportSeriesData;
import io.softwareecg.awb.evidence.export.ExcelExporter;
import io.softwareecg.awb.evidence.export.ScreenshotExportDialog;
import io.softwareecg.awb.metricview.queryeditor.QueryEditorPort;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.skin.CellSkinBase;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import io.softwareecg.awb.query.api.ComputedTimeSeriesResponse;
import io.softwareecg.awb.query.api.MetricQueryService;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.softwareecg.awb.metricview.command.ChartCommand;
import io.softwareecg.awb.metricview.command.ChartCommandProcessor;
import io.softwareecg.awb.metricview.command.ChartDataFetchParameters;
import io.softwareecg.awb.metricview.command.ChartDataService;
/**
 * Controller that manages the whole MetricAnalyser analytics pane that contains the
 * EKG chart as primary component and filter header, chart legend and panel with
 * additional actions.
 *
 * This controller knows as single point of knowledge how to wire all these components
 * correctly and passes the commands of the chart to a dedicated ChartCommandProcessor.
 *
 * @since 30.08.2018
 */
@SuppressWarnings("unused")
public class MetricViewController implements Initializable {

    private static final String AXIS_STYLESHEET = "/io/softwareecg/awb/metricview/ChartPanelStyle.css";

    private static final Logger LOGGER = EkgLogger.get();

    @FXML
    private AnchorPane overlayPane;

    @FXML
    private BorderPane chartContainer;

    @FXML
    private Pane chartActionsPanel;

    @FXML
    private ChartLegendPort chartLegend;

    @FXML
    private QueryEditorPort chartHeaderController;

    @FXML
    private CheckBox cbAlignYAxis;

    @FXML
    private CheckBox cbShowGridLines;

    @FXML
    private Button btnExportToExcel;

    @FXML
    private Button btnExportToPNG;

    @FXML
    private ComboBox<ChartTimeZone> cbTimeZone;


    private final ChartCommandProcessor commandProcessor = Lookup.lookup(ChartCommandProcessor.class);
    private final LayerComposer layerComposer = new LayerComposer(commandProcessor);

    private ZoomableStackedChart zoomableStackedChart;

    private final ChartPanelModel viewModel = new ChartPanelModel();

    /**
     * The time of the newest value the chart has followed up to; 0 before the first
     * {@link SeriesDataWrittenEvent} for the shown project.
     */
    private long newestDataTime;

    /**
     * The time of the newest value reported since the chart last followed; 0 if nothing is pending.
     */
    private long newestPendingDataTime;

    /**
     * How long reports of new data are gathered before the chart loads again.
     */
    private static final javafx.util.Duration DATA_FOLLOW_DELAY = javafx.util.Duration.seconds(2);

    private final PauseTransition dataFollowPause = new PauseTransition(DATA_FOLLOW_DELAY);

    /**
     * Whether the base chart shows the result of a query. It does not after it was pushed to the
     * background: it is empty then until something is loaded into it.
     */
    private boolean baseShowsQuery;

    /** The queries the charts in the background came from, by the id of the chart. */
    private final InvestigationStore investigationStore = new InvestigationStore();

    /**
     * Loads the data of the base chart; kept to know whether a load is still running.
     */
    private ChartDataService chartDataService;

    /**
     * Returns the effective filter parameters for the current chart view.
     * If data has been loaded (or zoomed), returns the active query parameters from the view model
     * which include zoom-adjusted start/end bounds. Otherwise falls back to the header UI selections.
     */
    public QuerySpec getCurrentFilterParams() {
        QuerySpec active = viewModel.getBaseChartFilterParams();
        if (active != null) {
            return active;
        }
        return chartHeaderController.getDefinedFilterParameters();
    }

    /**
     * Returns the current live compute parameters as selected in the chart header UI.
     */
    public ComputationSpec getCurrentComputeParams() {
        return chartHeaderController.getDefinedComputedParameters();
    }

    /**
     * @return the time zone the chart shows its times in, chosen at its lower left corner
     */
    public java.time.ZoneId getTimeZone() {
        ChartTimeZone chosen = cbTimeZone.getValue();
        return (chosen != null ? chosen : ChartTimeZone.getDefaultTimeZone()).getTimeZone();
    }

    /**
     * @return what the repository of this view holds; {@code null} if there is none or it cannot be reached
     */
    public MetricDataAccessService getMetricDataAccessService() {
        if (viewModel.getEkgRepository() == null) return null;
        return viewModel.getEkgRepository().getBoundedService(MetricDataAccessService.class);
    }

    /**
     * @return the queries of the repository of this view; {@code null} if there is none or it cannot be reached
     */
    public MetricQueryService getMetricQueryService() {
        if (viewModel.getEkgRepository() == null) return null;
        return viewModel.getEkgRepository().getBoundedService(MetricQueryService.class);
    }

    /**
     */
    private final EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);

    //================================================================================================================
    //  Initializable interface implementation
    //================================================================================================================

    /* (non-Javadoc)
     * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // set all members to initial values
        initializeViewState();

        // init pseudo classes and other view styles that
        // will setup programmatically
        initializeViewStyles();

        // init default behaviour (validation, filter, ...0) and binding
        // before calling methods overwritten by deriving classes
        initializeViewBehavior();

        // update view from model properties and link the binding if exists
        initializeModelToView();

        // init event handlers
        initializeHandler();
    }


    //=================================================================================================================
    // controller API
    //=================================================================================================================

    /**
     * Handle the delegation of a {@link OpenInvestigationCommand}.
     *
     * It updates the search fields of the current chart panel with the data from the event
     * and load the according metric data to the chart panel.
     *
     * @param event the event with filters, Repository to use and more info need to setup the analytics panel
     */
    public void updateByContextEvent(OpenInvestigationCommand event) {

        if (event.getOpeningMode() == OpeningMode.CLEAR_VIEW) {
            commandProcessor.executeCommand(ChartCommand.CLEAR_ALL);
        } else if (!zoomableStackedChart.getBaseChart().isEmpty()) {
            // keep what is shown, behind the new chart - unless there is nothing to keep
            pushBaseToBackground(null);
        }

        chartHeaderController.setRepository(event.getRepository());
        chartHeaderController.setQueryContext(event.getFilterParams(), event.getOpeningMode());

        viewModel.setEkgRepository(event.getRepository());
        viewModel.setBaseChartFilterParams(event.getFilterParams());

        reloadGraphs(true);
    }

    /**
     * New values of a project were written, as happens continuously while its data is collected.
     * A chart that shows this project follows them; how, is decided by {@link ChartFollow}.
     */
    @EventSubscriber(eventClass = SeriesDataWrittenEvent.class)
    public void seriesDataWritten(SeriesDataWrittenEvent event) {
        Platform.runLater(() -> {
            if (!concernsShownProject(event)) {
                return;
            }
            // Several sources and the chunks of a long first fill report in quick succession.
            // They are gathered: the chart loads once for all of them, not once for each.
            newestPendingDataTime = Math.max(newestPendingDataTime, event.getNewestTimestamp());
            if (dataFollowPause.getStatus() != Animation.Status.RUNNING) {
                dataFollowPause.playFromStart();
            }
        });
    }

    /**
     * The view was closed and will not be shown again: ends what works in the background for it.
     */
    public void dispose() {
        dataFollowPause.stop();
    }

    /**
     * The project this view shows. A view belongs to the project it was opened for: requests
     * for another project go to a view of their own and never change this one.
     *
     * @return what identifies the project - repository and name ({@link #projectKey}) -, or
     *         {@code null} while the view shows nothing
     */
    public String getBoundProjectKey() {
        QuerySpec shown = viewModel.getBaseChartFilterParams();
        return shown == null ? null : projectKey(viewModel.getEkgRepository(), shown.getProjectName());
    }

    /**
     * @return what identifies a project across repositories, {@code null} if it has no name
     */
    public static String projectKey(Repository repository, String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return null;
        }
        return (repository == null ? "" : repository.getId()) + "/" + projectName;
    }

    private boolean concernsShownProject(SeriesDataWrittenEvent event) {
        QuerySpec shown = viewModel.getBaseChartFilterParams();
        Repository repository = viewModel.getEkgRepository();
        return shown != null && repository != null
                && Objects.equals(repository.getId(), event.getRepositoryId())
                && Objects.equals(shown.getProjectName(), event.getProjectName());
    }

    private void followPendingData() {
        if (newestPendingDataTime <= 0) {
            return;
        }
        if (chartDataService != null && chartDataService.isRunning()) {
            // the previous load is still on its way; loading again would only cancel it
            dataFollowPause.playFromStart();
            return;
        }
        long newest = newestPendingDataTime;
        newestPendingDataTime = 0;
        followNewData(newest);
    }

    private void followNewData(long newestTimestamp) {
        if (viewModel.getBaseChartFilterParams() == null || viewModel.getEkgRepository() == null) {
            return;
        }

        long previousNewest = newestDataTime;
        newestDataTime = Math.max(newestDataTime, newestTimestamp);

        ValueAxis<?> xAxis = zoomableStackedChart.getBaseChart().toXYChart().getXAxis() instanceof ValueAxis<?> axis
                ? axis : null;
        boolean wholeRangeShown = xAxis == null || xAxis.isAutoRanging() || zoomableStackedChart.getBaseChart().isEmpty();

        ChartFollow follow = ChartFollow.decide(wholeRangeShown, xAxis == null ? 0 : xAxis.getUpperBound(),
                previousNewest, newestDataTime);
        // The update repeats the query the chart shows - not what the filters above it say at this
        // moment. They may be half way through a change, and whatever they are set to only
        // counts once the user asks for the chart to be loaded with it.
        // What was pushed to the background follows as well, each chart with the query it came from.
        // The base chart only does if it shows a query: after it was pushed away it is empty, and
        // loading its last query again would bring back what the user just moved out of the way.
        switch (follow.action()) {
            case RELOAD_WHOLE_RANGE -> {
                QuerySpec header = chartHeaderController.getDefinedFilterParameters();
                if (baseShowsQuery) {
                    QuerySpec shown = shownQuery();
                    // the time range is the one set in the header: a zoom that was undone left its bounds in the query
                    shown.setStart(header.getStart());
                    shown.setEnd(header.getEnd());
                    reloadGraphs(shown, true);
                }
                followBackgroundCharts(header.getStart(), header.getEnd());
            }
            case MOVE_WINDOW -> {
                xAxis.setLowerBound(xAxis.getLowerBound() + follow.shift());
                xAxis.setUpperBound(xAxis.getUpperBound() + follow.shift());
                double width = xAxis.getUpperBound() - xAxis.getLowerBound();
                long start = (long) (xAxis.getLowerBound() - width / 8d);

                if (baseShowsQuery) {
                    QuerySpec movedOn = shownQuery();
                    movedOn.setStart(start);
                    movedOn.setEndAsDouble(xAxis.getUpperBound() + width / 8d);
                    reloadGraphs(movedOn, false);
                }
                followBackgroundCharts(start, -1);
            }
            case NONE -> {
                // zoomed into the past: nothing changes there
            }
        }
    }

    /**
     * @return a copy of the query whose result the chart shows
     */
    private QuerySpec shownQuery() {
        return copyOf(viewModel.getBaseChartFilterParams());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static QuerySpec copyOf(QuerySpec query) {
        return (QuerySpec) new QuerySpec.Builder(query).build();
    }

    /**
     * Pushes what the base chart shows to the background and remembers the query it came from,
     * so that the chart in the background can follow the data like the base chart does.
     *
     * @param chartId the id the chart in the background gets, or {@code null} for a new one
     */
    private void pushBaseToBackground(String chartId) {
        String id = StringUtils.isBlank(chartId) ? UUID.randomUUID().toString() : chartId;
        if (baseShowsQuery && viewModel.getBaseChartFilterParams() != null) {
            investigationStore.put(id, shownQuery(), viewModel.getBaseChartComputeParams());
        }
        baseShowsQuery = false;
        layerComposer.pushBaseToBackground(id);
    }

    /**
     * Loads the charts in the background again, each with the query it was pushed away with.
     * Their values are replaced in place, so type, colours and hidden series stay as they are.
     *
     * @param start the start of the time range, or -1 for the whole of it
     * @param end   the end of the time range, or -1 for the whole of it
     */
    /**
     * Brings a chart of the background to the front. The chart in front goes to the
     * background in exchange, and the filter header shows the query of the chart that is in
     * front now - so that it can be changed there, which a chart in the background cannot.
     *
     * @param chartId the chart of the background; one this view has the query of
     */
    private void bringToFront(String chartId) {
        InvestigationStore.Entry query = investigationStore.remove(chartId);
        if (query == null) {
            return;
        }
        String idOfTheChartPushedBack = UUID.randomUUID().toString();
        if (!zoomableStackedChart.getBaseChart().isEmpty() && baseShowsQuery
                && viewModel.getBaseChartFilterParams() != null) {
            investigationStore.put(idOfTheChartPushedBack,
                    shownQuery(), viewModel.getBaseChartComputeParams());
        }
        layerComposer.exchangeWithBackground(chartId, idOfTheChartPushedBack);

        // The header takes over the query without loading it: the chart is there already.
        // Not the time, though: what the query has there is the interval that was zoomed
        // into when the chart went to the background, not what "From" and "To" said.
        QuerySpec forTheHeader = copyOf(query.filter());
        QuerySpec header = chartHeaderController.getDefinedFilterParameters();
        forTheHeader.setStart(header.getStart());
        forTheHeader.setEnd(header.getEnd());

        viewModel.suppressEventHandling(true);
        viewModel.setBaseChartFilterParams(copyOf(query.filter()));
        viewModel.setBaseChartComputeParams(query.compute());
        chartHeaderController.setQueryContext(forTheHeader, OpeningMode.UPDATE);
        chartHeaderController.setComputeParameters(query.compute());
        viewModel.suppressEventHandling(false);
        baseShowsQuery = true;
    }

    private void followBackgroundCharts(long start, long end) {
        List<ColoredChart> charts = new ArrayList<>(zoomableStackedChart.getBackgroundCharts());
        investigationStore.retainOnly(charts.stream().map(ColoredChart::getId).collect(Collectors.toSet()));
        Repository repository = viewModel.getEkgRepository();

        for (ColoredChart chart : charts) {
            InvestigationStore.Entry query = investigationStore.get(chart.getId());
            if (query == null || chart.isComputed()) {
                continue;   // calculated from other charts, or not the result of a query of this view
            }
            QuerySpec filter = copyOf(query.filter());
            filter.setStart(start);
            filter.setEnd(end);

            Thread loader = new Thread(() -> {
                try {
                    ComputedTimeSeriesResponse response = repository.getBoundedService(MetricQueryService.class)
                            .getComputedTimeSeries(filter, query.compute(), FillChartDataTask.MAX_ALLOWED_SERIES_EKG_REPO);
                    if (response.isRequestAborted()) {
                        return;
                    }
                    List<XYChart.Series<Long, Double>> series = FillChartDataTask.toChartSeries(response);
                    Platform.runLater(() -> {
                        if (zoomableStackedChart.getBackgroundCharts().contains(chart)) {
                            chart.replaceSeries(series, false);
                        }
                    });
                } catch (RuntimeException e) {
                    LOGGER.warn("Could not update a chart in the background: {}", e.getMessage());
                }
            }, "follow-background-chart");
            loader.setDaemon(true);
            loader.start();
        }
    }

    /**
     * Handle the delegation of a {@link BookmarkEvent} by
     * using it's data to update the chart view.
     *
     * @param event the bookmark event to proceed
     */
    public void updateByBookmarkEvent(BookmarkEvent event) {

        zoomableStackedChart.getController().clearAll();
        chartHeaderController.setRepository(event.getSourceRepository());
        viewModel.setEkgRepository(event.getSourceRepository());

        // a global bookmark is a template: it opens in the project this view shows
        QuerySpec current = getCurrentFilterParams();
        String projectInUse = current == null || current.getProject() == null ? null : current.getProjectName();
        BookmarkContent content = Lookup.lookup(MetricsBookmarkService.class).contentOf(event.getBookmark(), projectInUse);

        chartHeaderController.setQueryContext(content.query(), event.getOpeningMode());
        viewModel.setBaseChartFilterParams(content.query());
        reloadGraphs(true);

        // Restore chart type from bookmark via the command mechanism.
        // This executes on the FX thread before the queued data service restart,
        // so the chart type is set before data loads into the chart.
        if (StringUtils.isNotBlank(content.chartType())) {
            ChartType chartType = ChartType.valueOf(content.chartType());
            if (chartType != ChartType.LINE) {
                commandProcessor.executeObjectArgCommand(
                        ChartCommand.CHANGE_CHART_TYPE,
                        zoomableStackedChart.getBaseChart().getId(),
                        chartType
                );
            }
        }
    }

    //=================================================================================================================
    // controller default initializer
    //=================================================================================================================

    private void initializeViewState() {

        // init axis of default chart
        DateAxis xAxis = new DateAxis();

        MetricAxis yAxis = new MetricAxis();

        yAxis.setLabel(chartHeaderController.getDefinedFilterParameters().getRawQuery());

        // wire this controller and the one of the filter header panel using an own
        // Bind query-editor actions through its component port.
        chartHeaderController.initButtonHandler(new HeaderButtonActions());

        // create the default line chart and add it to the view container
        ObservableList<XYChart.Series<Long, Double>> emptySeries = FXCollections.observableArrayList();
        ColoredChart baseChart = new ColoredChart(new LineChart<>(xAxis, yAxis, emptySeries));
        baseChart.setId("baseChart");
        chartLegend.setBaseChart(baseChart);
        chartLegend.setActionHandler(new ChartLegendActions());
        zoomableStackedChart = new ZoomableStackedChart(baseChart, chartLegend);
        chartContainer.setCenter(zoomableStackedChart);

        overlayPane.getChildren().add(zoomableStackedChart.getMouseOverlays());

        cbTimeZone.setItems(FXCollections.observableArrayList(ChartTimeZone.getChartTimeZones()));
        cbTimeZone.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    // the axis of the chart that is shown now: a change of the chart type
                    // replaces the base chart and its axis with it
                    if (zoomableStackedChart.getBaseChart().getXAxis() instanceof DateAxis shown) {
                        shown.setTimeZone(newValue.getTimeZone());
                    }
                }
        );
        cbTimeZone.setValue(ChartTimeZone.getDefaultTimeZone());
    }



    private void initializeHandler() {
        commandProcessor.setStackedChart(zoomableStackedChart);
        dataFollowPause.setOnFinished(event -> followPendingData());
        //commandProcessor.setStackedChartLegend(chartLegend);
        chartDataService = new ChartDataService() {

            /**
             * a boolean flag that indicates if the chart axis should reset on
             */
            private final boolean resetChartAxis = true;


            @Override
            protected Task createTask() {
                return new FillChartDataTask(
                        getFilterParams(),
                        getComputeParams(),
                        doResetChartAxis(),
                        viewModel.getEkgRepository(),
                        zoomableStackedChart
                );
            }
        };
        commandProcessor.setChartDataService(chartDataService);

        // bind actions
        chartHeaderController.initButtonHandler(new HeaderButtonActions());
        btnExportToExcel.setOnAction(event -> {
            exportChartToExcelSheet();
            chartActionsPanel.requestFocus();
        });

        btnExportToPNG.setOnAction(event -> {
            exportChartToPngFile();
            chartActionsPanel.requestFocus();
        });

        zoomableStackedChart.setOnZoomChanged(this::zoomChangedHandler);

        chartAsDialog(zoomableStackedChart);
    }

    private void initializeModelToView() {

    }


    private void initializeViewBehavior() {
        cbShowGridLines.selectedProperty().addListener((observable, old, isSelected) -> {
            zoomableStackedChart.getBaseChart().toXYChart().setHorizontalGridLinesVisible(isSelected);
            zoomableStackedChart.getBaseChart().toXYChart().setVerticalGridLinesVisible(isSelected);
            chartActionsPanel.requestFocus();
        });

        AtomicBoolean blockAlignHandling = new AtomicBoolean(false);

        zoomableStackedChart.getController().yAxisAlignmentActiveProperty().addListener((observable, oldValue, newValue) -> {
            blockAlignHandling.set(true);
            cbAlignYAxis.setSelected(newValue);
            chartActionsPanel.requestFocus();
            blockAlignHandling.set(false);
        });

        cbAlignYAxis.selectedProperty().addListener((observable, old, isSelected) -> {

            if (blockAlignHandling.get()) {
                return;
            }

            if (isSelected) {
                commandProcessor.executeCommand(ChartCommand.FORCE_ALIGN_Y_AXIS);
            } else {
                commandProcessor.executeCommand(ChartCommand.FREE_ALIGN_Y_AXIS);
            }
            // zoomableStackedChart.getController().alignYAxis(isSelected);
            chartActionsPanel.requestFocus();
        });
    }

    private void initializeViewStyles() {
        cbTimeZone.setSkin(new TimeZoneBoxSkin(cbTimeZone));
    }

    //=================================================================================================================
    // private helpers to manage the UI components and it's states
    //=================================================================================================================

    /**
     * An handler that handles the case where the user zoom in into the chart or vice versa.
     * It will update the model with the start/end date bounds the derived from the zoom rectangle
     * an reloads the base chart series on that base.
     *
     * @param zoomChangedEvent an Zoomable.ZoomEvent instance fired than the user zooms into the chart
     */
    private void zoomChangedHandler(Zoomable.ZoomEvent zoomChangedEvent) {

        // nothing to load if base chart is currently empty
        if (zoomableStackedChart.getBaseChart().isEmpty()) {
            return;
        }

        long diff = (long) zoomChangedEvent.getXAxisUpperBound() - (long) zoomChangedEvent.getXAxisLowerBound();

        // build filter params from the header UI and override start/end with zoom bounds
        QuerySpec zoomFilterParams = chartHeaderController.getDefinedFilterParameters();
        zoomFilterParams.setStart(zoomChangedEvent.getXAxisLowerBound() - diff / 8d);
        zoomFilterParams.setEndAsDouble(zoomChangedEvent.getXAxisUpperBound() + diff / 8d);

        reloadGraphs(zoomFilterParams, !zoomChangedEvent.isZoomIn());
        zoomChangedEvent.consume();
    }

    /**
     * Reloads the base chart by fetching the latest time series data from repository using the current
     * defined filter and post-compute parameter set.
     *
     * Existing chart data will be overwritten.
     *
     * @param resetChartAxis boolean flag that control if the chart axis and it's bounds should be reset or not
     */
    private void reloadGraphs(boolean resetChartAxis) {
        reloadGraphs(chartHeaderController.getDefinedFilterParameters(), resetChartAxis);
    }

    /**
     * Reloads the base chart using the given filter parameters instead of reading them from the header UI.
     * This is used by the zoom handler to pass filter params with the zoomed start/end bounds.
     *
     * @param filterParams the filter parameters to use for the query
     * @param resetChartAxis boolean flag that control if the chart axis and it's bounds should be reset or not
     */
    private void reloadGraphs(QuerySpec filterParams, boolean resetChartAxis) {

        baseShowsQuery = true;
        viewModel.setBaseChartFilterParams(filterParams);
        viewModel.setBaseChartComputeParams(chartHeaderController.getDefinedComputedParameters());

        commandProcessor.executeObjectArgCommand(
                ChartCommand.LOAD_BASE_CHART_DATA,
                zoomableStackedChart.getBaseChart().getId(),
                new ChartDataFetchParameters(
                        viewModel.getBaseChartFilterParams(),
                        viewModel.getBaseChartComputeParams(),
                        resetChartAxis
                ),
                false
        );
    }

    /**
     * Wraps the SplitPane with Chart and legend in a dialog that will displayed in
     * a separate modal than the user double clicks on the SplitPane.
     *
      * @param stackedChart a ZoomableStackedChart instance that will be wrapped and displayed by a JavaFX dialog
     */
    private void chartAsDialog(ZoomableStackedChart stackedChart) {

        final Dialog chartPaneDialog = new Dialog();

        chartPaneDialog.initModality(Modality.NONE);
        chartPaneDialog.setTitle("Metric");
        chartPaneDialog.getDialogPane().setStyle(
                "-fx-background-color: #FDFDFC; " +
                "-fx-border-color: #0096c9; " +
                "-fx-border-width: 2 0 2 0;");
        chartPaneDialog.setResizable(true);
        chartPaneDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = chartPaneDialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);
        chartPaneDialog.getDialogPane().setPadding(new Insets(0, 0, -20, 0));

        stackedChart.setOnMouseClicked(mouseEvent -> {

            // if main button double click
            if (!(mouseEvent.getButton().equals(MouseButton.PRIMARY) && mouseEvent.getClickCount() == 2)) {
                return;
            }

            final Bounds boundsInScreen = chartContainer.getBoundsInLocal();

            // reallocate the chart pane with overlay to dialog and fill the old location with an empty pane
            chartContainer.setCenter(new Pane());
            chartContainer.layout();
            chartPaneDialog.getDialogPane().setContent(zoomableStackedChart);

            // layout the dialog with borrowed chart in the JavaFX thread
            Platform.runLater(() -> {
                chartPaneDialog.setY(boundsInScreen.getMinY());
                chartPaneDialog.setX(boundsInScreen.getMinX());
                chartPaneDialog.setHeight(boundsInScreen.getHeight() * 1.5);
                chartPaneDialog.setWidth(boundsInScreen.getWidth());
                chartPaneDialog.getDialogPane().layout();
            });

            // than dialog will closed by the user the overlay will set back to the primary chart pane
            chartPaneDialog.setOnHidden(windowEvent -> {
                chartPaneDialog.getDialogPane().setContent(null);
                chartContainer.setCenter(zoomableStackedChart);

                Platform.runLater(() -> chartContainer.layout());
            });

            chartPaneDialog.show();
        });
    }

    /**
     * Creates a screenshot of the StackableChart with it's current displayed series
     * and write it as image file to the file system.
     */
    private void exportChartToPngFile() {
        ScreenshotExportDialog screenshotExportDialog = new ScreenshotExportDialog(zoomableStackedChart);
        screenshotExportDialog.showAndWait();
    }

    /**
     * Exports the current graph data from (base) chart to an excel file the user can
     * specify via FileChooser.
     * The export will executed with blocking behavior, the UI didn't respond during that.
     */
    private void exportChartToExcelSheet() {

        FileChooser chooser = new FileChooser();

        chooser.setTitle("Choose the file for the Excel export");

        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel files", "*.xlsx"),
                new FileChooser.ExtensionFilter("All files", "*")
        );

        File chosenFile = chooser.showSaveDialog(null);

        if (chosenFile == null) {
            return;
        }

        List<ExcelExportSeriesData> seriesData = zoomableStackedChart
                .getBaseChart()
                .getAllSeries()
                .stream()
                .map(series -> new ExcelExportSeriesData(series.getName(), series.getData()))
                .collect(Collectors.toList());

        seriesData.addAll(
                zoomableStackedChart
                .getBackgroundCharts()
                .stream()
                .flatMap(chart -> chart.getAllSeries().stream())
                .map(series -> new ExcelExportSeriesData(series.getName(), series.getData()))
                .collect(Collectors.toList())
        );

        ExcelExporter exporter = new ExcelExporter(chosenFile);
        exporter.createExcelSheetFromSeriesData(seriesData);
    }

    //=================================================================================================================
    // chart legend / chart actions function handler
    //=================================================================================================================

    /**
     * Handler for the chart filter header panel
     */
    private class ChartLegendActions extends LegendActionCallbacks {

        @Override
        public void combineBgChartsRelativeToBase(boolean interpolate) {
            commandProcessor.executeObjectArgCommand(ChartCommand.RELATIVE_COMBINE_TO_BASE, null, interpolate);
        }

        @Override
        public void combineBgAbsolute() {
            commandProcessor.executeCommand(ChartCommand.ABSOLUTE_COMBINE_TO_BASE);
        }

        @Override
        public void pushToBackground(String newChartId) {
            pushBaseToBackground(newChartId);
        }

        @Override
        public void clearAll() {
            commandProcessor.executeCommand(ChartCommand.CLEAR_ALL);
        }

        @Override
        public void deleteAllBgCharts() {
            commandProcessor.executeCommand(ChartCommand.DELETE_ALL_BG_CHARTS);
        }

        @Override
        public void deleteBgChart(String chartId) {
            commandProcessor.executeCommand(ChartCommand.DELETE_BG_CHART, chartId);
        }

        @Override
        public void bringToFront(String chartId) {
            MetricViewController.this.bringToFront(chartId);
        }

        @Override
        public boolean canBringToFront(String chartId) {
            return investigationStore.contains(chartId);
        }

        @Override
        public void changeChartType(String chartId, ChartType newChartType) {
            commandProcessor.executeObjectArgCommand(ChartCommand.CHANGE_CHART_TYPE, chartId, newChartType);
        }

        @Override
        public void setChartVisible(String chartId, boolean isVisible) {
            commandProcessor.executeObjectArgCommand(ChartCommand.SET_CHART_VISIBLE, chartId, isVisible);
        }

        @Override
        public void changeChartColor(String chartId, Color newColor) {
            commandProcessor.executeObjectArgCommand(ChartCommand.CHANGE_CHART_COLOR, chartId, newColor);
        }

        @Override
        public void changeSeriesColor(String chartId, String seriesName, Color newColor) {
            commandProcessor.executeObjectArgCommand(ChartCommand.CHANGE_SERIES_COLOR, chartId, seriesName, newColor);
        }
    }

    //=================================================================================================================
    // Chart header panel actions (handler callbacks)
    //=================================================================================================================

    /**
     * Handler for the chart filter header panel
     */
    private class HeaderButtonActions extends QueryEditorPort.Actions {

        /**
         * Creates the Metrics for the Graph.
         */
        @Override
        public void updateMetricsGraph() {
            reloadGraphs(true);
        }

        /**
         * Creates a new bookmark. A Dialog is shown, in this dialog the user can choose the bookmark's name.
         */
        @Override
        public void createBookmark() {

            // a bookmark is opened in the repository it was made in
            if (viewModel.getEkgRepository() == null) {
                return;
            }

            boolean created = Lookup.lookup(BookmarkDialogs.class).createBookmark(
                    chartHeaderController.getOwnerWindow(),
                    getCurrentFilterParams(),
                    zoomableStackedChart.getBaseChart().getChartType().name());
            if (created) {
                eventBus.publish(new ExplorerUpdateEvent(this, viewModel.getEkgRepository()));
            }
        }

        @Override
        public void changeSmoothingGranularity(SeriesSmoothingGranularity smoothingGranularity) {
            viewModel.getBaseChartComputeParams().setSeriesSmoothingGranularity(smoothingGranularity);
            if (!viewModel.isSuppressEventHandling()) {
                reloadGraphs(true);
            }
        }

        @Override
        public void changeSmoothingType(SeriesSmoothingType aggregation) {
            viewModel.getBaseChartComputeParams().setSeriesSmoothingType(aggregation);
            if (!viewModel.isSuppressEventHandling()) {
                reloadGraphs(true);
            }
        }

        @Override
        public void changeSeriesCombineMode(SeriesCombineMode combineMode) {
            viewModel.getBaseChartComputeParams().setSeriesCombineMode(combineMode);
            if (!viewModel.isSuppressEventHandling()) {
                reloadGraphs(true);
            }
        }

        @Override
        public void changeThreshold(int threshold) {
            viewModel.getBaseChartComputeParams().setThreshold(threshold);

            if (!viewModel.isSuppressEventHandling()) {
                reloadGraphs(true);
            }
        }

        @Override
        public void changeMaxDisplaySeries(int maxDisplaySeries) {
            viewModel.getBaseChartComputeParams().setMaxDisplaySeries(maxDisplaySeries);

            if (!viewModel.isSuppressEventHandling()) {
                reloadGraphs(true);
            }
        }
    }

    private static class TimeZoneBoxSkin extends ComboBoxListViewSkin<ChartTimeZone> {

        /**
         * Creates a new ComboBoxListViewSkin instance, installing the necessary child
         * nodes into the Control {@link Control::getChildren() children} list, as
         * well as the necessary input mappings for handling key, mouse, etc events.
         *
         * @param timeSeriesCombobox The control that this skin should be installed onto.
         */
        public TimeZoneBoxSkin(ComboBox<ChartTimeZone> timeSeriesCombobox) {
            super(timeSeriesCombobox);
            timeSeriesCombobox.getStyleClass().add("timezoneComboBox");
            timeSeriesCombobox.getStylesheets().add(MetricViewController.class
                    .getResource("ChartPanelStyle.css").toExternalForm());


            timeSeriesCombobox.setCellFactory(value -> new TimeZoneListCell());
        }

        /**
         * This method should return a Node that will be positioned within the
         * ComboBox 'button' area.
         * @return the node that will be positioned within the ComboBox 'button' area
         */
        public Node getDisplayNode() {
            //noinspection unchecked
            return new ButtonCell((ChartTimeZone)((ComboBox) getNode()).getSelectionModel().getSelectedItem());
        }


        private static class ButtonCell extends Cell<ChartTimeZone> {

            public ButtonCell(ChartTimeZone initialValue) {
                super.getStyleClass().add("timezoneComboBoxBtn");
                this.updateItem(initialValue, false);
            }

            /** {@inheritDoc} */
            @Override
            protected void updateItem(ChartTimeZone selectedValue, boolean empty) {
                super.updateItem(selectedValue, empty);
                setText("");
                setGraphic(null);
            }

            /** {@inheritDoc} */
            @Override protected Skin<?> createDefaultSkin() {
                return new CellSkinBase<>(this);
            }
        }

        private static class TimeZoneListCell extends ListCell<ChartTimeZone> {
            /** {@inheritDoc} */
            @Override
            protected void updateItem(ChartTimeZone timeZone, boolean empty) {
                super.updateItem(timeZone, empty);
                if (timeZone != null) {
                    setText(" " + timeZone.getAlias());
                }
            }
        }
    }
}
