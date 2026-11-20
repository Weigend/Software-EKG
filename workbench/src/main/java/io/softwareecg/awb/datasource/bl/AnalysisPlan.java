package io.softwareecg.awb.datasource.bl;

import io.softwareecg.awb.datasource.bl.prometheus.MetricExpression;
import io.softwareecg.collector.source.prometheus.PrometheusCounterSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the user decided when starting an analysis on a Prometheus environment: where the data
 * comes from, which groups of systems are of interest, what to collect for each, how far back
 * to start and how often to update.
 *
 * @param projectName the name of the analysis; becomes the project in the AWB
 * @param url         the address of the Prometheus server
 * @param username    the user for basic authentication, or {@code null}
 * @param password    the password, or a bearer token if no user is given, or {@code null}
 * @param groups      the selected groups with what to collect for each
 * @param lookback    how much existing history the first fill brings in
 * @param interval    the time between two values of a series, and between two updates
 */
public record AnalysisPlan(String projectName, String url, String username, String password,
                           List<GroupSelection> groups, Duration lookback, Duration interval) implements CollectionPlan {

    /**
     * @param group       the name of the group (the Prometheus job)
     * @param expressions what to collect for it
     */
    public record GroupSelection(String group, List<MetricExpression> expressions) {
    }

    @Override
    public String sourceType() {
        return PrometheusCounterSource.ID;
    }

    @Override
    public String endpoint() {
        return url;
    }

    @Override
    public String secret() {
        return password;
    }

    @Override
    public String toCollectorConfig(String storedProjectName) {
        return CollectorConfigCompiler.toYaml(new AnalysisPlan(storedProjectName, url, username, password,
                groups, lookback, interval));
    }

    @Override
    public boolean isComplete() {
        return !groups.isEmpty() && groups.stream().noneMatch(group -> group.expressions().isEmpty());
    }

    @Override
    public String firstHostGroup() {
        return groups.get(0).group();
    }

    @Override
    public List<String> firstMetrics() {
        return groups.get(0).expressions().stream().map(MetricExpression::seriesName).collect(Collectors.toList());
    }
}
