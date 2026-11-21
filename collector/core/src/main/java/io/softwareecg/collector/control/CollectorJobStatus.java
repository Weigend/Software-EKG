package io.softwareecg.collector.control;

import io.softwareecg.collector.control.jobs.CollectorJobState.CollectorJobStateEnum;

import java.time.Instant;

/**
 * What one collector job collects and how it is doing. A job is one combination of
 * node, process and metric group from the collector configuration.
 *
 * @param nodeGroupId    the host group (or cloud project) the node belongs to
 * @param nodeName       the name of the node the values come from
 * @param nodeAddress    the address the node is reached at
 * @param processLabel   the process the values belong to
 * @param metricGroupId  the metric group collected
 * @param sourceType     the id of the counter source that fetches the values
 * @param state          the current state of the job
 * @param metricCount    the number of concrete metrics, after wildcard expansion
 * @param lastSampleTime when the last value set was written, {@code null} before the first one
 * @param lastError      why the job failed, {@code null} while it is healthy
 */
public record CollectorJobStatus(
        String nodeGroupId,
        String nodeName,
        String nodeAddress,
        String processLabel,
        String metricGroupId,
        String sourceType,
        CollectorJobStateEnum state,
        int metricCount,
        Instant lastSampleTime,
        String lastError,
        CollectorJobValues latestValues) {

    /**
     * @return {@code true} if the job does not deliver values: the source is unreachable,
     *         returns no data, or the job has failed
     */
    public boolean isInError() {
        return state == CollectorJobStateEnum.ENV_ERROR
                || state == CollectorJobStateEnum.NO_DATA_ERROR
                || state == CollectorJobStateEnum.STORAGE_ERROR
                || state == CollectorJobStateEnum.FAILED;
    }
}
