package io.softwareecg.collector.source.prometheus;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.control.CollectorEngine;
import io.softwareecg.collector.control.CollectorJobStatus;
import io.softwareecg.collector.control.CollectorStatus;
import io.softwareecg.collector.control.jobs.CollectorJobState.CollectorJobStateEnum;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The Prometheus source inside a real collector job: configured as XML, run by the engine,
 * written to an output.
 */
public class PrometheusCollectorJobTest {

    private static final String LOAD = "node_load1";

    private FakePrometheus prometheus;

    private CollectorEngine engine;

    private final Output output = new Output();

    @Before
    public void startPrometheus() throws Exception {
        prometheus = new FakePrometheus().expression(LOAD, List.of(
                Map.of("__name__", "node_load1", "job", "node", "instance", "server-01"),
                Map.of("__name__", "node_load1", "job", "node", "instance", "server-02")));
    }

    @After
    public void stop() {
        if (engine != null) {
            engine.stop();
        }
        prometheus.close();
    }

    @Test
    public void jobFillsTheHistoryAndThenKeepsUpdating() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("lookback=2m")));

        awaitRows(120);   // two minutes at a one second step

        CollectorStatus status = engine.getStatus();
        assertEquals(1, status.jobs().size());
        CollectorJobStatus job = status.jobs().get(0);
        assertEquals("prometheus", job.sourceType());
        assertEquals(CollectorJobStateEnum.RUN_OK, job.state());
        assertEquals(2, job.metricCount());

        assertEquals(List.of("node_load1{instance=\"server-01\",job=\"node\"}",
                             "node_load1{instance=\"server-02\",job=\"node\"}"), output.metricLabels);
        assertEquals("prometheus-local", output.header.getHostName());

        // gap-free and strictly ascending on the one second grid
        List<Date> times = new ArrayList<>(output.times);
        for (int i = 1; i < times.size(); i++) {
            assertEquals("row " + i, 1_000, times.get(i).getTime() - times.get(i - 1).getTime());
        }

        int before = output.times.size();
        awaitRows(before + 2);   // the regular cycle adds the new points
    }

    @Test
    public void jobContinuesWhereTheOutputAlreadyHasValues() throws Exception {
        long resumeAt = (System.currentTimeMillis() / 1000) * 1000 - 30_000;
        output.lastCaptureTime = new Date(resumeAt);

        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("lookback=10m")));

        awaitRows(30);

        assertEquals("continues right after the newest stored value, not at the lookback",
                new Date(resumeAt + 1_000), output.times.get(0));
    }

    @Test
    public void failingStorageIsVisibleAndNothingIsLostOrStoredTwice() throws Exception {
        BufferingOutput storage = new BufferingOutput();
        engine = new CollectorEngine(storage, true);
        engine.start(CollectorConfigurations.fromFile(configuration("lookback=30s")));
        awaitStored(storage, 30);

        storage.failing = true;
        awaitState(CollectorJobStateEnum.STORAGE_ERROR);
        CollectorJobStatus failed = engine.getStatus().jobs().get(0);
        assertTrue(failed.isInError());
        assertTrue(failed.lastError(), failed.lastError().contains("repository down"));
        assertTrue("the job keeps running and retries", engine.isRunning());
        int storedBeforeRecovery = storage.stored.size();
        Thread.sleep(2_500);
        assertEquals("nothing is stored while the storage fails", storedBeforeRecovery, storage.stored.size());

        storage.failing = false;
        awaitState(CollectorJobStateEnum.RUN_OK);
        awaitStored(storage, storedBeforeRecovery + 5);
        assertEquals(null, engine.getStatus().jobs().get(0).lastError());

        // across the outage: every second exactly once
        List<Date> times = new ArrayList<>(storage.stored);
        for (int i = 1; i < times.size(); i++) {
            assertEquals("row " + i, 1_000, times.get(i).getTime() - times.get(i - 1).getTime());
        }
        assertTrue("the storage never held more than one cycle's worth of rejected values", storage.largestBuffer < 200);
    }

    @Test
    public void aServerThatDoesNotAnswerIsShownAsSuchUntilItDoes() throws Exception {
        prometheus.statusCode = 503;
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("lookback=5s")));

        awaitState(CollectorJobStateEnum.ENV_ERROR);
        CollectorJobStatus unreachable = engine.getStatus().jobs().get(0);
        assertTrue(unreachable.isInError());
        assertTrue("the job says what is wrong, not that it is still starting: " + unreachable.lastError(),
                unreachable.lastError().contains("did not answer the query"));
        assertTrue(output.times.isEmpty());

        prometheus.statusCode = 200;
        awaitState(CollectorJobStateEnum.RUN_OK);
        awaitRows(5);
        assertEquals(null, engine.getStatus().jobs().get(0).lastError());
    }

    @Test
    public void valuesOfZeroAreDataLikeAnyOther() throws Exception {
        prometheus.fixedValue = 0.0;
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("lookback=5s")));

        awaitRows(5);
        awaitState(CollectorJobStateEnum.RUN_OK);
        assertFalse(engine.getStatus().jobs().get(0).isInError());

        prometheus.fixedValue = -3.5;
        int before = output.times.size();
        awaitRows(before + 2);
        assertEquals(CollectorJobStateEnum.RUN_OK, engine.getStatus().jobs().get(0).state());
    }

    @Test
    public void aSeriesReplacedByAnotherGetsAStorageOfItsOwnAndAsksWhereToResume() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("lookback=5s", 3_000)));
        awaitRows(5);
        assertEquals(1, output.storages.get());

        // same number of series, but server-02 went and server-03 came
        prometheus.expression(LOAD, List.of(
                Map.of("__name__", "node_load1", "job", "node", "instance", "server-01"),
                Map.of("__name__", "node_load1", "job", "node", "instance", "server-03")));

        long deadline = System.currentTimeMillis() + 15_000;
        while (output.storages.get() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals("the values must not go on under the names of the series before", 2, output.storages.get());
        assertEquals(List.of("node_load1{instance=\"server-01\",job=\"node\"}",
                             "node_load1{instance=\"server-03\",job=\"node\"}"), output.metricLabels);
        assertTrue("the new storage is asked where its series end", output.resumeQuestions.get() >= 2);
    }

    @Test
    public void configurationCanBeReadFromMemory() throws Exception {
        File file = configuration("lookback=1m");

        assertEquals(1, CollectorConfigurations.fromString(Files.readString(file.toPath())).getClassicCollectorJobConfiguration().size());
    }

    private void awaitStored(BufferingOutput storage, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < deadline) {
            if (storage.stored.size() >= expected) {
                return;
            }
            Thread.sleep(100);
        }
        fail("only " + storage.stored.size() + " of " + expected + " value sets stored; status: " + engine.getStatus());
    }

    private void awaitState(CollectorJobStateEnum expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < deadline) {
            CollectorStatus status = engine.getStatus();
            if (!status.jobs().isEmpty() && status.jobs().get(0).state() == expected) {
                return;
            }
            Thread.sleep(50);
        }
        fail("job did not reach " + expected + ": " + engine.getStatus());
    }

    private void awaitRows(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (output.times.size() >= expected) {
                return;
            }
            Thread.sleep(100);
        }
        fail("only " + output.times.size() + " of " + expected + " value sets arrived; status: " + engine.getStatus());
    }

    private File configuration(String processArguments) throws Exception {
        return configuration(processArguments, 0);
    }

    /**
     * @param wildcardRefreshMillis how often the series of the expression are resolved anew; 0 for never
     */
    private File configuration(String processArguments, long wildcardRefreshMillis) throws Exception {
        String refresh = wildcardRefreshMillis > 0
                ? "\n    expandInterval: " + wildcardRefreshMillis + "ms" : "";
        String yaml = """
                metricGroups:
                  node-basics:
                    type: prometheus
                    metrics:
                      - '%s'
                processGroups:
                  prometheus:
                    - name: prometheus
                      args: %s
                hostGroups:
                  prometheus:
                    hosts:
                      - name: prometheus-local
                        address: %s
                collectors:
                  - metricGroup: node-basics
                    processGroup: prometheus
                    hostGroup: prometheus
                    interval: 1s%s
                """.formatted(LOAD, processArguments, prometheus.address(), refresh);
        File file = File.createTempFile("collector-prometheus", ".yaml");
        file.deleteOnExit();
        Files.writeString(file.toPath(), yaml);
        return file;
    }

    /**
     * A storage that buffers what it is given and writes it on flush, like the output of the AWB.
     * When writing fails it drops the buffer and reports the failure; where its content ends is
     * always what was really written.
     */
    private static final class BufferingOutput implements RepositoryFactory {

        final List<Date> stored = new CopyOnWriteArrayList<>();
        volatile boolean failing;
        volatile int largestBuffer;

        @Override
        public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {
            return new StaticTimeSeriesRepository() {
                private final List<Date> buffer = new ArrayList<>();

                @Override
                public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] labels) {
                }

                @Override
                public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {
                    buffer.add(captureTime);
                    largestBuffer = Math.max(largestBuffer, buffer.size());
                }

                @Override
                public void flush() {
                    if (failing) {
                        buffer.clear();
                        throw new IllegalStateException("repository down");
                    }
                    stored.addAll(buffer);
                    buffer.clear();
                }

                @Override
                public Date getLastCaptureTime() {
                    if (failing) {
                        throw new IllegalStateException("repository down");
                    }
                    return stored.isEmpty() ? null : stored.get(stored.size() - 1);
                }
            };
        }
    }

    /** Records what the job writes. */
    private static final class Output implements RepositoryFactory {

        final List<Date> times = new CopyOnWriteArrayList<>();
        volatile List<String> metricLabels;
        volatile TimeSeriesHeader header;
        volatile Date lastCaptureTime;
        final java.util.concurrent.atomic.AtomicInteger storages = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger resumeQuestions = new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {
            storages.incrementAndGet();
            return new StaticTimeSeriesRepository() {
                @Override
                public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] labels) {
                    header = timeSeriesHeader;
                    metricLabels = List.of(labels);
                }

                @Override
                public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {
                    assertTrue(metricValues.length == 2);
                    times.add(captureTime);
                }

                @Override
                public void flush() {
                }

                @Override
                public Date getLastCaptureTime() {
                    resumeQuestions.incrementAndGet();
                    return lastCaptureTime;
                }
            };
        }
    }
}
