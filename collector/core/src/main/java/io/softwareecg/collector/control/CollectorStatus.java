package io.softwareecg.collector.control;

import java.time.Instant;
import java.util.List;

/**
 * A snapshot of a {@link CollectorEngine}: whether it runs and the state of every job.
 *
 * @param running   {@code true} while at least one job is still alive
 * @param startedAt when the current (or last) run was started, {@code null} if never
 * @param jobs      one entry per collector job of that run
 */
public record CollectorStatus(boolean running, Instant startedAt, List<CollectorJobStatus> jobs) {

    public CollectorStatus {
        jobs = List.copyOf(jobs);
    }

    /**
     * @return the jobs that do not deliver values at the moment
     */
    public List<CollectorJobStatus> jobsInError() {
        return jobs.stream().filter(CollectorJobStatus::isInError).toList();
    }
}
