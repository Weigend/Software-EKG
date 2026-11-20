//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend
//       creation date: 2009
//       changed by:    $Author: adersberger $
//       change date:   $Date: 2010-02-11 01:47:26 +0100 (Mi, 16 Dez 2009) $
//       revision:      $Revision: 190 $
//       description:   Configuration.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.control.jobs;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.common.Registry;
import io.softwareecg.collector.common.SleepUtil;
import io.softwareecg.collector.control.CollectorJobStatus;
import io.softwareecg.collector.control.util.SchedulingHelper;
import io.softwareecg.collector.dt.CollectorJobConfiguration;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.memory.CollectorMemory;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.source.*;
import org.slf4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * The base class of all specific collector jobs that implements the core logic
 * to periodically retrieve metric data from configured sources and write it to the storage.
 *
 * @since 09.08.2018
 */
public abstract class CollectorJob implements Runnable, Comparable<CollectorJob>, CounterSourceListener {

    /**
     * The logger used by the CollectorJob to notify about errors and special events
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The collector config the is the setup-up base for this collector job.
     * This abstract class only knows the base config without specifics like
     * cloud support.
     * This config should keep untouched because it will used as base for periodic wildcard refresh.
     */
    private final CollectorJobConfiguration jobConfig;

    /**
     * The JobManager handler that controls the execution of delayed collector jobs
     * and manages in this context the overall execution of all running collectors.
     */
    private final JobManager jobManager;

    /**
     * The job state that holds the current job state of this collector
     * and provided it to other components which need to know it.
     */
    private final CollectorJobState collectorJobState;

    /**
     * The repository factory used to instantiate new repository
     * used to log the fetched metrics
     */
    private final RepositoryFactory repositoryFactory;

    /**
     * a flag that indicates that the storage has never been created or
     * a new storage is requested as result of changed metrics
     */
    private boolean createNewStorage = true;

    /**
     * Set when the job is asked to end. Interrupting the thread alone is not enough to tell
     * a requested stop from a failure: sources and helpers swallow or wrap the interruption.
     */
    private volatile boolean stopRequested;

    /**
     * When the last value set was written, {@code null} before the first one.
     */
    private volatile Instant lastSampleTime;

    /**
     * The number of concrete metrics the job currently collects (after wildcard expansion).
     */
    private volatile int metricCount;

    /**
     * Short description of the error that made the job fail, {@code null} while it is healthy.
     */
    private volatile String lastError;

    /**
     * The time of the newest value set collected from a source with a history of its own
     * ({@link CounterHistorySupport}); the next cycle continues after it.
     */
    private Date historyPosition;

    /**
     * How often writing to the storage failed in a row; the pause before the next try grows with it.
     */
    private int storageFailures;

    /**
     * The longest pause between two tries to write to a failing storage.
     */
    private static final long MAX_STORAGE_RETRY_PAUSE = 5 * 60_000;

    /**
     * The longest pause between two tries to resolve the metrics at a source that does not answer.
     */
    private static final long MAX_SOURCE_RETRY_PAUSE = 60_000;


    //==================================================================================================================
    // protected constructors that is designed to used by deriving concrete CollectorJob classes
    //==================================================================================================================

    /**
     * Create the base collector job that
     * stores the common job properties and will exec the main logic.
     *
     * @param collectorJobConfig a CollectorJobConfiguration that will use to retrieve the setup for the job
     * @param jobManager the job manager that manages all collector jobs running at time
     */
    protected CollectorJob(CollectorJobConfiguration collectorJobConfig, JobManager jobManager) {
        this(collectorJobConfig, jobManager, Registry.getDefault().lookup(RepositoryFactory.class));
    }

    /**
     * Create the base collector job with an explicit output.
     *
     * @param collectorJobConfig a CollectorJobConfiguration that will use to retrieve the setup for the job
     * @param jobManager the job manager that manages all collector jobs running at time
     * @param repositoryFactory creates the storage the fetched metrics are written to. Lets a host
     *                          application supply its own output instead of the one configured
     *                          in the collector properties.
     */
    protected CollectorJob(CollectorJobConfiguration collectorJobConfig, JobManager jobManager,
                           RepositoryFactory repositoryFactory) {
        this.jobConfig = collectorJobConfig;
        this.jobManager = jobManager;
        this.repositoryFactory = repositoryFactory;
        this.collectorJobState = new CollectorJobState();
        this.metricCount = collectorJobConfig.getMetricDefinitions() != null
                ? collectorJobConfig.getMetricDefinitions().length : 0;
    }

    //==================================================================================================================
    // main method that computes the collector logic and is the only API of collector jobs
    //==================================================================================================================

    /**
     * Executes the collector logic that will expand metrics > fetch metrics > write it to storage and recheck metrics
     * in an continuous cycle. This method will only end in case of fatal errors or interruption from outside.
     */
    public void run() {

        StaticTimeSeriesRepository repository = null;

        try {
            // wait for own jmx counter to get alive
            Thread.sleep(1000);

            // set the name of the owner thread of this collector
            // for better analytic convenience
            setupOwnThreadName();

            // if this CollectorJob belongs to a delayed counter type, stop it until
            // all other jobs has passed the first cycle
            jobManager.delayExecutionOfCalculatedCounters(this);

            String[] lastMetricLabels = null;

            while (isActive()) {

                // resolve and instance counter source based on the configured type and expand wildcards if supported
                CounterSource source = CounterSourceFactory.getPrototypeInstance(jobConfig.getCounterGroupType());
                LOGGER.info("Retrieve new CounterSource '{}' to fetch metric data.",
                        source.getId() + "_(" + source + ")");

                collectorJobState.switchToWildcardExpansionState();
                CollectorJobConfiguration confWithExpMetrics = expandWildcardMetrics(jobConfig, source);
                collectorJobState.switchToWaitForDataState();

                // every time the metrics change, we request a new storage block that supports the changed
                // metric structure. Comparing their number is not enough: one series may have gone while
                // another one came, and the values would be written under the wrong names.
                String[] metricLabels = counter2Names(confWithExpMetrics.getMetricDefinitions());
                createNewStorage = !Arrays.equals(lastMetricLabels, metricLabels);
                lastMetricLabels = metricLabels;
                metricCount = metricLabels.length;

                // always create a storage if it was not created before and or a new storage is requested
                if (repository == null || createNewStorage ) {

                    // ensure persistence of IN-memory data if we already have an storage in use
                    if (repository != null) {
                        repository.flush();
                        repository.cleanUp();
                    }

                    // create a new storage that is configured to the current amount of metrics
                    repository = initializeNewStorage(confWithExpMetrics);

                    // the new storage says where to resume: series that are new to it start earlier
                    // than those it already has values of
                    historyPosition = null;
                }

                // reset states
                createNewStorage = false;
                Date lastRefresh = new Date();

                // loop unit we need a new file (new counter or log rotation)
                while (!createNewStorage && isActive()) {

                    // write the counters to the configured storage
                    if(!writeCounter(confWithExpMetrics, repository, source)) {
                        // exit the loop if no data could written this can be the case than wildcard
                        // expansion didn't work or isn't up to date
                        LOGGER.warn("Do not write any data. Exit the fetching loop and try to re-expand counters.");
                        break;
                    }

                    // signal this job passed the first cycle with new storage, delayed/depending jobs can start
                    jobManager.firstJobCycleFinished(this);

                    // await the conditions before start the next fetch > write > wait > check cycle
                    waitForNextFetchCycle(confWithExpMetrics);

                    //check if we need to refresh the Counters Wildcards
                    createNewStorage = wildcardRefreshRequired(lastRefresh);
                }
            }

        } catch (Exception e) {
            if (stopRequested) {
                // the interruption that ends the job surfaces here, often wrapped by a helper
                LOGGER.debug("Collector job interrupted by the stop request.", e);
            } else {
                LOGGER.error("Exception during job execution.", e);
                lastError = describe(e);
                collectorJobState.switchToFailedState();
                jobManager.jobStateChanged(this);

                while (isActive()) {
                    try {
                        LOGGER.error("Collector waits for explicit close command after exception");
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        break;
                    }
                }
            }
        } finally {
            closeStorage(repository);
            if (collectorJobState.getState() != CollectorJobState.CollectorJobStateEnum.FAILED) {
                collectorJobState.switchToStoppedState();
            }
        }

        LOGGER.info("Collector Job stopped.");
    }

    /**
     * Asks the job to end. The caller interrupts the job thread in addition, to wake it from
     * waiting for the next fetch cycle.
     */
    public void requestStop() {
        stopRequested = true;
    }

    /**
     * @return a snapshot of what this job collects and how it is doing
     */
    public CollectorJobStatus getStatus() {
        return new CollectorJobStatus(
                jobConfig.getNodeGroupId(),
                jobConfig.getNodeName(),
                jobConfig.getNodeAddress(),
                jobConfig.getProcessLabel(),
                jobConfig.getMetricGroupId(),
                jobConfig.getCounterGroupType(),
                collectorJobState.getState(),
                metricCount,
                lastSampleTime,
                lastError);
    }

    private boolean isActive() {
        return !stopRequested && !Thread.currentThread().isInterrupted();
    }

    /**
     * Writes out what the storage still buffers. Without this a stop would lose the values
     * collected since the last flush.
     */
    private void closeStorage(StaticTimeSeriesRepository repository) {
        if (repository == null) {
            return;
        }
        try {
            repository.flush();
            repository.cleanUp();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not flush the storage of the stopped collector job.", e);
        }
    }

    private static String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + (root.getMessage() != null ? ": " + root.getMessage() : "");
    }

    /**
     * Returns the state of this job
     *
     * @return the job state
     */
    public CollectorJobState getCollectorJobState() {
        return collectorJobState;
    }

    @Override
    public int compareTo(CollectorJob otherJob) {
        return jobConfig.getNodeAddress().compareTo(otherJob.getJobConfig().getNodeAddress());
    }

    @Override
    public void counterChanged() {
        createNewStorage = true;
    }

    //==================================================================================================================
    // internal helper that support the main logic
    //==================================================================================================================

    /**
     * Returns the job configuration this collector job
     * is based on.
     *
     * @return the job config belong to this collector job
     */
    protected CollectorJobConfiguration getJobConfig() {
        return jobConfig;
    }

    /**
     * Returns a boolean flag if is time to refresh the
     * metric wildcards or not.
     *
     * @param lastRefresh the datetime of the last wildcard refresh.
     * @return true to signal it time to refresh the wildcard metrics, false to continue with the current ones.
     */
    private boolean wildcardRefreshRequired(Date lastRefresh) {
        return jobConfig.isRefreshWildcards()
                && (new Date().getTime() - lastRefresh.getTime() > jobConfig.getWildcardExpandRefreshInterval());
    }

    /**
     * Blocks the execution of the collector logic until the time to start the next
     * fetch cycle is reached. This will controlled by the metricFetchInterval or
     * the metricFetchAt parameter of the config.
     *
     * @param currentJobConfig the job config used to retrieve the fetch interval
     *
     * @throws ParseException thrown if an error occurs at parsing the fetch time (config parameter 'metricFetchAt')
     * @throws InterruptedException thrown if the collector get interrupted during wait for next cycle
     */
    private void waitForNextFetchCycle(CollectorJobConfiguration currentJobConfig)
            throws ParseException, InterruptedException {

        if (jobConfig.getMetricFetchAt() != null) {
            Date nextRun = SchedulingHelper.nextDate(currentJobConfig.getMetricFetchAt());
            LOGGER.info("Next scheduled date: " + new SimpleDateFormat().format(nextRun) + ". Sleeping.");
            SchedulingHelper.waitUntil(nextRun);
        }

        Thread.sleep(jobConfig.getMetricFetchInterval());
    }

    /**
     * Create a new instance of StaticTimeSeriesRepository that will bind to the properties of the given
     * job configuration an can use to write out the according metric data.
     *
     * @param currentJobConfig the job config used to initialize/preconfigure the storage
     * @return the new create storage instance
     */
    protected StaticTimeSeriesRepository initializeNewStorage(CollectorJobConfiguration currentJobConfig) {

        StaticTimeSeriesRepository repository = repositoryFactory.createStaticRepository(currentJobConfig);

        // create metrics i.e. the csv header or the metrics in the db
        String[] metricLabels = counter2Names(currentJobConfig.getMetricDefinitions());
        repository.createMeasurement(currentJobConfig.getTimeSeriesHeader(), metricLabels);

        return repository;
    }

    /**
     * Setup the name of the owner thread based on the
     * underlying job configuration.
     */
    protected void setupOwnThreadName() {
        Thread.currentThread().setName(jobConfig.getNodeAddress() + ":" + jobConfig.getProcessName()
                + ":" + jobConfig.getMetricGroupId());
    }

    /**
     * Collects and returns the labels of all metric definitions
     * in the given array.
     *
     * @param metricDefinitions the metricDefinitions.
     * @return a string array of labels.
     */
    private String[] counter2Names(MetricDefinition[] metricDefinitions) {
        List<String> result = new ArrayList<>();
        for (MetricDefinition metricDefinition : metricDefinitions) {
            result.add(metricDefinition.getLabel());
        }
        return result.toArray(new String[metricDefinitions.length]);
    }

    /**
     * Write one set of metric values to the storage.
     *
     * @param jobConfiguration the config used to get metric definitions and other information to retrieve metric values
     * @param repository the repository the metric values will written to (for example to CSV or Solr repository)
     * @param source the adapter that fetch the metrics from the source
     * @return boolean flag if the write process was successful or if it skipped because the data wasn't complete
     *
     */
    private boolean writeCounter(CollectorJobConfiguration jobConfiguration, StaticTimeSeriesRepository repository, CounterSource source)
            throws InterruptedException {

        if (source instanceof CounterHistorySupport historySource) {
            return writeCounterHistory(jobConfiguration, repository, historySource);
        }

        // request counter
        CounterSeries values = source.queryCounter(jobConfiguration);

        if (values.getValues().isEmpty()
                || jobConfiguration.getMetricDefinitions().length != values.getValues().size()) {
            return false;
        }

        // store counter values. A storage that cannot take them does not end the job: it shows
        // the problem in its state and keeps collecting, with growing pauses.
        try {
            storage(() -> {
                repository.addValueSetToMeasurement(values.getDate(), values.getValues().toArray(new Double[0]));
                return null;
            });
        } catch (StorageFailure failure) {
            storageFailed(failure.getCause());
            return true;
        }
        storageFailures = 0;
        if (collectorJobState.getState() == CollectorJobState.CollectorJobStateEnum.STORAGE_ERROR) {
            lastError = null;
            collectorJobState.switchToWaitForDataState();
        }
        lastSampleTime = values.getDate() != null ? values.getDate().toInstant() : Instant.now();

        // record values for calculated counters
        CollectorMemory memory = CollectorMemory.getMemory(jobConfiguration.getNodeAddress());
        memory.recordCounter(jobConfiguration.getMetricDefinitions(), values.getValues());

        updateCollectorState(values.getValues());

        return true;
    }

    /**
     * Writes all value sets the source holds since the last one collected. This covers the
     * first fill, the regular update and catching up after a pause alike.
     * <p>
     * If the storage cannot take the values, nothing is lost and nothing piles up: the job
     * shows the problem in its state, waits with growing pauses and then asks the storage where
     * its content ends and collects again from there - the source still has the values.
     *
     * @return {@code false} if the source could not be queried or delivered incomplete value sets
     */
    private boolean writeCounterHistory(CollectorJobConfiguration jobConfiguration,
                                        StaticTimeSeriesRepository repository, CounterHistorySupport source)
            throws InterruptedException {

        final int expectedValues = jobConfiguration.getMetricDefinitions().length;
        final CounterSeries[] newest = new CounterSeries[1];
        final boolean[] complete = {true};
        boolean queried;

        try {
            if (historyPosition == null) {
                // a storage that already holds values of this measurement tells where to resume
                historyPosition = storage(repository::getLastCaptureTime);
            }

            queried = source.queryCounterSince(jobConfiguration, historyPosition, values -> {
                if (values.getValues().size() != expectedValues) {
                    complete[0] = false;
                    return;
                }
                storage(() -> {
                    repository.addValueSetToMeasurement(values.getDate(), values.getValues().toArray(new Double[0]));
                    return null;
                });
                historyPosition = values.getDate();
                newest[0] = values;
            });

            if (queried && complete[0] && newest[0] != null) {
                // what was collected in this cycle is complete: let the storage write it out
                storage(() -> {
                    repository.flush();
                    return null;
                });
            }
        } catch (StorageFailure failure) {
            storageFailed(failure.getCause());
            return true;   // stay in the fetch cycle: the metrics are fine, only the storage is not
        }

        if (!queried || !complete[0]) {
            return false;
        }

        storageFailures = 0;
        if (newest[0] != null) {
            lastSampleTime = newest[0].getDate().toInstant();

            // record values for calculated counters; the memory cannot hold missing values
            if (!newest[0].getValues().contains(null)) {
                CollectorMemory memory = CollectorMemory.getMemory(jobConfiguration.getNodeAddress());
                memory.recordCounter(jobConfiguration.getMetricDefinitions(), newest[0].getValues());
            }
            lastError = null;
            // A source with a history delivers real values or none at all, so any value counts -
            // also a zero or a negative one, which the polling sources use to say "nothing read".
            boolean anyValue = newest[0].getValues().stream().anyMatch(java.util.Objects::nonNull);
            if (anyValue && !collectorJobState.isInRunOkState()) {
                collectorJobState.switchToRunOkState();
                jobManager.jobStateChanged(this);
            } else if (!anyValue && !collectorJobState.isInNoDataErrorState()) {
                collectorJobState.switchToNoDataErrorState();
                jobManager.jobStateChanged(this);
            }
        }
        if (collectorJobState.getState() == CollectorJobState.CollectorJobStateEnum.STORAGE_ERROR) {
            // writing works again, even if this cycle had nothing new
            lastError = null;
            collectorJobState.switchToRunOkState();
            jobManager.jobStateChanged(this);
        }
        return true;
    }

    /**
     * Runs a call on the storage and marks its failure as such, so that it is not mistaken
     * for a failure of the source, through whose code the call stack leads.
     */
    private static <T> T storage(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            throw new StorageFailure(e);
        }
    }

    private void storageFailed(Throwable cause) throws InterruptedException {
        storageFailures++;
        // forget the position: values handed to the storage in this cycle may not have been
        // written. The storage is asked again where its content really ends.
        historyPosition = null;
        lastError = describe(cause);
        if (collectorJobState.getState() != CollectorJobState.CollectorJobStateEnum.STORAGE_ERROR) {
            LOGGER.warn("The collected values cannot be written, the job keeps retrying: {}", lastError);
            collectorJobState.switchToStorageErrorState();
            jobManager.jobStateChanged(this);
        }

        long interval = Math.max(1_000, jobConfig.getMetricFetchInterval());
        long pause = Math.min(MAX_STORAGE_RETRY_PAUSE, interval << Math.min(storageFailures - 1, 10));
        Thread.sleep(pause);
    }

    /**
     * Marks an exception as coming from the storage.
     */
    private static final class StorageFailure extends RuntimeException {
        StorageFailure(RuntimeException cause) {
            super(cause);
        }
    }

    /**
     * Helper method to update the job status and notify the JobManager about it
     * depending on the the availability of metric data.
     *
     * @param values the metric values fetched before an will checked by this helper
     */
    private void updateCollectorState(List<Double> values) {

        if (valuesAreValid(values) && !collectorJobState.isInRunOkState()) {
            collectorJobState.switchToRunOkState();
            jobManager.jobStateChanged(this);

        // also make some output if the connection was lost
        } else if (!valuesAreValid(values) && !collectorJobState.isInNoDataErrorState()) {
            collectorJobState.switchToNoDataErrorState();
            jobManager.jobStateChanged(this);
        }
    }

    /**
     * Helper to check if the result values are valid;.
     *
     * @param values values.
     * @return true if the list contains valid values.
     */
    private boolean valuesAreValid(List<Double> values) {
        for (Double next : values) {
            // At least one value is valid. A source that could not read a value says so with a
            // negative number; a zero is a value like any other - an idle process uses no CPU.
            if (next != null && next >= 0.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method that will replace the given job configuration with a new one that
     * has expanded metric definitions if the counter source supports wildcards.
     * This helper will also register this job as CounterSource lister if the counter
     * source updates metrics by itself.
     *
     * @param config the default configuration with wildcard metrics
     * @param source the counter source that provides the metric data
     *
     * @return a new config with expanded metric definitions if wildcard
     *         expansion was proceeded otherwise the given config will returned
     */
    private CollectorJobConfiguration expandWildcardMetrics(CollectorJobConfiguration config, CounterSource source)
            throws InterruptedException {

        if (!(source instanceof CounterWildcardSupport)) {
            return config;
        }

        ExpandWildcardResponse wildcardResponse = ((CounterWildcardSupport)source).replaceWildcardCounter(config);

        // A source that cannot be reached is no "metric not found": the job says what is wrong and
        // keeps trying, with growing pauses, until the source answers.
        int attempts = 0;
        while (wildcardResponse.getResultType() == ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE && isActive()) {
            lastError = wildcardResponse.getMessage() != null ? wildcardResponse.getMessage()
                    : "The source at " + config.getNodeAddress() + " is not available.";
            if (collectorJobState.getState() != CollectorJobState.CollectorJobStateEnum.ENV_ERROR) {
                LOGGER.warn("The metrics cannot be resolved, the job keeps retrying: {}", lastError);
                collectorJobState.switchToEnvErrorErrorState();
                jobManager.jobStateChanged(this);
            }
            long interval = Math.max(1_000, config.getMetricFetchInterval());
            Thread.sleep(Math.min(MAX_SOURCE_RETRY_PAUSE, interval << Math.min(attempts++, 10)));
            wildcardResponse = ((CounterWildcardSupport) source).replaceWildcardCounter(config);
        }
        if (attempts > 0 && isActive()) {
            LOGGER.info("The source at {} answers again.", config.getNodeAddress());
            lastError = null;
        }

        if (wildcardResponse.getResultType() == ExpandWildcardResultType.METRIC_NOT_FOUND) {

            if (config.isRefreshWildcards()) {
                LOGGER.warn("Unable to find concrete metrics for the wildcards expressions. " +
                        "Wait for the next (specified) refresh cycle to check it again.");

                SleepUtil.sleep(config.getWildcardExpandRefreshInterval());

            } else {
                LOGGER.warn("Unable to find concrete metrics for the wildcards expression. It will check in 10min again.");
                SleepUtil.sleep(600_000);
            }

        } else if (wildcardResponse.getResultType() == ExpandWildcardResultType.METRIC_DEFINITION_INVALID) {
            LOGGER.warn("The configuration of the counter source seems to be invalid and can't resolved automatically.");
            throw new IllegalArgumentException("Invalid metric wildcard definitions given.");
        }

        CollectorJobConfiguration newConfiguration = jobConfig.copyWithMetrics(wildcardResponse.getExpandedDefinitions());

        LOGGER.info("Replacing {} generic counter ({}) with {} concrete counter.",
                config.getMetricDefinitions().length,
                Arrays.toString(config.getMetricDefinitions()),
                newConfiguration.getMetricDefinitions().length);

        ((CounterWildcardSupport) source).addCounterListener(this);

        return newConfiguration;
    }
}
