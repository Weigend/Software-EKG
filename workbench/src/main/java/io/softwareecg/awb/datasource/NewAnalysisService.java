package io.softwareecg.awb.datasource;

import io.softwareecg.awb.datasource.model.HostAddress;
import io.softwareecg.awb.repository.api.Repository;
import io.avaje.inject.External;
import io.softwareecg.awb.metadata.api.CollectionSourceEvent;
import io.softwareecg.awb.metadata.api.CollectionSourceRow;
import io.softwareecg.awb.metadata.api.CollectionSourceStore;
import io.softwareecg.awb.secrets.api.SecretStore;
import io.softwareecg.awb.repository.api.Embedded;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusDiscovery;
import io.softwareecg.awb.datasource.adapter.ssh.SshDiscovery;
import io.softwareecg.awb.datasource.adapter.windows.WindowsDiscovery;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment;
import io.softwareecg.awb.projectcontext.api.ProjectDataAccessService;
import io.softwareecg.awb.projectcontext.spi.ProjectDataCollection;
import io.softwareecg.awb.projectcontext.api.ProjectDataCollectionChangedEvent;
import io.softwareecg.awb.projectcontext.api.Project;
import io.softwareecg.awb.repository.api.SeriesDataWrittenEvent;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.awb.projectcontext.api.CloudPlatformType;
import io.softwareecg.awb.projectcontext.api.ProjectFlavor;
import io.softwareecg.awb.repository.api.TimeSeriesWriteService;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.control.CollectorEngine;
import io.softwareecg.collector.control.CollectorJobStatus;
import io.softwareecg.collector.control.CollectorStatus;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.source.prometheus.PrometheusCounterSource;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.softwareecg.awb.datasource.adapter.collector.CollectorConfigCompiler;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusConfigCompiler;
import io.softwareecg.awb.datasource.adapter.collector.ProjectSeriesOutput;
import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.model.DataSourceInfo;
import io.softwareecg.awb.datasource.model.RepositorySeriesEnds;
import io.softwareecg.awb.datasource.model.SeriesEnds;
/**
 * Connects analyses to their data: looks at what a Prometheus server offers, starts an analysis
 * from the user's selection, keeps its data current and lets the user manage the sources
 * afterwards - pause, reconnect, change the connection, remove.
 * <p>
 * An analysis can have several data sources. Each source has its own collector engine. The
 * engines run while the AWB runs; when it is started again they continue where the stored data
 * ends, so the time in between is filled from the history Prometheus keeps.
 * <p>
 * Credentials never go into the metadata database or a configuration: they live in the
 * {@link SecretStore} and are filled in only for the running engine.
 */
@Singleton
public class NewAnalysisService implements ProjectDataCollection {

    /**
     * The collection runs in a collector embedded into the AWB. Other modes - a collector
     * process of its own, a collector on another machine - have their place in the data model.
     */
    public static final String EXECUTION_EMBEDDED = "EMBEDDED";

    private static final Logger LOGGER = LoggerFactory.getLogger(NewAnalysisService.class);

    /**
     * The id under which projects created here are shown with the Prometheus look in the explorer.
     */
    private static final String PROMETHEUS_IMPORTER_ID = "PrometheusImporter";

    private static final long MONITOR_INTERVAL_SECONDS = 5;

    private final Repository repository;

    private final CollectionSourceStore sourceStore;

    private final SecretStore secretStore;

    /** The running engines, by source id. */
    private final Map<String, CollectorEngine> engines = new ConcurrentHashMap<>();

    /** Why a source that should run could not be started, by source id. */
    private final Map<String, String> startProblems = new ConcurrentHashMap<>();

    /** The state last written to the history of a source, to record changes only. */
    private final Map<String, String> recordedStates = new ConcurrentHashMap<>();

    /**
     * The sources as last read from or written to the store, by id. Lets the state of a project
     * be told without going to the database, which views ask for often.
     */
    private final Map<String, CollectionSourceRow> knownSources = new ConcurrentHashMap<>();

    /** The state of each project as last announced, to announce changes only. */
    private final Map<String, String> announcedProjectStates = new ConcurrentHashMap<>();

    private ScheduledExecutorService monitor;

    @Inject
    public NewAnalysisService(@External @Embedded Repository repository,
                                 @External CollectionSourceStore sourceStore,
                                 @External SecretStore secretStore) {
        this.repository = repository;
        this.sourceStore = sourceStore;
        this.secretStore = secretStore;
    }

    //=================================================================================================================
    // looking at a source and starting an analysis
    //=================================================================================================================

    /**
     * Looks at what the Prometheus server monitors. Fast: no series or values are transferred.
     *
     * @throws io.softwareecg.collector.source.prometheus.PrometheusException with a message for
     *         the user if the server cannot be reached or rejects the credentials
     */
    public PrometheusEnvironment discover(String url, String username, String password) {
        return PrometheusDiscovery.discover(url, username, password);
    }

    /**
     * Looks at hosts that are to be read directly: whether they can be reached the way the
     * collector will reach them, and which processes run there.
     *
     * @param username the user to log in with, or {@code null}
     * @param password the password, or {@code null} to log in with the SSH key
     * @return one result per host; a host that cannot be read says why
     */
    public List<SshDiscovery.HostCheck> inspectHosts(List<HostAddress> hosts, String username, String password) {
        return SshDiscovery.inspect(hosts, username, password);
    }

    /**
     * Says why Windows performance counters cannot be read by this AWB: it does not run on
     * Windows, or the library that reads them is missing.
     *
     * @return the reason in words for the user, {@code null} if they can be read
     */
    public String windowsCountersProblem() {
        return WindowsDiscovery.problem();
    }

    /**
     * Looks at Windows hosts that are to be read through their performance counters: whether
     * that works from this machine, and which processes run there.
     *
     * @return one result per host; a host that cannot be read says why
     */
    public List<WindowsDiscovery.HostCheck> inspectWindowsHosts(List<HostAddress> hosts) {
        return WindowsDiscovery.inspect(hosts);
    }

    /**
     * Creates the project of an analysis with its first data source and starts collecting.
     * The first values arrive within seconds; the history is filled in the background.
     * <p>
     * Either all of it happens or none: if the source cannot be set up, the project is not
     * left behind and its name stays free.
     *
     * @return the created project
     * @throws IllegalArgumentException if the plan is incomplete or a project of that name exists
     * @throws IllegalStateException    if the analysis could not be started
     */
    public Project startAnalysis(CollectionPlan plan) {
        validate(plan);
        ProjectDataAccessService projects = projects();
        String name = plan.projectName().trim();
        if (projects.listProjects().stream().anyMatch(project -> name.equalsIgnoreCase(project.getName()))) {
            throw new IllegalArgumentException("An analysis named '" + name + "' already exists.");
        }

        // everything that can be checked is checked before anything is created
        CollectionSourceRow source = toSource(plan, name);
        parse(source, plan.secret());

        Project project = new Project(name, "Collected from " + plan.endpoint(),
                ProjectFlavor.CLASSIC.name(), CloudPlatformType.NONE.name());
        project.setImporterId(PROMETHEUS_IMPORTER_ID);
        project = projects.persistProject(project);

        try {
            register(source, plan.secret());
        } catch (RuntimeException e) {
            try {
                projects.deleteProjectByName(name);
            } catch (RuntimeException cleanupFailure) {
                LOGGER.warn("Could not remove project '{}' after its data source failed to start.", name, cleanupFailure);
            }
            throw new IllegalStateException("The analysis could not be started: " + message(e), e);
        }

        publish(new ExplorerUpdateEvent(repository.getId()));
        return project;
    }

    /**
     * Adds a further data source to an existing analysis and starts collecting from it.
     *
     * @return the id of the new source
     * @throws IllegalArgumentException if the plan is incomplete or the analysis does not exist
     */
    public String addSource(CollectionPlan plan) {
        validate(plan);
        ProjectDataAccessService projects = projects();
        String name = projects.listProjects().stream().map(Project::getName)
                .filter(existing -> existing.equalsIgnoreCase(plan.projectName().trim())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There is no analysis named '" + plan.projectName() + "'."));

        CollectionSourceRow source = toSource(plan, name);
        parse(source, plan.secret());
        register(source, plan.secret());
        return source.id;
    }

    //=================================================================================================================
    // managing the sources
    //=================================================================================================================

    /**
     * @return all data sources with the state of their collection, ordered by analysis
     */
    public List<DataSourceInfo> sources() {
        List<DataSourceInfo> result = new ArrayList<>();
        List<CollectionSourceRow> stored = sourceStore.listAll();
        remember(stored);
        for (CollectionSourceRow source : stored) {
            result.add(describe(source));
        }
        return result;
    }

    /**
     * @return the data sources of one analysis with the state of their collection
     */
    public List<DataSourceInfo> sourcesOf(String projectName) {
        return sources().stream().filter(source -> source.projectName().equals(projectName)).toList();
    }

    /**
     * The state of the data collection of a project, all its sources taken together. Answered
     * from memory, so views can ask whenever they show a project.
     */
    @Override
    public Status statusOf(String projectName) {
        State worst = null;
        String detail = null;
        Instant lastValue = null;
        int count = 0;
        for (CollectionSourceRow source : knownSources.values()) {
            if (!source.projectName.equals(projectName)) {
                continue;
            }
            count++;
            DataSourceInfo info = describe(source);
            if (info.lastValue() != null && (lastValue == null || info.lastValue().isAfter(lastValue))) {
                lastValue = info.lastValue();
            }
            State state = switch (info.state()) {
                case RUNNING -> State.RUNNING;
                case STARTING, WAITING -> State.WAITING;
                case ERROR -> State.ERROR;
                case PAUSED, STOPPED -> State.PAUSED;
            };
            if (state == State.ERROR && detail == null) {
                detail = info.detail();
            }
            if (worst == null || severity(state) > severity(worst)) {
                worst = state;
            }
        }
        return count == 0 ? Status.NONE : new Status(worst, count, lastValue, detail);
    }

    /** An error counts more than still connecting, that more than running, that more than paused. */
    private static int severity(State state) {
        return switch (state) {
            case ERROR -> 4;
            case WAITING -> 3;
            case RUNNING -> 2;
            case PAUSED -> 1;
            case NONE -> 0;
        };
    }

    /**
     * @return the newest entries in the history of a source, newest first
     */
    public List<CollectionSourceEvent> history(String sourceId, int limit) {
        return sourceStore.listEvents(sourceId, limit);
    }

    /**
     * Stops collecting from a source until it is resumed. The source and its data stay.
     */
    public void pause(String sourceId) {
        CollectionSourceRow source = require(sourceId);
        source.enabled = false;
        sourceStore.update(source);
        knownSources.put(source.id, source);
        stopEngine(sourceId);
        record(sourceId, DataSourceInfo.State.PAUSED, null);
        announce(source.projectName);
    }

    /**
     * Collects from a paused source again. It continues where its stored data ends.
     */
    public void resume(String sourceId) {
        CollectionSourceRow source = require(sourceId);
        source.enabled = true;
        sourceStore.update(source);
        knownSources.put(source.id, source);
        tryStart(source);
        announce(source.projectName);
    }

    /**
     * Drops the connection to a source and builds it up again, e.g. after the source was
     * unreachable for a while or its credentials were renewed outside the AWB.
     */
    public void reconnect(String sourceId) {
        CollectionSourceRow source = require(sourceId);
        stopEngine(sourceId);
        if (source.enabled) {
            tryStart(source);
        }
    }

    /**
     * Changes how a source is reached and reconnects. What is collected stays as it is.
     *
     * @param url      the new address
     * @param username the user for basic authentication, or blank
     * @param password the new password or token; {@code null} keeps the stored one, blank removes it
     */
    public void updateConnection(String sourceId, String url, String username, String password) {
        CollectionSourceRow source = require(sourceId);
        if (!PrometheusCounterSource.ID.equals(source.sourceType)) {
            // hosts and logins of other sources are a list, not one address
            throw new IllegalArgumentException("The connection of this kind of source cannot be changed here. "
                    + "Remove the source and add it again.");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("The source needs an address.");
        }

        String secretRef = source.secretRef;
        if (password != null) {
            if (password.isBlank()) {
                if (secretRef != null) {
                    secretStore.delete(secretRef);
                }
                secretRef = null;
            } else {
                secretRef = secretRef != null ? secretRef : secretReference(source.id);
                secretStore.store(secretRef, password);
            }
        }

        source.config = PrometheusConfigCompiler.withConnection(source.config, url.trim(), username, secretRef != null);
        source.endpoint = url.trim();
        source.secretRef = secretRef;
        parse(source, secretRef == null ? null : secretStore.read(secretRef));
        sourceStore.update(source);
        knownSources.put(source.id, source);
        reconnect(sourceId);
    }

    /**
     * Stops collecting from a source and forgets it together with its credentials. The analysis
     * and the data collected so far stay.
     */
    public void remove(String sourceId) {
        CollectionSourceRow source = sourceStore.find(sourceId);
        CollectionSourceRow known = knownSources.remove(sourceId);
        stopEngine(sourceId);
        startProblems.remove(sourceId);
        recordedStates.remove(sourceId);
        if (source != null) {
            sourceStore.delete(sourceId);
            if (source.secretRef != null) {
                secretStore.delete(source.secretRef);
            }
        } else if (known != null && known.secretRef != null) {
            // the source went away with its project; its secret is left to clean up
            quietly(() -> secretStore.delete(known.secretRef));
        }
        if (known != null) {
            announce(known.projectName);
        }
    }

    /**
     * @return the collector configuration of a source, to run it in a stand-alone collector.
     *         It contains no credentials: the password or token is expected in the environment
     *         variable {@value CollectionPlan#SECRET_ENVIRONMENT_VARIABLE}.
     */
    public String exportCollectorConfig(String sourceId) {
        return CollectorConfigCompiler.asYaml(require(sourceId).config);
    }

    //=================================================================================================================
    // life cycle
    //=================================================================================================================

    /**
     * Starts collecting for every stored source that should run and does not yet. Called when
     * the AWB starts. A source that cannot be started shows its problem in its state; the
     * others are not affected.
     */
    public void startAll() {
        List<CollectionSourceRow> stored = sourceStore.listAll();
        remember(stored);
        for (CollectionSourceRow source : stored) {
            if (source.enabled && !engines.containsKey(source.id)) {
                tryStart(source);
            }
        }
        startMonitor();
        stored.stream().map(source -> source.projectName).distinct().forEach(this::announce);
    }

    /**
     * Stops collecting for all sources. Called when the AWB is closed. The sources stay enabled
     * and start again with the AWB.
     */
    public void stopAll() {
        stopMonitor();
        List<String> running = new ArrayList<>(engines.keySet());
        running.forEach(this::stopEngine);
    }

    /**
     * @return the repository the analyses started here are stored in
     */
    public Repository getRepository() {
        return repository;
    }

    //=================================================================================================================
    // internals
    //=================================================================================================================

    private static void validate(CollectionPlan plan) {
        if (plan.projectName() == null || plan.projectName().isBlank()) {
            throw new IllegalArgumentException("The analysis needs a name.");
        }
        if (!plan.isComplete()) {
            throw new IllegalArgumentException("Nothing is selected to collect.");
        }
    }

    private static CollectionSourceRow toSource(CollectionPlan plan, String projectName) {
        CollectionSourceRow source = new CollectionSourceRow();
        source.id = UUID.randomUUID().toString();
        source.projectName = projectName;
        source.sourceType = plan.sourceType();
        source.executionMode = EXECUTION_EMBEDDED;
        source.endpoint = plan.endpoint();
        source.config = plan.toCollectorConfig(projectName);
        source.secretRef = plan.secret() == null || plan.secret().isBlank() ? null : secretReference(source.id);
        source.enabled = true;
        return source;
    }

    private static String secretReference(String sourceId) {
        return "source-" + sourceId;
    }

    /**
     * Stores a new source with its secret and starts it. If any step fails, the steps before
     * are undone and the failure is passed on.
     */
    private void register(CollectionSourceRow source, String secret) {
        boolean secretStored = false;
        boolean inserted = false;
        try {
            source.seriesNamespace = seriesNamespaceFor(source.projectName, source.endpoint, source.sourceType);
            if (source.secretRef != null) {
                secretStore.store(source.secretRef, secret);
                secretStored = true;
            }
            sourceStore.insert(source);
            inserted = true;
            knownSources.put(source.id, source);
            start(source);
            announce(source.projectName);
        } catch (RuntimeException e) {
            knownSources.remove(source.id);
            stopEngine(source.id);
            if (inserted) {
                quietly(() -> sourceStore.delete(source.id));
            }
            if (secretStored) {
                quietly(() -> secretStore.delete(source.secretRef));
            }
            throw e;
        }
    }

    /** Starts a stored source; a failure becomes its visible problem instead of an exception. */
    private void tryStart(CollectionSourceRow source) {
        try {
            start(source);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not start collecting from {} for project '{}': {}",
                    source.endpoint, source.projectName, message(e), e);
            startProblems.put(source.id, message(e));
            record(source.id, DataSourceInfo.State.ERROR, message(e));
        }
    }

    private void start(CollectionSourceRow source) {
        stopEngine(source.id);
        startProblems.remove(source.id);

        String secret = source.secretRef == null ? null : secretStore.read(source.secretRef);
        if (source.secretRef != null && secret == null) {
            throw new IllegalStateException("The credentials of this source are missing. Enter them again.");
        }

        String sourceId = source.id;
        String projectName = source.projectName;
        AtomicBoolean firstData = new AtomicBoolean(true);
        ProjectSeriesOutput output = new ProjectSeriesOutput(projectName, source.seriesNamespace, importService(),
                seriesEnds(), newestTimestamp -> {
            if (sourceStore.find(sourceId) == null) {
                // the analysis was deleted in the meantime: nothing to collect for anymore.
                // Not on this thread - it belongs to the engine that is about to be stopped.
                Thread cleaner = new Thread(() -> remove(sourceId), "stop-collecting-" + projectName);
                cleaner.setDaemon(true);
                cleaner.start();
                return;
            }
            if (firstData.compareAndSet(true, false)) {
                // the first values bring new systems and metrics into the explorer tree
                publish(new ExplorerUpdateEvent(repository.getId()));
            }
            publish(new SeriesDataWrittenEvent(this, repository.getId(), projectName, newestTimestamp));
        });

        engines.put(sourceId, startEngine(parse(source, secret), output));
        record(sourceId, DataSourceInfo.State.STARTING, null);
        startMonitor();
        LOGGER.info("Started collecting from {} for project '{}'.", source.endpoint, projectName);
    }

    /**
     * Creates and starts the engine of a source. A seam for tests.
     */
    protected CollectorEngine startEngine(CollectorConfiguration configuration, RepositoryFactory output) {
        CollectorEngine engine = new CollectorEngine(output, true);
        engine.start(configuration);
        return engine;
    }

    /**
     * @return the service that writes into the repository of the analyses. A seam for tests.
     */
    protected TimeSeriesWriteService importService() {
        return ServiceDiscovery.lookup(TimeSeriesWriteService.class, repository);
    }

    /** @return the projects of the repository of the analyses. A seam for tests. */
    protected ProjectDataAccessService projects() {
        return ServiceDiscovery.lookup(ProjectDataAccessService.class, repository);
    }

    /**
     * @return what tells where the stored values of a series end. A seam for tests.
     */
    protected SeriesEnds seriesEnds() {
        return new RepositorySeriesEnds(repository);
    }

    /**
     * Chooses the name under which the series of a new source of the project are stored. The
     * first source of a project gets the plain name; every further one a name of its own, taken
     * from its address, so that systems of the same names at two sources stay two sets of series.
     */
    private String seriesNamespaceFor(String projectName, String url, String sourceType) {
        Set<String> taken = new java.util.HashSet<>();
        for (CollectionSourceRow other : sourceStore.listByProject(projectName)) {
            // sources stored before there were such names all write under the plain one
            taken.add(other.seriesNamespace == null ? ProjectSeriesOutput.MEASUREMENT : other.seriesNamespace);
        }
        if (!taken.contains(ProjectSeriesOutput.MEASUREMENT)) {
            return ProjectSeriesOutput.MEASUREMENT;
        }
        String host;
        try {
            // only an address with a scheme names one server; a list of hosts does not
            host = url.contains("://") ? java.net.URI.create(url.trim()).getHost() : null;
        } catch (IllegalArgumentException e) {
            host = null;
        }
        String base = ProjectSeriesOutput.MEASUREMENT + "-"
                + (host == null || host.isBlank() ? sourceType : host.replaceAll("[^A-Za-z0-9._-]", "-"));
        String name = base;
        for (int i = 2; taken.contains(name); i++) {
            name = base + "-" + i;
        }
        return name;
    }

    private void stopEngine(String sourceId) {
        CollectorEngine engine = engines.remove(sourceId);
        if (engine != null) {
            engine.stop();
        }
    }

    /** Reads the configuration of a source with its secret filled in, in memory only. */
    private static CollectorConfiguration parse(CollectionSourceRow source, String secret) {
        try {
            return CollectorConfigurations.fromString(CollectorConfigCompiler.withSecret(source.config, secret));
        } catch (RuntimeException e) {
            throw new IllegalStateException("The configuration of the source is not valid: " + message(e), e);
        }
    }

    private CollectionSourceRow require(String sourceId) {
        CollectionSourceRow source = sourceStore.find(sourceId);
        if (source == null) {
            throw new IllegalArgumentException("This data source does not exist anymore.");
        }
        return source;
    }

    private DataSourceInfo describe(CollectionSourceRow source) {
        DataSourceInfo.State state;
        String detail = null;
        Instant lastValue = null;
        int series = 0;

        CollectorEngine engine = engines.get(source.id);
        CollectorStatus status = engine == null ? null : engine.getStatus();
        if (status != null) {
            for (CollectorJobStatus job : status.jobs()) {
                series += job.metricCount();
                if (job.lastSampleTime() != null && (lastValue == null || job.lastSampleTime().isAfter(lastValue))) {
                    lastValue = job.lastSampleTime();
                }
            }
        }

        if (!source.enabled) {
            state = DataSourceInfo.State.PAUSED;
        } else if (startProblems.containsKey(source.id)) {
            state = DataSourceInfo.State.ERROR;
            detail = startProblems.get(source.id);
        } else if (status == null || !status.running()) {
            state = DataSourceInfo.State.STOPPED;
        } else if (!status.jobsInError().isEmpty()) {
            state = DataSourceInfo.State.ERROR;
            CollectorJobStatus failed = status.jobsInError().get(0);
            detail = failed.metricGroupId() + ": "
                    + (failed.lastError() != null ? failed.lastError() : failed.state().getPrintableAlias());
        } else if (status.jobs().stream().allMatch(job -> job.lastSampleTime() != null)) {
            state = DataSourceInfo.State.RUNNING;
        } else if (status.jobs().stream().anyMatch(job -> job.lastSampleTime() != null || job.metricCount() > 0)) {
            state = DataSourceInfo.State.WAITING;
        } else {
            state = DataSourceInfo.State.STARTING;
        }
        return new DataSourceInfo(source.id, source.projectName, source.sourceType, source.executionMode,
                source.endpoint, state, detail, lastValue, series);
    }

    //----------------------------------------------------------------------------------------------- history

    private synchronized void startMonitor() {
        if (monitor == null) {
            monitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "data-source-monitor");
                thread.setDaemon(true);
                return thread;
            });
            monitor.scheduleWithFixedDelay(this::recordStateChanges, MONITOR_INTERVAL_SECONDS,
                    MONITOR_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    private synchronized void stopMonitor() {
        if (monitor != null) {
            monitor.shutdownNow();
            monitor = null;
        }
    }

    /**
     * Writes every change of state of a running source into its history and announces every
     * change of the state of a project.
     */
    private void recordStateChanges() {
        try {
            for (String sourceId : new ArrayList<>(engines.keySet())) {
                CollectionSourceRow source = knownSources.get(sourceId);
                if (source != null) {
                    DataSourceInfo info = describe(source);
                    record(sourceId, info.state(), info.detail());
                }
            }
            knownSources.values().stream().map(source -> source.projectName).distinct().forEach(this::announce);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not record the state of the data sources.", e);
        }
    }

    /** Replaces what is known about the stored sources. */
    private void remember(List<CollectionSourceRow> stored) {
        knownSources.keySet().retainAll(stored.stream().map(source -> source.id).toList());
        stored.forEach(source -> knownSources.put(source.id, source));
    }

    /**
     * Tells the views that the data collection of a project changed, if it did. The time of the
     * newest value counts as a change only by the minute, to keep the views calm.
     */
    private void announce(String projectName) {
        Status status = statusOf(projectName);
        String fingerprint = status.state() + "|" + status.sourceCount() + "|" + status.detail() + "|"
                + (status.lastValue() == null ? "" : status.lastValue().getEpochSecond() / 60);
        if (!fingerprint.equals(announcedProjectStates.put(projectName, fingerprint))) {
            publish(new ProjectDataCollectionChangedEvent(this, projectName));
        }
    }

    private void record(String sourceId, DataSourceInfo.State state, String detail) {
        String entry = state.name() + (detail == null ? "" : ": " + detail);
        if (entry.equals(recordedStates.put(sourceId, entry))) {
            return;
        }
        quietly(() -> sourceStore.addEvent(sourceId, state.name(), detail));
    }

    //----------------------------------------------------------------------------------------------- helpers

    private static void quietly(Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException e) {
            LOGGER.debug("Cleanup step failed.", e);
        }
    }

    private static String message(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    private static void publish(EventObject event) {
        EventBus<EventObject> eventBus;
        try {
            eventBus = Lookup.lookup(EventBus.class);
        } catch (RuntimeException e) {
            return;
        }
        if (eventBus != null) {
            eventBus.publish(event);
        }
    }
}
