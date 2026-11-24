package io.softwareecg.collector.ui;

import io.softwareecg.collector.control.CollectorJobStatus;
import io.softwareecg.collector.control.CollectorJobValues;
import io.softwareecg.collector.control.CollectorStatus;
import io.softwareecg.collector.control.CollectorJobState.CollectorJobStateEnum;
import io.softwareecg.collector.ui.DisplayModel.Condition;
import io.softwareecg.collector.ui.DisplayModel.Job;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DisplayModelTest {

    private static final Instant T0 = Instant.parse("2026-11-21T10:00:00Z");

    private final DisplayModel model = new DisplayModel();

    @Test
    public void theCollectorAtAGlance() {
        model.update(status(true,
                job("db-01", "global", "os", CollectorJobStateEnum.RUN_OK, 3, T0, null, values(T0, 1.0, 2.0, 3.0)),
                job("db-02", "global", "os", CollectorJobStateEnum.ENV_ERROR, 3, null, "db-02 is not reachable", null),
                job("app-01", "checkout", "jvm", CollectorJobStateEnum.WAIT_FOR_DATA, 2, null, null, null)));

        DisplayModel.Summary summary = model.summary();
        assertEquals(3, summary.jobs());
        assertEquals(1, summary.ok());
        assertEquals(1, summary.problems());
        assertEquals(8, summary.metrics());
        assertEquals(T0, summary.newestValues());
        assertEquals("one problem is what is said, whatever else is fine", Condition.PROBLEM, summary.condition());
        assertEquals("1 problem", summary.text());
    }

    @Test
    public void theCollectorIsWhatTheWorstOfItsJobsIs() {
        model.update(status(true, job("a", "p", "g", CollectorJobStateEnum.RUN_OK, 1, T0, null, null)));
        assertEquals(Condition.OK, model.summary().condition());
        assertEquals("All jobs OK", model.summary().text());

        model.update(status(true, job("a", "p", "g", CollectorJobStateEnum.RUN_OK, 1, T0, null, null),
                job("b", "p", "g", CollectorJobStateEnum.BOOTSTRAP, 0, null, null, null)));
        assertEquals(Condition.STARTING, model.summary().condition());

        model.update(status(true, job("a", "p", "g", CollectorJobStateEnum.STORAGE_ERROR, 1, T0, "disk full", null),
                job("b", "p", "g", CollectorJobStateEnum.FAILED, 0, null, "boom", null)));
        assertEquals("2 problems", model.summary().text());

        model.update(status(false, job("a", "p", "g", CollectorJobStateEnum.STOPPED, 1, T0, null, null)));
        assertEquals(Condition.STOPPED, model.summary().condition());
    }

    @Test
    public void jobsWithAProblemComeFirst() {
        model.update(status(true,
                job("b-ok", "p", "g", CollectorJobStateEnum.RUN_OK, 1, T0, null, null),
                job("d-starting", "p", "g", CollectorJobStateEnum.WILDCARD_EXPANSION, 0, null, null, null),
                job("c-broken", "p", "g", CollectorJobStateEnum.NO_DATA_ERROR, 1, null, null, null),
                job("a-ok", "p", "g", CollectorJobStateEnum.RUN_OK, 1, T0, null, null)));

        assertEquals(List.of("c-broken  ·  p", "d-starting  ·  p", "a-ok  ·  p", "b-ok  ·  p"),
                model.jobs().stream().map(Job::title).toList());
        assertEquals("No values", model.jobs().get(0).stateText());
        assertEquals("g (unix)", model.jobs().get(0).subtitle());
    }

    @Test
    public void theValuesOfAMetricAreRememberedEveryTimeTheJobReadNewOnes() {
        for (int second = 0; second < 5; second++) {
            Instant time = T0.plusSeconds(second);
            model.update(status(true, job("db-01", "global", "os", CollectorJobStateEnum.RUN_OK, 2, time, null,
                    new CollectorJobValues(time, List.of("cpu", "load"), Arrays.asList(10.0 * second, null)))));
            // the display looks more often than the job reads: the same values are not counted twice
            model.update(status(true, job("db-01", "global", "os", CollectorJobStateEnum.RUN_OK, 2, time, null,
                    new CollectorJobValues(time, List.of("cpu", "load"), Arrays.asList(10.0 * second, null)))));
        }

        Job job = model.jobs().get(0);
        assertEquals(List.of("cpu", "load"), job.metrics().stream().map(DisplayModel.Metric::label).toList());
        assertArrayEquals(new double[]{0, 10, 20, 30, 40}, job.metric("cpu").history().values(), 0);
        assertEquals(40, job.metric("cpu").history().last(), 0);
        assertEquals(0, job.metric("cpu").history().min(), 0);
        assertEquals(40, job.metric("cpu").history().max(), 0);
        assertTrue("a value that was missing is a gap", Double.isNaN(job.metric("load").history().last()));
        assertTrue(Double.isNaN(job.metric("load").history().min()));
    }

    @Test
    public void onlyTheNewestValuesAreKept() {
        for (int second = 0; second < DisplayModel.HISTORY + 30; second++) {
            Instant time = T0.plusSeconds(second);
            model.update(status(true, job("db-01", "global", "os", CollectorJobStateEnum.RUN_OK, 1, time, null,
                    new CollectorJobValues(time, List.of("cpu"), List.of((double) second)))));
        }

        double[] values = model.jobs().get(0).metric("cpu").history().values();
        assertEquals(DisplayModel.HISTORY, values.length);
        assertEquals(30, values[0], 0);
        assertEquals(DisplayModel.HISTORY + 29, values[values.length - 1], 0);
    }

    @Test
    public void aJobStaysTheSameJobAndItsMetricsFollowItsConfiguration() {
        model.update(status(true, job("db-01", "global", "os", CollectorJobStateEnum.RUN_OK, 2, T0, null,
                new CollectorJobValues(T0, List.of("cpu", "load"), List.of(1.0, 2.0)))));
        Job first = model.jobs().get(0);

        // the wildcards of the job were resolved anew: one metric went, one came
        Instant later = T0.plusSeconds(1);
        model.update(status(true, job("db-01", "global", "os", CollectorJobStateEnum.RUN_OK, 2, later, null,
                new CollectorJobValues(later, List.of("cpu", "swap"), List.of(3.0, 4.0)))));

        assertSame(first, model.jobs().get(0));
        assertSame(first, model.job(DisplayModel.keyOf(first)));
        assertEquals(List.of("cpu", "swap"), first.metrics().stream().map(DisplayModel.Metric::label).toList());
        assertArrayEquals(new double[]{1, 3}, first.metric("cpu").history().values(), 0);
        assertNull(first.metric("load"));

        model.update(status(true));
        assertNull("a job the collector no longer has is gone", model.job(DisplayModel.keyOf(first)));
    }

    // ---------------------------------------------------------------------------------------------

    static CollectorStatus status(boolean running, CollectorJobStatus... jobs) {
        return new CollectorStatus(running, T0.minusSeconds(3_700), List.of(jobs));
    }

    static CollectorJobStatus job(String host, String process, String metricGroup, CollectorJobStateEnum state,
                                  int metrics, Instant lastSample, String error, CollectorJobValues values) {
        return new CollectorJobStatus("servers", host, "10.0.0.1", process, metricGroup, "unix", state, metrics,
                lastSample, error, values == null ? CollectorJobValues.NONE : values);
    }

    private static CollectorJobValues values(Instant time, Double... values) {
        List<String> labels = new java.util.ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            labels.add("metric-" + i);
        }
        return new CollectorJobValues(time, labels, Arrays.asList(values));
    }
}
