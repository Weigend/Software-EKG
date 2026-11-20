package io.softwareecg.awb.datasource.bl;

import java.util.List;

/**
 * What the user decided when connecting a data source to an analysis: where the data comes from
 * and what is collected. Every kind of source has its own plan - a Prometheus server is one
 * address that already holds many systems, hosts reached by SSH are a list - but all of them
 * end in a configuration for the collector.
 */
public interface CollectionPlan {

    /**
     * @return the name of the analysis; becomes the project in the AWB
     */
    String projectName();

    /**
     * @return the kind of source, the id of the counter source that collects it
     */
    String sourceType();

    /**
     * @return where the data comes from, for display
     */
    String endpoint();

    /**
     * @return the password or token the source needs, {@code null} if it needs none
     */
    String secret();

    /**
     * Compiles the plan to a collector configuration. The configuration never contains the
     * secret, only the placeholder {@link CollectorConfigCompiler#SECRET_PLACEHOLDER}.
     *
     * @param projectName the name of the project as it is stored
     */
    String toCollectorConfig(String projectName);

    /**
     * @return {@code true} if there is something to collect
     */
    boolean isComplete();

    /**
     * @return the group of systems the first chart of the analysis shows
     */
    String firstHostGroup();

    /**
     * @return the names of the metrics collected for {@link #firstHostGroup()}
     */
    List<String> firstMetrics();
}
