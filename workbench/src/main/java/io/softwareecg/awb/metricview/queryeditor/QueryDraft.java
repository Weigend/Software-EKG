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
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.repository.model.TimeSeriesImportSource;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static javafx.collections.FXCollections.observableArrayList;

/**
 * The Class QueryDraft.
 *
 */
@SuppressWarnings("all")
public class QueryDraft {

    public static final Map<FilterDimension, String> DEFAULT_FILTER_DIMENSION_LABELS = new HashMap<>();

    static {
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.HOST_GROUP, "Clusters/Devices");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.HOST, "Host/Device");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.NAMESPACE, "Namespace");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.SERVICE, "Service");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.POD, "Pod");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.CONTAINER, "Container");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.MEASUREMENT, "Measurement");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.PROCESS_GROUP, "Process Group");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.PROCESS, "Process");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.METRIC_GROUP, "Metric Type");
        DEFAULT_FILTER_DIMENSION_LABELS.put(FilterDimension.METRIC_NAME, "Metric");
    }


    private final ObjectProperty<Repository> repository = new SimpleObjectProperty<>();

    /* ---------------------------- generic filter properties (I) -------------------------------- */

    private final ListProperty<Measurement> measurements = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Measurement> currentMeasurement = new SimpleObjectProperty<>(new Measurement("*", -1, -1));

    /* ------------------------------ classic filter properties ---------------------------------- */

    private final ListProperty<HostGroup> hostGroups = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<HostGroup> currentHostGroup = new SimpleObjectProperty<>(new HostGroup("*"));

    private final ListProperty<Host> hosts = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Host> currentHost = new SimpleObjectProperty<>(new Host("*"));

    /* ------------------------------- cloud filter properties ----------------------------------- */

    private final ListProperty<Namespace> namespaces = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Namespace> currentNamespace = new SimpleObjectProperty<>(new Namespace("*"));

    private final ListProperty<Service> services = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Service> currentService = new SimpleObjectProperty<>(new Service("*"));

    private final ListProperty<Pod> pods = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Pod> currentPod = new SimpleObjectProperty<>(new Pod("*"));

    private final ListProperty<Container> containers = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Container> currentContainer = new SimpleObjectProperty<>(new Container("*"));

    /* ---------------------------- generic filter properties (II) ------------------------------- */

    private final ListProperty<ProcessGroup> processGroups = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<ProcessGroup> currentProcessGroup = new SimpleObjectProperty<>(new ProcessGroup("*"));

    private final ListProperty<Process> process = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Process> currentProcess = new SimpleObjectProperty<>(new Process("*", "*"));

    private final ListProperty<MetricGroup> metricMetricGroups = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<MetricGroup> currentMetricGroup = new SimpleObjectProperty<>(new MetricGroup("*"));

    /* ------------------------------- compute params roperties ----------------------------------- */

    private final ObjectProperty<SeriesSmoothingType> smoothingType = new SimpleObjectProperty<>(SeriesSmoothingType.NONE);
    private final ObjectProperty<SeriesSmoothingGranularity> smoothingGranularity = new SimpleObjectProperty<>(SeriesSmoothingGranularity.AUTO);
    private final ObjectProperty<SeriesCombineMode> seriesCombineMode = new SimpleObjectProperty<>(SeriesCombineMode.NONE.NONE);

    /* --------------------------- generic filter properties (III) ------------------------------- */

    private final ObjectProperty<Project> currentProject = new SimpleObjectProperty<>();
    private final ListProperty<Metric> metrics = new SimpleListProperty<>(observableArrayList());
    private final ObjectProperty<Metric> currentMetric = new SimpleObjectProperty<>();

    private final ObjectProperty<Metric> excludeMetric = new SimpleObjectProperty<>();

    private final StringProperty rawQuery = new SimpleStringProperty();

    private final ObjectProperty<Instant> start = new SimpleObjectProperty<>();
    private final ObjectProperty<Instant> stop = new SimpleObjectProperty<>();

    /* --------------------------- other view component properties ------------------------------- */

    private final ObjectProperty<Integer> threshold = new SimpleObjectProperty<>(500);
    private final ObjectProperty<Integer> maxDisplaySeries = new SimpleObjectProperty<>(20);
    private final BooleanProperty expertMode = new SimpleBooleanProperty(false);
    private final BooleanProperty multiMetricMode = new SimpleBooleanProperty(false);
    private final BooleanProperty cloudMode = new SimpleBooleanProperty(false);
    private TimeSeriesImportSource importerSourceRepository = null;

    /* -------------------------------- internal model state ------------------------------------ */

    private boolean suppressEventHandling = false;


    /* ================================================ Model API =================================================== */

    public boolean isSuppressEventHandling() {
        return suppressEventHandling;
    }

    /**
     * Sets the current excludeMetric.
     *
     * @param excludeMetric the new current excludeMetric
     */
    public void setExcludeMetric(Metric excludeMetric) {
        this.excludeMetric.set(excludeMetric);
    }

    /**
     * Sets the current start.
     *
     * @param start the start
     */
    public void setCurrentStart(Instant start) {
        this.start.set(start);
    }

    /**
     * Get the start date property
     *
     * @return return start date property
     */
    public Property<Instant> startDateProperty() {
        return start;
    }

    /**
     * Sets the current stop.
     *
     * @param stop the stop
     */
    public void setCurrentStop(Instant stop) {
        this.stop.set(stop);
    }

    /**
     * Get the calendar stop date property
     *
     * @return return stop date property
     */
    public Property<Instant> stopDateProperty() {
        return stop;
    }

    /**
     * Gets the expert mode.
     *
     * @return returns the expert mode value(true if the expert mode is enabled, false otherwise)
     */
    public boolean isExpertMode() {
        return expertMode.get();
    }

    /**
     * Sets the expert mode
     *
     * @param expertMode true for expert mode
     */
    public void setExpertMode(boolean expertMode) {
        this.expertMode.set(expertMode);
    }

    /**
     * Gets the expert mode property, e.g. used for binding operations.
     *
     * @return - the expert mode property
     */
    public BooleanProperty getExpertModeProperty() {
        return expertMode;
    }

    /**
     * @return the raw query.
     */
    public String getRawQuery() {
        return rawQuery.get();
    }

    /**
     * @return the raw query property
     */
    public StringProperty rawQueryProperty() {
        return rawQuery;
    }

    /**
     * @param rawQuery the new raw query.
     */
    public void setRawQuery(String rawQuery) {
        this.rawQuery.set(rawQuery);
    }

    /**
     * Gets the multi metric mode.
     *
     * @return returns the current value of the multi metric mode (true if enabled, false otherwise)
     */
    public boolean isMultiMetricMode() {
        return multiMetricMode.get();
    }

    /**
     * Set the current multi metric mode.
     *
     * @param multiMetricMode - the new value
     */
    public void setMultiMetricMode(boolean multiMetricMode) {
        this.multiMetricMode.set(multiMetricMode);
    }

    /**
     * Get the current working types.
     *
     * @return the working types.
     */
    public Repository getRepository() {
        return repository.get();
    }

    /**
     * Set the working types.
     *
     * @param repository the working types.
     */
    public void setRepository(Repository repository) {
        this.repository.set(repository);
    }

    /**
     * Get the working types property.
     *
     * @return the working types property.
     */
    public Property<Repository> repositoryProperty() {
        return repository;
    }

    /**
     * Get the host property
     *
     * @return return host property
     */
    public Property<Host> currentHostProperty() {
        return currentHost;
    }

    /**
     * Returns the an observable list with {@link HostGroup} entities
     * used as filter parameters
     *
     * @return a list of HostGroup
     */
    public ObservableList<HostGroup> getHostGroups() {
        return hostGroups.get();
    }

    /**
     * Returns the ObjectProperty that stores the {@link HostGroup} entity
     * used as filter param
     *
     * @return  ObjectProperty that stores the HostGroup
     */
    public Property<HostGroup> currentHostGroupProperty() {
        return currentHostGroup;
    }

    /**
     * Get the process property
     *
     * @return return process property
     */
    public Property<Process> currentProcessProperty() {
        return currentProcess;
    }

    /**
     * Get the group property
     *
     * @return return group property
     */
    public Property<MetricGroup> currentMetricGroupProperty() {
        return currentMetricGroup;
    }

    /**
     * @return the current metric
     */
    public Metric getCurrentMetric() {
        return currentMetric.get();
    }

    /**
     * Get the metric property
     *
     * @return return metric property
     */
    public Property<Metric> currentMetricProperty() {
        return currentMetric;
    }

    /**
     * @param currentMetric the current metric
     */
    public void setCurrentMetric(Metric currentMetric) {
        this.currentMetric.set(currentMetric);
    }

    /**
     * Get the smoothing property
     *
     * @return return smoothing property
     */
    public Property<SeriesSmoothingType> currentSmoothingTypeProperty() {
        return smoothingType;
    }


    public ObjectProperty<SeriesSmoothingGranularity> smoothingGranularityProperty() {
        return smoothingGranularity;
    }

    public ObjectProperty<SeriesCombineMode> seriesCombineModeProperty() {
        return seriesCombineMode;
    }

    /**
     * Get the series property
     *
     * @return return series property
     */
    public Property<Measurement> currentMeasurementProperty() {
        return currentMeasurement;
    }

    /**
     * Gets the metricMetricGroups.
     *
     * @return the metricMetricGroups
     */
    public ObservableList<MetricGroup> getMetricMetricGroups() {
        return metricMetricGroups;
    }

    /**
     * Gets the hosts.
     *
     * @return the hosts
     */
    public ObservableList<HostGroup> getHostsGroups() {
        return hostGroups;
    }

    /**
     * Gets the hosts.
     *
     * @return the hosts
     */
    public ObservableList<Host> getHosts() {
        return hosts;
    }

    /**
     * Gets the metrics.
     *
     * @return the metrics
     */
    public ObservableList<Metric> getMetrics() {
        return metrics;
    }

    /**
     * @return the process groups the filter offers
     */
    public ObservableList<ProcessGroup> getProcessGroups() {
        return processGroups;
    }

    /**
     * @return the process group the chart is filtered by
     */
    public Property<ProcessGroup> currentProcessGroupProperty() {
        return currentProcessGroup;
    }

    /**
     * Gets the process.
     *
     * @return the process
     */
    public ObservableList<Process> getProcess() {
        return process;
    }

    /**
     * Gets the measurements.
     *
     * @return the process
     */
    public ObservableList<Measurement> getMeasurements() {
        return measurements;
    }


    public ObservableList<Namespace> getNamespaces() {
        return namespaces.get();
    }

    public ListProperty<Namespace> namespacesProperty() {
        return namespaces;
    }

    public Namespace getCurrentNamespace() {
        return currentNamespace.get();
    }

    public ObjectProperty<Namespace> currentNamespaceProperty() {
        return currentNamespace;
    }

    public ObservableList<Service> getServices() {
        return services.get();
    }

    public ListProperty<Service> servicesProperty() {
        return services;
    }

    public Service getCurrentService() {
        return currentService.get();
    }

    public ObjectProperty<Service> currentServiceProperty() {
        return currentService;
    }

    public ObservableList<Pod> getPods() {
        return pods.get();
    }

    public ListProperty<Pod> podsProperty() {
        return pods;
    }

    public Pod getCurrentPod() {
        return currentPod.get();
    }

    public ObjectProperty<Pod> currentPodProperty() {
        return currentPod;
    }

    public ObservableList<Container> getContainers() {
        return containers.get();
    }

    public ListProperty<Container> containersProperty() {
        return containers;
    }

    public Container getCurrentContainer() {
        return currentContainer.get();
    }

    public ObjectProperty<Container> currentContainerProperty() {
        return currentContainer;
    }

    public Project getCurrentProject() {
        return currentProject.get();
    }

    public ObjectProperty<Project> currentProjectProperty() {
        return currentProject;
    }

    public boolean isCloudMode() {
        return cloudMode.get();
    }

    public BooleanProperty cloudModeProperty() {
        return cloudMode;
    }

    /**
     * As query params.
     *
     * @return the query context
     */
    public QuerySpec asQueryParams() {
        return new QuerySpec.Builder()
                .withMetric(currentMetric.get())
                .withMetricGroup(currentMetricGroup.get())
                .withExclude(excludeMetric.get())
                .withMultiMetricMode(multiMetricMode.get())
                .withExpertMode(expertMode.get())
                .withHost(currentHost.get())
                .withHostGroup(currentHostGroup.get())
                .withMeasurement(currentMeasurement.get())
                .withNamespace(currentNamespace.get())
                .withService(currentService.get())
                .withPod(currentPod.get())
                .withContainer(currentContainer.get())
                .withProcessGroup(currentProcessGroup.get())
                .withProcess(currentProcess.get())
                .withStart(start.get() == null ? -1 : start.get().toEpochMilli())
                .withStop(stop.get() == null ? -1 : stop.get().toEpochMilli())
                .withRawQuery(rawQuery.get())
                .withProject(currentProject.get())
                .withImporterSourceRepository(importerSourceRepository)
                .build();
    }

    /**
     * Returns the compute settings as ComputationSpec instance that defines
     * how two post process the metric data.
     *
     * @return the user defined compute parameters
     */
    public ComputationSpec asComputeParams() {
        ComputationSpec params = new ComputationSpec();
        params.setThreshold(threshold.get());
        params.setMaxDisplaySeries(maxDisplaySeries.get());
        params.setSeriesCombineMode(seriesCombineMode.get());
        params.setSeriesSmoothingGranularity(smoothingGranularity.get());
        params.setSeriesSmoothingType(smoothingType.get());
        return params;
    }

    /**
     * Sets the model from a query context and overwrites all
     * data properties that are provided by the query context
     *
     * @param queryContext the new query context for the values.
     */
    public void loadDataFromQueryContext(QuerySpec queryContext) {

        // we lock the model to signal the controller to prevent any update actions until the model update is finished
        this.suppressEventHandling = true;

        // classic world
        currentHostGroup.set(queryContext.getHostGroup());
        currentHost.set(queryContext.getHost());

        // cloud native world
        currentNamespace.set(queryContext.getNamespace());
        currentService.set(queryContext.getService());
        currentPod.set(queryContext.getPod());
        currentContainer.set(queryContext.getContainer());

        // common stuff
        currentProject.set(queryContext.getProject());
        // The process group as well. A metric opened below a process group in the explorer
        // brings one along; the header used to drop it, and the first change of another
        // field showed the series of all process groups.
        currentProcessGroup.set(queryContext.getProcessGroup() == null
                ? new ProcessGroup("*") : queryContext.getProcessGroup());
        currentProcess.set(queryContext.getProcess());
        currentMetricGroup.set(queryContext.getMetricGroup());
        currentMetric.set(queryContext.getMetric());


        multiMetricMode.set(queryContext.isMultiMetricMode());
        importerSourceRepository = queryContext.getImporterSourceRepository();

        // Do not loss the date
        if (queryContext.getStart() >= 0) {
            start.set(Instant.ofEpochMilli(queryContext.getStart()));
        }

        if (queryContext.getEnd() >= 0) {
            stop.set(Instant.ofEpochMilli(queryContext.getEnd()));
        }

        currentMeasurement.set(queryContext.getMeasurement());
        excludeMetric.set(queryContext.getExcludeMetric());
        expertMode.set(queryContext.isExpertMode());
        rawQuery.set(queryContext.getRawQuery());

        // now the model is finished and others can do their stuff
        this.suppressEventHandling = false;
    }

    /**
     * Returns the selected value for the threshold (max data points per individual series)
     *
     * @return the selected value for the threshold
     */
    public Property<Integer> thresholdProperty() {
        return threshold;
    }

    /**
     * Returns the threshold values for the combobox.
     * These represent the maximum number of data points per individual series.
     * -1 means auto/unlimited, 0 means only value-changes.
     *
     * @return the threshold values
     */
    public List<Integer> getThresholds() {
        return asList(new Integer[]{-1, 0, 200, 500, 1000, 2000, 5000, 10000, 20000});
    }

    /**
     * Returns the max display series property (max number of series shown simultaneously)
     *
     * @return the max display series property
     */
    public Property<Integer> maxDisplaySeriesProperty() {
        return maxDisplaySeries;
    }

    /**
     * Returns the possible values for the max display series combobox.
     *
     * @return the max display series values
     */
    public List<Integer> getMaxDisplaySeriesValues() {
        return asList(new Integer[]{5, 10, 15, 20, 30, 50, 100});
    }

    public void suppressEventHandling(boolean suppress) {
        suppressEventHandling = suppress;
    }


    /**
     * An {@link ObjectProperty} implementation that do not allow to set {@code null} values. When trying to set {@code
     * null} than the {@link Supplier} of {@code defaultValue} is executed and the returned value is set instead the
     * null-value.
     *
     * @param <T> Type of the stored value.
     */
    private static class NullDefaultObjectProperty<T> extends SimpleObjectProperty<T> {
        private final Supplier<T> defaultValue;

        /**
         * The constructor of {@code ObjectProperty}
         *
         * @param bean         the bean of this {@code ObjectProperty}
         * @param name         the name of this {@code ObjectProperty}
         * @param initialValue the initial value of the wrapped value
         * @param defaultValue the supplier for the default value
         */
        private NullDefaultObjectProperty(Object bean, String name, T initialValue, Supplier<T> defaultValue) {
            super(bean, name, initialValue);
            this.defaultValue = defaultValue;
        }

        @Override
        public void set(T newValue) {
            if (newValue == null) {
                super.set(defaultValue == null ? null : defaultValue.get());
            } else {
                super.set(newValue);
            }
        }
    }
}
