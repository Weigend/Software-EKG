package io.softwareecg.collector.control.jobs;

import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.config.SidecarCollectorJobConfiguration;

/**
 * todo insert comment here
 *
 * @since 10.08.2018
 */
public class SidecarCollectorJob extends CollectorJob {

    /**
     * Create a collector.
     *
     * @param collectorConfig
     */
    public SidecarCollectorJob(SidecarCollectorJobConfiguration collectorConfig, JobManager jobManager) {
        super(collectorConfig, jobManager);
    }

    /**
     * Create a collector that writes to the given output.
     */
    public SidecarCollectorJob(SidecarCollectorJobConfiguration collectorConfig, JobManager jobManager,
                 RepositoryFactory repositoryFactory) {
        super(collectorConfig, jobManager, repositoryFactory);
    }
}
