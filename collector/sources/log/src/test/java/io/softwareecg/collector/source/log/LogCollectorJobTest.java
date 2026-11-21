package io.softwareecg.collector.source.log;

import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.control.CollectorEngine;
import io.softwareecg.collector.control.CollectorJobStatus;
import io.softwareecg.collector.control.CollectorStatus;
import io.softwareecg.collector.control.jobs.CollectorJobState.CollectorJobStateEnum;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The source in a collector job, as a collector runs it: with the clock of this machine and a
 * file that is written while it is followed.
 */
public class LogCollectorJobTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path file;

    private CollectorEngine engine;

    private final Output output = new Output();

    @Before
    public void createFile() throws IOException {
        file = folder.newFile("checkout.log").toPath();
    }

    @After
    public void stop() {
        if (engine != null) {
            engine.stop();
        }
    }

    @Test
    public void jobCountsWhatIsWrittenWhileItRuns() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("")));
        awaitRows(1);   // the file is followed

        int written = 0;
        for (int i = 0; i < 25; i++) {
            write(line(System.currentTimeMillis(), "user login succeeded"),
                  line(System.currentTimeMillis(), "request took " + (100 + i) + " ms"));
            written++;
            Thread.sleep(100);
        }
        long lastLine = System.currentTimeMillis();
        awaitRowAfter(lastLine);

        CollectorJobStatus job = engine.getStatus().jobs().get(0);
        assertEquals("log", job.sourceType());
        assertEquals(CollectorJobStateEnum.RUN_OK, job.state());
        assertEquals(2, job.metricCount());
        assertNull(job.lastError());
        assertEquals(List.of("logins", "slowest request"), output.metricLabels);
        assertEquals("this-machine", output.header.getHostName());

        assertEquals("every line is counted once", written, output.sum(0), 0);
        assertEquals("the slowest request of all intervals", 124, output.max(1), 0);
        assertGridOfSeconds();
    }

    @Test
    public void jobFillsInWhatTheFileAlreadyHas() throws Exception {
        long start = System.currentTimeMillis();
        for (int secondsAgo = 40; secondsAgo >= 5; secondsAgo -= 5) {
            write(line(start - secondsAgo * 1_000L, "user login succeeded"));
        }

        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("lookback=1m")));
        awaitRowAfter(start);

        assertEquals("the eight logins before the collector was started", 8, output.sum(0), 0);
        assertTrue("at the time they happened: " + output.times.get(0),
                output.times.get(0).getTime() <= start - 40_000 && output.times.get(0).getTime() > start - 42_000);
        assertEquals(CollectorJobStateEnum.RUN_OK, engine.getStatus().jobs().get(0).state());
        assertGridOfSeconds();
    }

    @Test
    public void aFileThatIsNotThereIsShownAsSuchUntilItIs() throws Exception {
        Files.delete(file);
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("")));

        awaitState(CollectorJobStateEnum.ENV_ERROR);
        CollectorJobStatus missing = engine.getStatus().jobs().get(0);
        assertTrue(missing.isInError());
        assertTrue(missing.lastError(), missing.lastError().contains("does not exist or may not be read"));
        assertTrue(output.times.isEmpty());

        Files.createFile(file);
        awaitState(CollectorJobStateEnum.RUN_OK);
        write(line(System.currentTimeMillis(), "user login succeeded"));
        awaitRowAfter(System.currentTimeMillis());

        assertEquals(1, output.sum(0), 0);
        assertNull(engine.getStatus().jobs().get(0).lastError());
    }

    @Test
    public void aMetricThatIsNoExpressionEndsTheJobWithTheReason() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromFile(configuration("").toPath().resolveSibling("broken.yaml").toFile()));

        awaitState(CollectorJobStateEnum.FAILED);
        String error = engine.getStatus().jobs().get(0).lastError();
        assertTrue(error, error.contains("not a regular expression"));
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private void assertGridOfSeconds() {
        List<Date> times = new ArrayList<>(output.times);
        for (int i = 1; i < times.size(); i++) {
            assertEquals("row " + i, 1_000, times.get(i).getTime() - times.get(i - 1).getTime());
        }
    }

    private static String line(long time, String message) {
        return Instant.ofEpochMilli(time) + " INFO " + message;
    }

    private void write(String... lines) throws IOException {
        Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
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

    /**
     * Waits until the interval of the given time was delivered.
     */
    private void awaitRowAfter(long time) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (!output.times.isEmpty() && output.times.get(output.times.size() - 1).getTime() + 1_000 > time) {
                return;
            }
            Thread.sleep(100);
        }
        fail("the interval of " + new Date(time) + " did not arrive; status: " + engine.getStatus());
    }

    private void awaitState(CollectorJobStateEnum expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            CollectorStatus status = engine.getStatus();
            if (!status.jobs().isEmpty() && status.jobs().get(0).state() == expected) {
                return;
            }
            Thread.sleep(50);
        }
        fail("job did not reach " + expected + ": " + engine.getStatus());
    }

    /**
     * Writes two configurations next to each other: the one returned, and "broken.yaml" with a
     * metric that is no regular expression.
     */
    private File configuration(String processArguments) throws Exception {
        String yaml = """
                metricGroups:
                  checkout-log:
                    type: log
                    metrics:
                      - metric: 'login succeeded'
                        label: logins
                      - metric: '%s'
                        label: slowest request
                processGroups:
                  checkout:
                    - name: checkout
                      source: '%s'
                      args: '%s'
                hostGroups:
                  local:
                    hosts:
                      - name: this-machine
                        address: 127.0.0.1
                collectors:
                  - metricGroup: checkout-log
                    processGroup: checkout
                    hostGroup: local
                    interval: 1s
                """;
        File configuration = folder.newFile("collector.yaml");
        Files.writeString(configuration.toPath(), yaml.formatted("took (?<max>\\d+) ms", file, processArguments));
        Files.writeString(configuration.toPath().resolveSibling("broken.yaml"),
                yaml.formatted("took (?<max>\\d+ ms", file, processArguments));
        return configuration;
    }

    private static final class Output implements RepositoryFactory {

        final List<Date> times = new CopyOnWriteArrayList<>();
        final List<Double[]> values = new CopyOnWriteArrayList<>();
        volatile List<String> metricLabels;
        volatile TimeSeriesHeader header;

        @Override
        public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {
            return new StaticTimeSeriesRepository() {
                @Override
                public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] labels) {
                    header = timeSeriesHeader;
                    metricLabels = List.of(labels);
                }

                @Override
                public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {
                    values.add(metricValues.clone());
                    times.add(captureTime);
                }

                @Override
                public void flush() {
                }
            };
        }

        double sum(int metric) {
            return values.stream().filter(v -> v[metric] != null).mapToDouble(v -> v[metric]).sum();
        }

        double max(int metric) {
            return values.stream().filter(v -> v[metric] != null).mapToDouble(v -> v[metric]).max().orElse(Double.NaN);
        }
    }
}
