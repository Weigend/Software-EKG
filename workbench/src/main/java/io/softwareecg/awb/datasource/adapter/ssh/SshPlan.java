package io.softwareecg.awb.datasource.adapter.ssh;

import io.softwareecg.awb.datasource.model.HostAddress;
import io.softwareecg.awb.datasource.model.CollectionPlan;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the user decided when collecting from Unix hosts directly, by SSH and {@code top}: the
 * hosts, how to log in, which processes to watch on each of them and which metrics to read.
 * <p>
 * The collector multiplies it out: every host is read for the system metrics, and every host
 * for every process.
 *
 * @param projectName    the name of the analysis
 * @param hostGroup      the name the hosts have as a group in the analysis
 * @param hosts          the hosts; a host named {@code localhost} is read without SSH
 * @param username       the user to log in with, or {@code null} for the current one
 * @param password       the password, or {@code null} to log in with the SSH key
 * @param processes      the processes to watch on every host, by a part of their command line
 * @param systemMetrics  what to read about each host
 * @param processMetrics what to read about each process
 * @param interval       the time between two values
 */
public record SshPlan(String projectName, String hostGroup, List<HostAddress> hosts, String username, String password,
                      List<String> processes, List<TopMetric> systemMetrics, List<TopMetric> processMetrics,
                      Duration interval) implements CollectionPlan {

    /**
     * The id of the collector's counter source that reads {@code top}.
     */
    public static final String SOURCE_TYPE = "unix";

    /**
     * @return how many collector jobs the plan becomes: one per host for the system metrics and
     *         one per host and process
     */
    public int jobCount() {
        return hosts.size() * ((systemMetrics.isEmpty() ? 0 : 1) + (processMetrics.isEmpty() ? 0 : processes.size()));
    }

    /**
     * @return how many series the plan yields
     */
    public int seriesCount() {
        return hosts.size() * (systemMetrics.size() + processes.size() * processMetrics.size());
    }

    @Override
    public String sourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public String endpoint() {
        String who = username == null || username.isBlank() ? "" : username + "@";
        if (hosts.size() <= 3) {
            return who + hosts.stream().map(HostAddress::name).collect(Collectors.joining(", "));
        }
        return who + hosts.get(0).name() + " ... " + hosts.get(hosts.size() - 1).name() + " (" + hosts.size() + " hosts)";
    }

    @Override
    public String secret() {
        return password;
    }

    @Override
    public String toCollectorConfig(String storedProjectName) {
        return SshConfigCompiler.toYaml(new SshPlan(storedProjectName, hostGroup, hosts, username, password,
                processes, systemMetrics, processMetrics, interval));
    }

    @Override
    public boolean isComplete() {
        return !hosts.isEmpty() && hostGroup != null && !hostGroup.isBlank() && jobCount() > 0;
    }

    @Override
    public String firstHostGroup() {
        return hostGroup;
    }

    @Override
    public List<String> firstMetrics() {
        List<TopMetric> metrics = systemMetrics.isEmpty() ? processMetrics : systemMetrics;
        return metrics.stream().map(TopMetric::seriesName).collect(Collectors.toList());
    }
}
