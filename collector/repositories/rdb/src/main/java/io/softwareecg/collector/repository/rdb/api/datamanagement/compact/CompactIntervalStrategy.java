package io.softwareecg.collector.repository.rdb.api.datamanagement.compact;

/**
 *
 *
 * @since 29.11.2018
 */
public interface CompactIntervalStrategy {

    CompactInterval getTimeIntervalToCompact();

    long getNextExecutionTime();
}
