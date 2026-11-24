package io.softwareecg.collector.ui;

import io.softwareecg.collector.control.CollectorJobStatus;
import io.softwareecg.collector.control.CollectorJobValues;
import io.softwareecg.collector.control.CollectorStatus;
import io.softwareecg.collector.control.CollectorJobState.CollectorJobStateEnum;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the display shows, kept apart from how it is drawn: the jobs of the collector with their
 * state, and for every metric of a job the values it read lately.
 * <p>
 * The collector itself only knows the values a job read last. The display looks at them once a
 * second and remembers what it saw, which gives the small curves beside the numbers. That
 * history begins when the display is started and is not what the collector stores.
 */
final class DisplayModel {

    /** How many values of a metric are remembered. */
    static final int HISTORY = 120;

    /** How a job is doing, in the three colours a glance can tell apart. */
    enum Condition {
        /** something is wrong and the job says what */
        PROBLEM,
        /** the job is on its way to its first values */
        STARTING,
        /** the job delivers */
        OK,
        /** the job has ended */
        STOPPED
    }

    /**
     * The values of one metric that were seen, oldest first. A value that was missing is
     * {@link Double#NaN}.
     */
    static final class History {

        private final double[] values = new double[HISTORY];

        private int size;

        private int next;

        void add(Double value) {
            values[next] = value == null ? Double.NaN : value;
            next = (next + 1) % HISTORY;
            size = Math.min(size + 1, HISTORY);
        }

        /** @return the values seen, oldest first */
        double[] values() {
            double[] ordered = new double[size];
            int first = size < HISTORY ? 0 : next;
            for (int i = 0; i < size; i++) {
                ordered[i] = values[(first + i) % HISTORY];
            }
            return ordered;
        }

        /** @return the value seen last; {@link Double#NaN} if there is none */
        double last() {
            return size == 0 ? Double.NaN : values[(next - 1 + HISTORY) % HISTORY];
        }

        /** @return the smallest value seen; {@link Double#NaN} if there is none */
        double min() {
            double min = Double.NaN;
            for (double value : values()) {
                if (!Double.isNaN(value) && !(value >= min)) {
                    min = value;
                }
            }
            return min;
        }

        /** @return the largest value seen; {@link Double#NaN} if there is none */
        double max() {
            double max = Double.NaN;
            for (double value : values()) {
                if (!Double.isNaN(value) && !(value <= max)) {
                    max = value;
                }
            }
            return max;
        }
    }

    /**
     * One metric of a job.
     */
    record Metric(String label, History history) {
    }

    /**
     * One job of the collector: a metric group read of one process on one host.
     */
    static final class Job {

        private final String key;

        private CollectorJobStatus status;

        private Instant seen;

        private final Map<String, History> histories = new LinkedHashMap<>();

        private Job(String key, CollectorJobStatus status) {
            this.key = key;
            this.status = status;
        }

        CollectorJobStatus status() {
            return status;
        }

        /** @return the host and the process, as a job is told from the others */
        String title() {
            String host = status.nodeName() == null || status.nodeName().isBlank() ? status.nodeAddress() : status.nodeName();
            return host + "  ·  " + status.processLabel();
        }

        /** @return the metric group and the source it is read with */
        String subtitle() {
            return status.metricGroupId() + " (" + status.sourceType() + ")";
        }

        Condition condition() {
            return conditionOf(status.state());
        }

        /** @return the state in words; a colour alone does not say it to everyone */
        String stateText() {
            return switch (status.state()) {
                case RUN_OK -> "OK";
                case BOOTSTRAP -> "Starting";
                case WILDCARD_EXPANSION -> "Resolving metrics";
                case WAIT_FOR_DATA -> "Waiting for values";
                case ENV_ERROR -> "Not reachable";
                case NO_DATA_ERROR -> "No values";
                case STORAGE_ERROR -> "Cannot write";
                case FAILED -> "Failed";
                case STOPPED -> "Stopped";
            };
        }

        /** @return the metrics of the job, in the order of its configuration */
        List<Metric> metrics() {
            List<Metric> metrics = new ArrayList<>();
            histories.forEach((label, history) -> metrics.add(new Metric(label, history)));
            return metrics;
        }

        /** @return the metric of that name, or {@code null} if the job no longer has it */
        Metric metric(String label) {
            History history = histories.get(label);
            return history == null ? null : new Metric(label, history);
        }

        private void take(CollectorJobStatus current) {
            status = current;
            CollectorJobValues values = current.latestValues();
            if (values == null || values.isEmpty() || values.time().equals(seen)) {
                return;   // nothing read since the last look
            }
            seen = values.time();
            // the metrics of a job change when its wildcards are resolved anew
            histories.keySet().retainAll(values.labels());
            for (int i = 0; i < values.labels().size(); i++) {
                Double value = i < values.values().size() ? values.values().get(i) : null;
                histories.computeIfAbsent(values.labels().get(i), label -> new History()).add(value);
            }
        }
    }

    /**
     * The collector at a glance.
     *
     * @param running      whether any job is still at work
     * @param jobs         the number of jobs
     * @param ok           how many of them deliver
     * @param problems     how many of them say that something is wrong
     * @param metrics      the number of metrics all jobs read together
     * @param startedAt    when the collector was started; {@code null} if it was not
     * @param newestValues the time of the newest values of any job; {@code null} if there are none
     */
    record Summary(boolean running, int jobs, int ok, int problems, int metrics, Instant startedAt,
                   Instant newestValues) {

        /** @return the collector as a whole, as the worst of what its jobs say */
        Condition condition() {
            if (problems > 0) {
                return Condition.PROBLEM;
            }
            if (!running) {
                return Condition.STOPPED;
            }
            return ok == jobs ? Condition.OK : Condition.STARTING;
        }

        String text() {
            return switch (condition()) {
                case PROBLEM -> problems == 1 ? "1 problem" : problems + " problems";
                case STOPPED -> "Stopped";
                case STARTING -> "Starting";
                case OK -> "All jobs OK";
            };
        }
    }

    private final Map<String, Job> jobs = new LinkedHashMap<>();

    private Summary summary = new Summary(false, 0, 0, 0, 0, null, null);

    /**
     * Takes a look at the collector.
     */
    void update(CollectorStatus status) {
        Map<String, Job> current = new LinkedHashMap<>();
        int ok = 0;
        int problems = 0;
        int metrics = 0;
        Instant newest = null;
        for (CollectorJobStatus jobStatus : status.jobs()) {
            String key = String.join("\u0000", String.valueOf(jobStatus.nodeGroupId()), String.valueOf(jobStatus.nodeName()),
                    String.valueOf(jobStatus.nodeAddress()), String.valueOf(jobStatus.processLabel()),
                    String.valueOf(jobStatus.metricGroupId()));
            Job job = jobs.get(key);
            if (job == null) {
                job = new Job(key, jobStatus);
            }
            job.take(jobStatus);
            current.put(key, job);

            Condition condition = job.condition();
            ok += condition == Condition.OK ? 1 : 0;
            problems += condition == Condition.PROBLEM ? 1 : 0;
            metrics += jobStatus.metricCount();
            if (jobStatus.lastSampleTime() != null && (newest == null || jobStatus.lastSampleTime().isAfter(newest))) {
                newest = jobStatus.lastSampleTime();
            }
        }
        jobs.clear();
        jobs.putAll(current);
        summary = new Summary(status.running(), current.size(), ok, problems, metrics, status.startedAt(), newest);
    }

    Summary summary() {
        return summary;
    }

    /**
     * @return the jobs, those with a problem first: on a small screen they must not be below
     *         the edge
     */
    List<Job> jobs() {
        List<Job> ordered = new ArrayList<>(jobs.values());
        ordered.sort(Comparator.comparing((Job job) -> job.condition()).thenComparing(Job::title)
                .thenComparing(Job::subtitle));
        return ordered;
    }

    /** @return the job with that key, or {@code null} if the collector no longer has it */
    Job job(String key) {
        return jobs.get(key);
    }

    static String keyOf(Job job) {
        return job.key;
    }

    static Condition conditionOf(CollectorJobStateEnum state) {
        return switch (state) {
            case RUN_OK -> Condition.OK;
            case ENV_ERROR, NO_DATA_ERROR, STORAGE_ERROR, FAILED -> Condition.PROBLEM;
            case STOPPED -> Condition.STOPPED;
            case BOOTSTRAP, WILDCARD_EXPANSION, WAIT_FOR_DATA -> Condition.STARTING;
        };
    }
}
