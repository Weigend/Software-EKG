//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.metricview.queryeditor;

import afester.javafx.svg.SvgLoader;
import io.softwareecg.awb.uitoolkit.bindings.Bindings;
import io.softwareecg.awb.uitoolkit.components.FilterableComboBox;
import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import io.softwareecg.awb.query.model.SeriesCombineMode;
import io.softwareecg.awb.query.model.SeriesSmoothingGranularity;
import io.softwareecg.awb.query.model.SeriesSmoothingType;
import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.timeseries.dimension.Metric;
import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.ProcessGroup;
import io.softwareecg.awb.timeseries.dimension.logical.Container;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.timeseries.dimension.logical.Service;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.query.model.ComputationSpec;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.metricview.queryeditor.converter.InstantToUtcDateConverter;
import io.softwareecg.awb.metricview.queryeditor.converter.NamedEnumConverter;
import io.softwareecg.awb.runtime.NamedEnum;
import io.softwareecg.awb.metricview.queryeditor.model.ChartHeaderBusinessProcess;
import io.softwareecg.awb.projectcontext.api.ProjectViewFlavorChangedEvent;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.projectcontext.model.ProjectFlavor;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static javafx.collections.FXCollections.observableList;

/**
 * Controller class that manages the filter header panel that is on top of the
 * time series chart and provides multiple facet filters and query/compute settings.
 *
 * @since 20.01.2017
 */
public class ChartHeaderController implements Initializable, QueryEditorPort {

    private static final int NUMBER_DUMMY_LOADING_ITEMS = 4;

    private static final String LOADING_TEXT = "Loading...";

    /**
     * view model for the chart header
     */
    private final ChartHeaderBusinessProcess viewModel = new ChartHeaderBusinessProcess();

    /**
     * Default callback for handling actions
     */
    private QueryEditorPort.Actions actions = new QueryEditorPort.Actions();

    /* ------------------ generic filter (I) ------------------ */

    @FXML
    private GridPane measurementFilterPane;

    @FXML
    private Label lbMeasurement;

    @FXML
    private FilterableComboBox<Measurement> cbxMeasurement;

    /* ------------------ classic filter ---------------------- */

    @FXML
    private Label lbHostGroup;

    @FXML
    private FilterableComboBox<HostGroup> cbxHostGroup;

    @FXML
    private Label lbHost;

    @FXML
    private FilterableComboBox<Host> cbxHost;

    /* ------------------- cloud filter ----------------------- */

    @FXML
    private Label lbNameSpace;

    @FXML
    private FilterableComboBox<Namespace> cbxNamespace;

    @FXML
    private GridPane serviceFilterPane;

    @FXML
    private Label lbService;

    @FXML
    private FilterableComboBox<Service> cbxService;

    @FXML
    private Label lbPod;

    @FXML
    private FilterableComboBox<Pod> cbxPod;

    @FXML
    private Label lbContainer;

    @FXML
    private FilterableComboBox<Container> cbxContainer;

    /* ------------------ generic filter (II) ----------------- */

    @FXML
    private GridPane processGroupFilterPane;
    @FXML
    private Label lbProcessGroup;
    @FXML
    private FilterableComboBox<ProcessGroup> cbxProcessGroup;

    @FXML
    private Label lbProcess;

    @FXML
    private FilterableComboBox<Process> cbxProcess;

    @FXML
    private Label lbMetricGroup;

    @FXML
    private FilterableComboBox<MetricGroup> cbxMetricGroup;

    /* ------------------ generic filter (III) ----------------- */

    @FXML
    private Label lbMetric;

    @FXML
    private FilterableComboBox<Metric> cbxMetric;

    @FXML
    private Label labelExclude;

    @FXML
    private TextField txtExclude;

    @FXML
    private Label labelExpertQuery;

    @FXML
    private TextArea taExpertQuery;

    @FXML
    private DatePicker startPicker;

    @FXML
    private DatePicker stopPicker;

    /* ------------------ other panel components ----------------- */

    @FXML
    private ComboBox<SeriesSmoothingType> cbxSmoothingType;

    @FXML
    private ComboBox<SeriesSmoothingGranularity> cbxSmoothingGranularity;

    @FXML
    private ComboBox<Integer> cbxThreshold;

    @FXML
    private ComboBox<Integer> cbxMaxDisplaySeries;

    @FXML
    private Label lbEnableAllFilter;

    @FXML
    private CheckBox cbEnableAllFilter;

    @FXML
    private Button btRefreshGraph;

    @FXML
    private Button btBookmark;

    @FXML
    private ComboBox<SeriesCombineMode> cbxCombineMode;

    @FXML
    private VBox seriesFilterGroup;

    /* ------------------ misc vars ----------------- */

    private final HashSet<Object> updatedComboboxes = new HashSet<>();


    /* ------------------------------------ Initializable Interface implementation ---------------------------------- */

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initViewStyles();
        initComboBoxCellFactories();
        configureClassicOrCloudFilterAccessibility();
        configureExtendedFilterAccessibility();
        bindFilterToModel();
        initHandler();
        initDefaultState();
    }

    //------------------------------------------------------------------------------------------------------------------
    // controller api used by other EKG components
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Initialize the Button handler
     * @param actions actions
     */
    public void initButtonHandler(QueryEditorPort.Actions actions) {
        this.actions = actions;
        btBookmark.setOnAction(e -> actions.createBookmark());
        btRefreshGraph.setOnAction(e -> actions.updateMetricsGraph());
    }

    /**
     * Initialize the default states of the model and view
     */
    private void initDefaultState() {
        updateFilterDimensionLabels(null);
        cbxCombineMode.setValue(SeriesCombineMode.CONCAT);
    }

    /**
     * Returns the view viewModel of the ChartHeader component
     *
     * @return the view viewModel
     */
    public QueryDraft getModel() {
        return viewModel;
    }

    /**
     * Returns the query filter parameters defined by the user in the filter panel.
     * The parameters will returned as {@link QuerySpec} instance that contains all defined
     * filters that will send to as query to persistence layer. The filters didn't include
     * post processing settings like threshold or smoothing type.
     *
     * @return all filter packed into a QuerySpec instance
     */
    public QuerySpec getDefinedFilterParameters() {
        return viewModel.asQueryParams();
    }

    /**
     * Returns the (post) compute parameters defined by the user in the filter panel.
     * The parameters will returned as {@link ComputationSpec} instance that contains all defined settings
     * and should use to control the post processing steps on fetched time series data.
     *
     * The settings doesn't include filter parameters send as query to the persistence layer.
     *
     * @return all compute parameters packed into a ComputationSpec instance
     */
    public ComputationSpec getDefinedComputedParameters() {
        return viewModel.asComputeParams();
    }

    /**
     * Returns a reference to the owner window the chart header panel
     * belongs to.
     *
     * @return the owner window
     */
    public Window getOwnerWindow() {
        return btRefreshGraph.getScene().getWindow();
    }

    /**
     * Event listener method that will switch the view flavor (classic filters vs cloud filter) if
     * it is notified by an ProjectViewFlavorChangedEvent event
     *
     * @param event ProjectViewFlavorChangedEvent instance with the new state
     */
    @SuppressWarnings("unused") // used by event bus via reflection
    @EventSubscriber(eventClass = ProjectViewFlavorChangedEvent.class)
    public void changeProjectViewFlavor(ProjectViewFlavorChangedEvent event) {
        // A chart view belongs to one project. Selecting another project sends this event too;
        // it must not rearrange the filters of the views that show something else.
        Project shown = viewModel.getCurrentProject();
        Project changed = event.getRelatedProject();
        if (shown == null || changed == null || !java.util.Objects.equals(shown.getName(), changed.getName())) {
            return;
        }
        if (viewModel.getCurrentProject().getProjectFlavor() == ProjectFlavor.CLASSIC
                && event.getNewViewFlavor() == ProjectViewFlavor.LOGICAL_VIEW) {
            return;

        } else if (viewModel.getCurrentProject().getProjectFlavor() == ProjectFlavor.CLOUD_NATIVE
                && event.getNewViewFlavor() == ProjectViewFlavor.PHYSICAL_VIEW){
            return;
        }
        viewModel.cloudModeProperty().setValue(event.getNewViewFlavor().equals(ProjectViewFlavor.LOGICAL_VIEW));
    }

    /**
     * Sets the expertQuery context.
     *
     * @param queryContext a expertQuery context.
     */
    public void setQueryContext(QuerySpec queryContext, OpeningMode openingMode) {

        if (openingMode == OpeningMode.MERGE_VIEW &&
                    !viewModel.getCurrentProject().equals(queryContext.getProject())) {
            // todo SKR disable button than different repositories are in use
            //btBookmark.setDisable(true);
        }

        viewModel.loadDataFromQueryContext(queryContext);
        updateFilterDimensionLabels(queryContext.getProject());
    }

    /**
     * Sets the expertQuery context, but this method does not(!) change the value of the expertMode and the
     * MultiMetricMode.
     */
    public void setQueryContextWithoutModes(QuerySpec queryContext) {

        updateFilterDimensionLabels(queryContext.getProject());
        boolean oldExpertMode = viewModel.isExpertMode();
        boolean oldMultiMetricMode = viewModel.isMultiMetricMode();

        viewModel.loadDataFromQueryContext(queryContext);

        viewModel.setExpertMode(oldExpertMode);
        viewModel.setMultiMetricMode(oldMultiMetricMode);
    }

    /**
     * Get the current types.
     *
     * @return the current types.
     */
    public Repository getRepository() {
        return viewModel.getRepository();
    }

    /**
     * Set the EKG repository that will used by the controller to fetch the
     * facet data for the filter combo boxes.
     *
     * @param repository the repository the stores the project which is the context for all settings in the panel
     */
    public void setRepository(Repository repository) {
        viewModel.setRepository(repository);
    }

    /**
     * Sets the current multi metric mode
     *
     * @param multiMetricMode the new value of the multi metric mode true = active, false = inactive
     */
    public void setMultiMetricMode(boolean multiMetricMode) {
        viewModel.setMultiMetricMode(multiMetricMode);
    }


    //------------------------------------------------------------------------------------------------------------------
    // internal initialization implementation
    //------------------------------------------------------------------------------------------------------------------

    private void initViewStyles() {
        Group iconImageUpdate = new SvgLoader().loadSvg(getClass().getResourceAsStream("update-icon.svg"));
        iconImageUpdate.setScaleX(0.05);
        iconImageUpdate.setScaleY(0.05);
        btRefreshGraph.setGraphic(new Group(iconImageUpdate));

        Group iconImageBookmark = new SvgLoader().loadSvg(getClass().getResourceAsStream("bookmark-icon.svg"));
        iconImageBookmark.setScaleX(1.2);
        iconImageBookmark.setScaleY(1.2);
        btBookmark.setGraphic(new Group(iconImageBookmark));

        cbxSmoothingType.setConverter(new StringConverter<>() {
            @Override
            public String toString(SeriesSmoothingType mode) {
                return mode.getName();
            }

            @Override
            public SeriesSmoothingType fromString(String alias) {
                return SeriesSmoothingType.NONE;
            }
        });

        cbxSmoothingGranularity.setConverter(new NamedEnumConverter<>());
        cbxSmoothingType.setConverter(new NamedEnumConverter<>());
        cbxCombineMode.setConverter(new NamedEnumConverter<>());

        explain(cbxSmoothingType, "Smoothing");
        explain(cbxCombineMode, "Combine metrics");
    }

    /**
     * Gives a list of modes tooltips that say what a mode does: one at every entry of the open
     * list, and one at the list itself for the mode that is chosen. "Concatenate" and "Value
     * Difference" are names nobody can guess the effect of.
     *
     * @param modes the list
     * @param title what the list is, the text of its label. The label of "Combine metrics" has
     *              no room in a narrow chart panel, so the tooltip names the list as well.
     */
    private static <T extends NamedEnum> void explain(ComboBox<T> modes, String title) {
        modes.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(T mode, boolean empty) {
                super.updateItem(mode, empty);
                boolean none = empty || mode == null;
                setText(none ? null : mode.getName());
                setTooltip(none || mode.getDescription().isBlank() ? null : new Tooltip(mode.getDescription()));
            }
        });
        Tooltip chosen = new Tooltip();
        chosen.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> modes.getValue() == null ? title : title + " - " + modes.getValue().getDescription(),
                modes.valueProperty()));
        modes.setTooltip(chosen);
    }

    private void updateFilterDimensionLabels(Project project) {

        if (project == null) {
            project = new Project(null, null, null, null);
        } else {
            ProjectFlavor projectFlavor = project.getProjectFlavor();
            viewModel.cloudModeProperty().setValue(projectFlavor != null && projectFlavor != ProjectFlavor.CLASSIC);
        }

        lbHostGroup.setText(getDimensionLabel(FilterDimension.HOST_GROUP, project.getDimensionAliasHostGroup()));
        lbHost.setText(getDimensionLabel(FilterDimension.HOST, project.getDimensionAliasHost()));
        lbNameSpace.setText(getDimensionLabel(FilterDimension.NAMESPACE, project.getDimensionAliasNamespace()));
        lbService.setText(getDimensionLabel(FilterDimension.SERVICE, project.getDimensionAliasService()));
        lbPod.setText(getDimensionLabel(FilterDimension.POD, project.getDimensionAliasPod()));
        lbContainer.setText(getDimensionLabel(FilterDimension.CONTAINER, project.getDimensionAliasContainer()));
        lbMeasurement.setText(getDimensionLabel(FilterDimension.MEASUREMENT, project.getDimensionAliasMeasurement()));
        lbProcessGroup.setText(getDimensionLabel(FilterDimension.PROCESS_GROUP, null));
        lbProcess.setText(getDimensionLabel(FilterDimension.PROCESS, project.getDimensionAliasProcess()));
        lbMetricGroup.setText(getDimensionLabel(FilterDimension.METRIC_GROUP, project.getDimensionAliasMetricGroup()));
        lbMetric.setText(getDimensionLabel(FilterDimension.METRIC_NAME, project.getDimensionAliasMetricName()));
    }

    private String getDimensionLabel(FilterDimension dimension, String projectSpecificLabel) {
        if (StringUtils.isBlank(projectSpecificLabel)) {
            projectSpecificLabel = QueryDraft.DEFAULT_FILTER_DIMENSION_LABELS.get(dimension);
        }

        if (!projectSpecificLabel.endsWith(":")) {
            projectSpecificLabel += ":";
        }

        return projectSpecificLabel;
    }

    private void configureExtendedFilterAccessibility() {
        cbEnableAllFilter.disableProperty().bind(viewModel.cloudModeProperty().not().or(viewModel.getExpertModeProperty()));
        lbEnableAllFilter.disableProperty().bind(cbEnableAllFilter.disableProperty());

        labelExpertQuery.visibleProperty().bind(viewModel.getExpertModeProperty());
        taExpertQuery.disableProperty().bind(viewModel.getExpertModeProperty().not());
        taExpertQuery.visibleProperty().bind(viewModel.getExpertModeProperty());

        labelExclude.visibleProperty().bind(viewModel.getExpertModeProperty().not());
        txtExclude.disableProperty().bind(viewModel.getExpertModeProperty());
        txtExclude.visibleProperty().bind(viewModel.getExpertModeProperty().not());

        lbMetric.visibleProperty().bind(viewModel.getExpertModeProperty().not());
        cbxMetric.disableProperty().bind(viewModel.getExpertModeProperty());
        cbxMetric.visibleProperty().bind(viewModel.getExpertModeProperty().not());

        startPicker.disableProperty().bind(viewModel.getExpertModeProperty());
        stopPicker.disableProperty().bind(viewModel.getExpertModeProperty());

//        cbExpertMode.selectedProperty().addListener((observable, oldValue, newValue)
//                -> viewModel.getExpertModeProperty().setValue(newValue));
    }

    private void configureClassicOrCloudFilterAccessibility() {

        BooleanBinding enableExtCloudFilters = viewModel.cloudModeProperty().not()
                .or(viewModel.cloudModeProperty().and(cbEnableAllFilter.selectedProperty()));

        measurementFilterPane.visibleProperty().bind(enableExtCloudFilters);
        measurementFilterPane.managedProperty().bind(measurementFilterPane.visibleProperty());

        // the process group like the measurement: always in a classic project, and in a
        // cloud-native one among the additional filters, where the header is long as it is
        processGroupFilterPane.visibleProperty().bind(enableExtCloudFilters);
        processGroupFilterPane.managedProperty().bind(processGroupFilterPane.visibleProperty());

        serviceFilterPane.visibleProperty().bind(viewModel.cloudModeProperty().and(cbEnableAllFilter.selectedProperty()));
        serviceFilterPane.managedProperty().bind(serviceFilterPane.visibleProperty());


        for (Node child : seriesFilterGroup.getChildren()) {

            initDisableAndEditableBehavior(child);

            // don't consume space in the region if invisible
            child.managedProperty().bind(child.visibleProperty());

            if (child == measurementFilterPane || child == processGroupFilterPane) {
                // bound above. The measurement is marked as a classic filter and was bound a
                // second time here, to classic projects only: the check box for the
                // additional filters never showed it in a cloud-native project.
                continue;
            }
            if (child.getStyleClass().contains("classicFilter")) {
                child.visibleProperty().bind(viewModel.cloudModeProperty().not());

            } else if (child.getStyleClass().contains("cloudFilter") && child != serviceFilterPane) {
                child.visibleProperty().bind(viewModel.cloudModeProperty());
            }
        }
    }

    private void initDisableAndEditableBehavior(Node filterPane) {

        if (!(filterPane instanceof Pane)) {
            return;
        }

        for (Node child : ((Pane)filterPane).getChildren()) {
            child.disableProperty().bind(viewModel.getExpertModeProperty());

            if (child instanceof ComboBox) {
                ((ComboBox)child).setEditable(true);
            }
        }
    }

    /**
     * Bind the series and generic filter controls (ComboBoxes)
     * to the view model. The binding will be bidirectional.
     */
    private void bindFilterToModel() {

        // classic series filter
        Bindings.bindComboBox(cbxHostGroup, viewModel.getHostGroups(), viewModel.currentHostGroupProperty());
        Bindings.bindComboBox(cbxHost, viewModel.getHosts(), viewModel.currentHostProperty());

        // cloud native series filter
        Bindings.bindComboBox(cbxNamespace, viewModel.getNamespaces(), viewModel.currentNamespaceProperty());
        Bindings.bindComboBox(cbxService, viewModel.getServices(), viewModel.currentServiceProperty());
        Bindings.bindComboBox(cbxPod, viewModel.getPods(), viewModel.currentPodProperty());
        Bindings.bindComboBox(cbxContainer, viewModel.getContainers(), viewModel.currentContainerProperty());

        // overall series filter
        Bindings.bindComboBox(cbxMeasurement, viewModel.getMeasurements(), viewModel.currentMeasurementProperty());
        Bindings.bindComboBox(cbxProcessGroup, viewModel.getProcessGroups(), viewModel.currentProcessGroupProperty());
        Bindings.bindComboBox(cbxProcess, viewModel.getProcess(), viewModel.currentProcessProperty());
        Bindings.bindComboBox(cbxMetricGroup, viewModel.getMetricMetricGroups(), viewModel.currentMetricGroupProperty());
        Bindings.bindComboBox(cbxMetric, viewModel.getMetrics(), viewModel.currentMetricProperty());

        // other filters
        txtExclude.textProperty().addListener((observable, oldValue, newValue) -> viewModel.setExcludeMetric(new Metric(txtExclude.getText())));
        viewModel.rawQueryProperty().bindBidirectional(taExpertQuery.textProperty());
        //viewModel.getExpertModeProperty().bindBidirectional(cbExpertMode.selectedProperty());
        Bindings.bindDatePicker(startPicker, viewModel.startDateProperty(), new InstantToUtcDateConverter());
        Bindings.bindDatePicker(stopPicker, viewModel.stopDateProperty(), new InstantToUtcDateConverter());

        // query/compute settings
        Bindings.bindComboBox(cbxSmoothingType, observableList(asList(SeriesSmoothingType.values())), viewModel.currentSmoothingTypeProperty());
        Bindings.bindComboBox(cbxSmoothingGranularity, observableList(asList(SeriesSmoothingGranularity.values())), viewModel.smoothingGranularityProperty());
        Bindings.bindComboBox(cbxThreshold, observableList(viewModel.getThresholds()), viewModel.thresholdProperty());
        Bindings.bindComboBox(cbxMaxDisplaySeries, observableList(viewModel.getMaxDisplaySeriesValues()), viewModel.maxDisplaySeriesProperty());
        Bindings.bindComboBox(cbxCombineMode, observableList(asList(SeriesCombineMode.values())), viewModel.seriesCombineModeProperty());

    }

    /**
     * Set a special kind of cell factory for the threshold combobox that will
     * use to format the value in a better readable way.
     */
    private void initComboBoxCellFactories() {

        Callback<ListView<Integer>, ListCell<Integer>> thresholdRenderer = new Callback<>() {
            @Override
            public ListCell<Integer> call(ListView<Integer> p) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || item < 0) {
                            setText("Off");
                        } else if (item == 0) {
                            setText("Changes Only");
                        } else if (item >= 1000) {
                            setText(item / 1000 + "K Pts/Line");
                        } else {
                            setText(item + " Pts/Line");
                        }
                    }
                };
            }
        };

        cbxThreshold.setButtonCell(thresholdRenderer.call(null));
        cbxThreshold.setCellFactory(thresholdRenderer);

        Callback<ListView<Integer>, ListCell<Integer>> maxSeriesRenderer = new Callback<>() {
            @Override
            public ListCell<Integer> call(ListView<Integer> p) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                        } else {
                            setText(item + " Lines");
                        }
                    }
                };
            }
        };

        cbxMaxDisplaySeries.setButtonCell(maxSeriesRenderer.call(null));
        cbxMaxDisplaySeries.setCellFactory(maxSeriesRenderer);
    }

    /* -------------------------------------------- event implementation -------------------------------------------- */

    /**
     * Initializes the controller.
     */
    private void initHandler() {

        CreateGraphEventHandler createGraphHandler = new CreateGraphEventHandler();
        txtExclude.setOnAction(createGraphHandler);
        
        // Use valueProperty listener instead of setOnAction for DatePicker
        // setOnAction only fires on Enter key or popup selection, not on all value changes
        startPicker.valueProperty().addListener((s, o, n) -> {
            if (!viewModel.isSuppressEventHandling() && n != null) {
                actions.updateMetricsGraph();
            }
        });
        stopPicker.valueProperty().addListener((s, o, n) -> {
            if (!viewModel.isSuppressEventHandling() && n != null) {
                actions.updateMetricsGraph();
            }
        });

        cbxSmoothingGranularity.valueProperty().addListener((s, o, n) -> actions.changeSmoothingGranularity(n));
        cbxSmoothingType.valueProperty().addListener((s, o, n) -> actions.changeSmoothingType(n));
        cbxCombineMode.valueProperty().addListener((s, o, n) -> actions.changeSeriesCombineMode(n));
        cbxThreshold.valueProperty().addListener((s, o, n) -> actions.changeThreshold(n));
        cbxMaxDisplaySeries.valueProperty().addListener((s, o, n) -> actions.changeMaxDisplaySeries(n));

        // ====== init filter actions and default value for each filter box  ======

        // classic filter
        initCbxItemAction(cbxHostGroup,   viewModel::loadHostGroups,   new HostGroup(LOADING_TEXT),   HostGroup::new);
        initCbxItemAction(cbxHost,        viewModel::loadHosts,        new Host(LOADING_TEXT),        Host::new);
        // cloud filter
        initCbxItemAction(cbxNamespace,   viewModel::loadNamespaces,   new Namespace(LOADING_TEXT),   Namespace::new);
        initCbxItemAction(cbxService,     viewModel::loadServices,     new Service(LOADING_TEXT),     Service::new);
        initCbxItemAction(cbxPod,         viewModel::loadPods,         new Pod(LOADING_TEXT),         Pod::new);
        initCbxItemAction(cbxContainer,   viewModel::loadContainers,   new Container(LOADING_TEXT),   Container::new);
        // generic filter
        initCbxItemAction(cbxMeasurement, viewModel::loadMeasurements, new Measurement(LOADING_TEXT), Measurement::new);
        initCbxItemAction(cbxProcessGroup, viewModel::loadProcessGroups, new ProcessGroup(LOADING_TEXT), ProcessGroup::new);
        initCbxItemAction(cbxProcess,     viewModel::loadProcesses,    new Process(LOADING_TEXT),     Process::new);
        initCbxItemAction(cbxMetricGroup, viewModel::loadMetricGroups, new MetricGroup(LOADING_TEXT), MetricGroup::new);
        initCbxItemAction(cbxMetric,      viewModel::loadMetrics,      new Metric(LOADING_TEXT),      Metric::new);
    }


    private <T> void initCbxItemAction(final FilterableComboBox<T> comboBox, Runnable loader,
                                       T firstDummyItem, Supplier<T> dummyItemCreator) {

        // add default value to each combo-box (normally a wildcard)
        List<T> dummyItems = initDummyItems(firstDummyItem, dummyItemCreator);
        comboBox.getItems().setAll(dummyItems);
        comboBox.setVisibleRowCount(10);

        comboBox.focusedProperty().addListener(e -> {
            // if the box has focus and is marked as "not updated" or still contains the initial dummy item
            // then load the data from server
            if (comboBox.isFocused() && (!updatedComboboxes.contains(comboBox) ||
                    (!comboBox.getItems().isEmpty() && firstDummyItem.equals(comboBox.getItems().get(0))))) {
                loader.run();
                updatedComboboxes.add(comboBox);
            }
        });

        comboBox.setOnMouseClicked(e -> {
            // if the user clicks on the arrow down button of the combo-box update it's model (each time)
            if (e.getTarget() instanceof StackPane && "arrow-button".equals(((StackPane)e.getTarget()).getId())) {
                loader.run();
                updatedComboboxes.add(comboBox);
            }
        });

        // if the user change a combobox value and leave it the states of all other boxes will invalidated.
        // On this way it will ensured that every box get the current data that are valid for the all selected filters
        comboBox.setOnSelectionCompleted(e -> {
            updatedComboboxes.clear();
            actions.updateMetricsGraph();
        });
    }


    /**
     * Creates a list with dummy items which should be shown while the backend system is calculating the real items.
     *
     * @param first the first item.
     * @param other a generator for the other items.
     * @param <T>   the actual type of all items.
     * @return a list with the created items.
     */
    private <T> List<T> initDummyItems(T first, Supplier<T> other) {
        List<T> dummyItems = new ArrayList<>();
        dummyItems.add(first);
        for (int i = 0; i < NUMBER_DUMMY_LOADING_ITEMS; i++) {
            dummyItems.add(other.get());
        }
        return dummyItems;
    }

    public void setComputeParameters(ComputationSpec queryComputeParams) {

        viewModel.suppressEventHandling(true);

        viewModel.thresholdProperty().setValue(queryComputeParams.getThreshold());
        viewModel.maxDisplaySeriesProperty().setValue(queryComputeParams.getMaxDisplaySeries());
        viewModel.seriesCombineModeProperty().setValue(queryComputeParams.getSeriesCombineMode());
        viewModel.smoothingGranularityProperty().setValue(queryComputeParams.getSeriesSmoothingGranularity());
        viewModel.currentSmoothingTypeProperty().setValue(queryComputeParams.getSeriesSmoothingType());
        viewModel.seriesCombineModeProperty().setValue(queryComputeParams.getSeriesCombineMode());

        viewModel.suppressEventHandling(false);
    }


    public class CreateGraphInvalidationListener implements InvalidationListener {

        @Override
        public void invalidated(Observable observable) {
            if (viewModel.isSuppressEventHandling()){
            }

//            actions.updateMetricsGraph();
        }
    }

    public class CreateGraphEventHandler implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {

            if (viewModel.isSuppressEventHandling()){
                return;
            }

            actions.updateMetricsGraph();
        }
    }

    /**
     * Callback for handling actions
     */
    @SuppressWarnings("all")
    @Deprecated(forRemoval = false)
    public static class ActionCallbacks extends QueryEditorPort.Actions {
        /**
         * Handles the button clicked event on the addToGraphButton
         */
        public void pushSnapshot() {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Deletes all Metrics from the Graph.
         */
        public void clearCharts() {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Creates the Metrics for the Graph.
         */
        public void updateMetricsGraph() {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Creates a new bookmark. A Dialog is shown, in this dialog the user can choose the bookmark's name.
         */
        public void createBookmark() {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Changes the Sampling smoothingGranularity
         *
         * @param smoothingGranularity new Sampling smoothingGranularity
         */
        public void changeSmoothingGranularity(@SuppressWarnings("unused") SeriesSmoothingGranularity smoothingGranularity) {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Changes the smoothing type for the sampling
         *
         * @param aggregation new smoothing type for the sampling
         */
        public void changeSmoothingType(@SuppressWarnings("unused") SeriesSmoothingType aggregation) {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Sets the combine mode which controls if multiple time series should combine to a single one
         * and if which algorithm should be used.
         *
         * @param combineMode the combine mode that will be used (NONE for disable combining)
         */
        public void changeSeriesCombineMode(@SuppressWarnings("unused") SeriesCombineMode combineMode) {

        }

        /**
         * Changes the graph type
         *
         * @param graphType new graph type
         */
        public void changeGraphType(@SuppressWarnings("unused") String graphType) {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Changes the threshold of the points of the chart
         *
         * @param threshold the threshold of the points of the chart
         */
        public void changeThreshold(@SuppressWarnings("unused") int threshold) {
            // Default implementation does nothing; can be overridden
        }

        /**
         * Changes the maximum number of series displayed in the chart
         *
         * @param maxDisplaySeries the maximum number of series
         */
        public void changeMaxDisplaySeries(@SuppressWarnings("unused") int maxDisplaySeries) {
            // Default implementation does nothing; can be overridden
        }
    }
}
