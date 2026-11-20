package io.softwareecg.collector.control.jobs;

import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.dt.ClassicCollectorJobConfiguration;

/**
 * todo insert comment here
 *
 * @since 10.08.2018
 */
public class ClassicCollectorJob extends CollectorJob {

    /**
     * Create a collector.
     *
     * @param collectorConfig
     */
    public ClassicCollectorJob(ClassicCollectorJobConfiguration collectorConfig, JobManager jobManager) {
        super(collectorConfig, jobManager);
    }

    /**
     * Create a collector that writes to the given output.
     */
    public ClassicCollectorJob(ClassicCollectorJobConfiguration collectorConfig, JobManager jobManager,
                 RepositoryFactory repositoryFactory) {
        super(collectorConfig, jobManager, repositoryFactory);
    }
}
