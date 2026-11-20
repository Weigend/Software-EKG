package io.softwareecg.collector.control.jobs;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.common.TextProgress;
import io.softwareecg.collector.common.TextTable;
import io.softwareecg.collector.dt.CollectorJobConfiguration;
import io.softwareecg.collector.source.CounterSource;
import io.softwareecg.collector.source.CounterSourceFactory;
import io.softwareecg.collector.source.Delayed;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * JobManager that manages the execution of calculate metric collectors
 * depending on the overall job state and updates the command line user
 * interface with periodically status updates about all job states.
 *
 * @since 10.08.2018
 */
public class JobManager {

    private static final Logger LOGGER = LoggerFactory.create();

    private final List<CollectorJob> standardCollectorJobs = new ArrayList<>();

    private final List<CollectorJob> delayedCollectorJobs = new ArrayList<>();

    private final Set<CollectorJob> collectorJobsWithError = new HashSet<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private CountDownLatch regularMetricJobOnFirstCycle;


    //===============================================================================================================
    // job manager API
    //===============================================================================================================

    /**
     * Add a collector job to this manager so it state displayed
     * on the user interface and it's execution can managed in context of all jobs.
     *
     * @param job a CollectorJob instance that should be managed by this manager
     */
    public void addCollectorJob(CollectorJob job) {
        try{
            writeLock.lock();
            if (jobCollectsCalculatedMetrics(job)) {
                delayedCollectorJobs.add(job);
                Collections.sort(delayedCollectorJobs);

            } else {
                standardCollectorJobs.add(job);
                Collections.sort(standardCollectorJobs);
            }

            if (job.getCollectorJobState().isInErrorState()) {
                collectorJobsWithError.add(job);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Notifies this job manager that a job state has change.
     * In this case the manager will update it's internal states
     * update the UI.
     *
     * @param job the job that state has changed
     */
    /**
     * @return all jobs managed here, regular ones first, as a snapshot
     */
    public List<CollectorJob> getJobs() {
        try {
            readLock.lock();
            List<CollectorJob> jobs = new ArrayList<>(standardCollectorJobs);
            jobs.addAll(delayedCollectorJobs);
            return jobs;
        } finally {
            readLock.unlock();
        }
    }

    public void jobStateChanged(CollectorJob job) {
        try{
            writeLock.lock();

            if (job.getCollectorJobState().isInErrorState()) {
                collectorJobsWithError.add(job);
            } else {
                collectorJobsWithError.remove(job);
            }

        } finally {
            writeLock.unlock();
        }

        logJobStateTable();
        startLogProgressIfNoError();
    }

    /**
     * Notifies this job manager that one of the managed jobs has
     * passed the first metric-fetch > write to storage cycle. Depending
     * on the overall state this can trigger the execution of calculated
     * metric collectors.
     */
    public void firstJobCycleFinished(CollectorJob job) {
        if (regularMetricJobOnFirstCycle == null) {
            regularMetricJobOnFirstCycle = new CountDownLatch(standardCollectorJobs.size());
        }

        if (!job.getCollectorJobState().isInErrorState()) {
            regularMetricJobOnFirstCycle.countDown();
        }
    }

    /**
     * Delay execution of calculated counters.
     *
     * @throws InterruptedException if the latch is interrupted.
     */
    public void delayExecutionOfCalculatedCounters(CollectorJob job) throws InterruptedException {

        if (regularMetricJobOnFirstCycle == null) {
            regularMetricJobOnFirstCycle = new CountDownLatch(standardCollectorJobs.size());
        }

        if (delayedCollectorJobs.contains(job)) {
            regularMetricJobOnFirstCycle.await();
        }
    }

    //===============================================================================================================
    // private helper to print the state to user interface
    //===============================================================================================================


    private boolean jobCollectsCalculatedMetrics(CollectorJob job) {
        CounterSource source = CounterSourceFactory.getSingletonInstance(job.getJobConfig().getCounterGroupType());
        return source.getClass().isAnnotationPresent(Delayed.class);
    }

    private void startLogProgressIfNoError() {
        if (collectorJobsWithError.isEmpty() && CollectorProperties.isUseProgressSign()) {
            TextProgress progress = new TextProgress();
            progress.start();
        }
    }

    private void logJobStateTable() {
        List<String> columnLabels = Arrays.asList("Source", "Process", "MetricDefinition", "State");
        List<List<String>> data = new ArrayList<>();

        try {
            readLock.lock();
            for (CollectorJob job : standardCollectorJobs) {
                data.add(createTableRow(job));
            }

            for (CollectorJob job : delayedCollectorJobs) {
                data.add(createTableRow(job));
            }

            String stateTable = "\n" + new TextTable(null, columnLabels, data);
            LOGGER.info(stateTable);

        } finally {
            readLock.unlock();
        }
    }

    private List<String> createTableRow(CollectorJob job) {
        CollectorJobConfiguration jobConfig = job.getJobConfig();
        return Arrays.asList(jobConfig.getNodeName(), jobConfig.getProcessLabel(),
                jobConfig.getMetricGroupId(), job.getCollectorJobState().getPrintableState());
    }
}
