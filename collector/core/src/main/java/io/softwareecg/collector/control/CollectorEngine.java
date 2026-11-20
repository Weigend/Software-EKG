package io.softwareecg.collector.control;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.control.jobs.ClassicCollectorJob;
import io.softwareecg.collector.control.jobs.CloudCollectorJob;
import io.softwareecg.collector.control.jobs.CollectorJob;
import io.softwareecg.collector.control.jobs.JobManager;
import io.softwareecg.collector.control.jobs.SidecarCollectorJob;
import io.softwareecg.collector.dt.ClassicCollectorJobConfiguration;
import io.softwareecg.collector.dt.CloudCollectorJobConfiguration;
import io.softwareecg.collector.dt.SidecarCollectorJobConfiguration;
import io.softwareecg.collector.repository.RepositoryFactory;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the collector jobs of a configuration and can be started, stopped, started again and
 * asked for its status. One job - and one thread - is created per combination of node, process
 * and metric group, exactly as the configuration multiplies them out.
 * <p>
 * The engine is the embeddable form of the collector: a host application creates an instance,
 * optionally with its own output, and controls it through this class. The stand-alone collector
 * ({@link CollectorCore}) is a thin shell around one engine.
 */
public final class CollectorEngine {

    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * How long {@link #stop()} waits for the jobs to end. A job that is blocked inside a source
     * (a hanging remote call) may need longer; it still ends as soon as that call returns.
     */
    private static final Duration DEFAULT_STOP_TIMEOUT = Duration.ofSeconds(15);

    private final RepositoryFactory repositoryFactory;

    private final boolean daemonThreads;

    private final Object lifecycleLock = new Object();

    private volatile List<Thread> jobThreads = List.of();

    private volatile JobManager jobManager;

    private volatile Instant startedAt;

    /**
     * Creates an engine that writes to the output configured in the collector properties.
     */
    public CollectorEngine() {
        this(null, false);
    }

    /**
     * Creates an engine with its own output.
     *
     * @param repositoryFactory creates the storage each job writes to; {@code null} uses the
     *                          output configured in the collector properties
     * @param daemonThreads     {@code true} if the job threads must not keep the JVM alive,
     *                          as wanted when the engine lives inside another application
     */
    public CollectorEngine(RepositoryFactory repositoryFactory, boolean daemonThreads) {
        this.repositoryFactory = repositoryFactory;
        this.daemonThreads = daemonThreads;
    }

    /**
     * Starts one job per job configuration.
     *
     * @param config the collector configuration to run
     * @throws IllegalStateException if the engine is already running
     */
    public void start(CollectorConfiguration config) {
        synchronized (lifecycleLock) {
            if (isRunning()) {
                throw new IllegalStateException("The collector engine is already running. Stop it first.");
            }

            JobManager manager = new JobManager();
            List<Thread> threads = new ArrayList<>();

            for (ClassicCollectorJobConfiguration classicConfig : config.getClassicCollectorJobConfiguration()) {
                register(repositoryFactory == null
                        ? new ClassicCollectorJob(classicConfig, manager)
                        : new ClassicCollectorJob(classicConfig, manager, repositoryFactory), manager, threads);
            }

            for (CloudCollectorJobConfiguration cloudConfig : config.getCloudCollectorJobConfiguration()) {
                register(repositoryFactory == null
                        ? new CloudCollectorJob(cloudConfig, manager)
                        : new CloudCollectorJob(cloudConfig, manager, repositoryFactory), manager, threads);
            }

            for (SidecarCollectorJobConfiguration sidecarConfig : config.getSidecarCollectorConfiguration()) {
                register(repositoryFactory == null
                        ? new SidecarCollectorJob(sidecarConfig, manager)
                        : new SidecarCollectorJob(sidecarConfig, manager, repositoryFactory), manager, threads);
            }

            jobManager = manager;
            jobThreads = List.copyOf(threads);
            startedAt = Instant.now();

            threads.forEach(Thread::start);
            LOGGER.info("Collector engine started with {} job(s).", threads.size());
        }
    }

    private void register(CollectorJob job, JobManager manager, List<Thread> threads) {
        manager.addCollectorJob(job);
        Thread thread = new Thread(job);
        thread.setDaemon(daemonThreads);
        threads.add(thread);
    }

    /**
     * Stops all jobs and waits for them to end.
     *
     * @return {@code true} if every job ended within the default timeout
     */
    public boolean stop() {
        return stop(DEFAULT_STOP_TIMEOUT);
    }

    /**
     * Stops all jobs and waits up to the given time for them to end. Each job flushes its
     * storage before it ends.
     *
     * @param timeout how long to wait for the jobs altogether
     * @return {@code true} if every job ended in time
     */
    public boolean stop(Duration timeout) {
        synchronized (lifecycleLock) {
            JobManager manager = jobManager;
            if (manager != null) {
                manager.getJobs().forEach(CollectorJob::requestStop);
            }
            List<Thread> threads = jobThreads;
            threads.forEach(Thread::interrupt);

            long deadline = System.nanoTime() + timeout.toNanos();
            boolean interrupted = false;
            for (Thread thread : threads) {
                long remainingMillis = Math.max(1, (deadline - System.nanoTime()) / 1_000_000);
                try {
                    thread.join(remainingMillis);
                } catch (InterruptedException e) {
                    interrupted = true;
                    break;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }

            boolean stopped = !isRunning();
            if (stopped) {
                LOGGER.info("Collector engine stopped.");
            } else {
                LOGGER.warn("Collector engine: not all jobs ended within {} ms.", timeout.toMillis());
            }
            return stopped;
        }
    }

    /**
     * @return {@code true} while at least one job is still alive
     */
    public boolean isRunning() {
        for (Thread thread : jobThreads) {
            if (thread.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Blocks until all jobs of the current run have ended.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void awaitTermination() throws InterruptedException {
        for (Thread thread : jobThreads) {
            thread.join();
        }
    }

    /**
     * @return whether the engine runs and the state of every job of the current (or last) run
     */
    public CollectorStatus getStatus() {
        JobManager manager = jobManager;
        List<CollectorJobStatus> jobs = manager == null
                ? List.of()
                : manager.getJobs().stream().map(CollectorJob::getStatus).toList();
        return new CollectorStatus(isRunning(), startedAt, jobs);
    }
}
