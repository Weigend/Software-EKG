package io.softwareecg.awb.datasource.spi;

import java.util.Objects;

/** Stable identity and presentation of a source offered by New Analysis. */
public record AnalysisSourceDefinition(String id, String title, boolean aggregating, boolean available) {
    public static final AnalysisSourceDefinition PROMETHEUS =
            new AnalysisSourceDefinition("prometheus", "Prometheus", true, true);
    public static final AnalysisSourceDefinition SSH_TOP =
            new AnalysisSourceDefinition("ssh-top", "SSH / top", false, true);
    public static final AnalysisSourceDefinition JOLOKIA =
            new AnalysisSourceDefinition("jolokia", "Jolokia", false, false);
    public static final AnalysisSourceDefinition JMX =
            new AnalysisSourceDefinition("jmx", "JMX", false, false);
    public static final AnalysisSourceDefinition WINDOWS_COUNTERS =
            new AnalysisSourceDefinition("windows-counters", "Performance Counters", false, true);
    public static final AnalysisSourceDefinition POSTGRES =
            new AnalysisSourceDefinition("postgres", "PostgreSQL", false, false);

    public AnalysisSourceDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Analysis source id must not be blank");
        }
    }

    public boolean isAggregating() {
        return aggregating;
    }

    public boolean isAvailable() {
        return available;
    }
}
