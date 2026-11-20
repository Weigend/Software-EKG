package io.softwareecg.collector.control;

import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.control.jobs.CollectorJobState.CollectorJobStateEnum;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The collector as an embeddable engine: start, status, stop and start again.
 * The configuration used has one host, two processes and one metric group of the mock source,
 * which the collector multiplies out to two jobs.
 */
public class CollectorEngineTest extends BaseIntegrationTest {

    private static final long AWAIT_MILLIS = 20_000;

    private final RecordingOutput output = new RecordingOutput();

    private CollectorEngine engine;

    @After
    public void stopEngine() {
        if (engine != null) {
            engine.stop();
        }
    }

    @Test
    public void startRunsOneJobPerCombinationAndReportsItsStatus() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(configuration());
        awaitAllJobsIn(CollectorJobStateEnum.RUN_OK);

        CollectorStatus status = engine.getStatus();

        assertTrue(status.running());
        assertNotNull(status.startedAt());
        assertEquals(2, status.jobs().size());
        assertEquals("one job per process", 2,
                status.jobs().stream().map(CollectorJobStatus::processLabel).collect(Collectors.toSet()).size());
        for (CollectorJobStatus job : status.jobs()) {
            assertEquals("mock", job.sourceType());
            assertEquals("mockMetrics", job.metricGroupId());
            assertEquals("localhost", job.nodeName());
            assertEquals("the mock source expands its wildcard to three metrics", 3, job.metricCount());
            assertNotNull(job.lastSampleTime());
            assertNull(job.lastError());
            assertFalse(job.isInError());
        }
        assertTrue(status.jobsInError().isEmpty());
        assertTrue("values reached the supplied output", output.valueSets.get() >= 2);
    }

    @Test
    public void stopEndsAllJobsAndFlushesTheOutput() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(configuration());
        awaitAllJobsIn(CollectorJobStateEnum.RUN_OK);

        assertTrue("all jobs end in time", engine.stop());

        assertFalse(engine.isRunning());
        CollectorStatus status = engine.getStatus();
        assertFalse(status.running());
        for (CollectorJobStatus job : status.jobs()) {
            assertEquals(CollectorJobStateEnum.STOPPED, job.state());
        }
        assertTrue("each job flushes its storage when it ends", output.flushes.get() >= 2);

        int valueSetsAfterStop = output.valueSets.get();
        Thread.sleep(2_500);
        assertEquals("a stopped engine collects nothing", valueSetsAfterStop, output.valueSets.get());
    }

    @Test
    public void canBeStartedAgainAfterStop() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(configuration());
        awaitAllJobsIn(CollectorJobStateEnum.RUN_OK);
        assertTrue(engine.stop());

        engine.start(configuration());
        awaitAllJobsIn(CollectorJobStateEnum.RUN_OK);

        assertTrue(engine.isRunning());
        assertEquals(2, engine.getStatus().jobs().size());
    }

    @Test
    public void startWhileRunningIsRejected() throws Exception {
        engine = new CollectorEngine(output, true);
        engine.start(configuration());

        try {
            engine.start(configuration());
            fail("a second start must be rejected");
        } catch (IllegalStateException expected) {
            // the running jobs are left alone
        }
        assertEquals(2, engine.getStatus().jobs().size());
    }

    @Test
    public void failingJobIsVisibleInTheStatusAndCanStillBeStopped() throws Exception {
        RepositoryFactory brokenOutput = descriptor -> new StaticTimeSeriesRepository() {
            @Override
            public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {
                throw new IllegalStateException("disk full");
            }

            @Override
            public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {
            }

            @Override
            public void flush() {
            }
        };
        engine = new CollectorEngine(brokenOutput, true);
        engine.start(configuration());
        awaitAllJobsIn(CollectorJobStateEnum.FAILED);

        CollectorStatus status = engine.getStatus();
        assertEquals(2, status.jobsInError().size());
        for (CollectorJobStatus job : status.jobs()) {
            assertTrue(job.lastError(), job.lastError().contains("disk full"));
        }

        assertTrue("failed jobs end on stop as well", engine.stop());
        assertFalse(engine.isRunning());
    }

    private CollectorConfiguration configuration() {
        File file = new File(getClass().getResource("ok/ok1.yaml").getPath());
        return CollectorConfigurations.fromFile(file);
    }

    private void awaitAllJobsIn(CollectorJobStateEnum expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            CollectorStatus status = engine.getStatus();
            if (!status.jobs().isEmpty() && status.jobs().stream().allMatch(job -> job.state() == expected)) {
                return;
            }
            Thread.sleep(100);
        }
        fail("jobs did not reach " + expected + ": " + engine.getStatus());
    }

    /**
     * An output that only counts what it is given.
     */
    private static final class RecordingOutput implements RepositoryFactory {

        private final AtomicInteger valueSets = new AtomicInteger();
        private final AtomicInteger flushes = new AtomicInteger();

        @Override
        public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {
            return new StaticTimeSeriesRepository() {
                @Override
                public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {
                }

                @Override
                public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {
                    valueSets.incrementAndGet();
                }

                @Override
                public void flush() {
                    flushes.incrementAndGet();
                }
            };
        }
    }
}
