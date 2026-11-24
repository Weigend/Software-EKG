package io.softwareecg.awb.datasource.adapter.windows;

import io.softwareecg.awb.datasource.model.HostAddress;
import io.softwareecg.awb.datasource.model.CollectionPlan;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the user decided when collecting from Windows hosts directly, through their performance
 * counters: the hosts, which processes to watch on each of them, which metrics to read and
 * which further counters.
 * <p>
 * The counters are read by the machine the AWB runs on, which has to be a Windows itself. It
 * reads its own counters and those of other hosts that let its user do so.
 * <p>
 * The collector multiplies the plan out: every host is read for the system metrics and the
 * further counters, and every host for every process.
 *
 * @param projectName    the name of the analysis
 * @param hostGroup      the name the hosts have as a group in the analysis
 * @param hosts          the hosts; a host named {@code localhost} is this machine
 * @param processes      the processes to watch on every host, by the name Windows gives them
 *                       ({@code java}, a second one of the same program is {@code java#1})
 * @param systemMetrics  what to read about each host
 * @param processMetrics what to read about each process
 * @param counters       further performance counters of each host, as Windows writes them:
 *                       {@code \Memory\Cache Bytes}, with wildcards {@code \LogicalDisk(*)\% Free Space}
 * @param interval       the time between two values
 */
public record WindowsPlan(String projectName, String hostGroup, List<HostAddress> hosts, List<String> processes,
                          List<WindowsMetric> systemMetrics, List<WindowsMetric> processMetrics,
                          List<String> counters, Duration interval) implements CollectionPlan {

    /**
     * The id of the collector's counter source that reads the Windows performance counters.
     */
    public static final String SOURCE_TYPE = "win";

    /**
     * Reads a list of counters as the user types it, separated by commas, semicolons or line
     * breaks. A counter is a path that begins with a backslash; anything else is left out.
     */
    public static List<String> parseCounters(String text) {
        List<String> counters = new ArrayList<>();
        if (text != null) {
            for (String part : text.split("[,;\\r\\n]+")) {
                String counter = part.trim();
                if (isCounter(counter) && !counters.contains(counter)) {
                    counters.add(counter);
                }
            }
        }
        return counters;
    }

    /**
     * @return {@code true} if the text has the form of a counter: {@code \Object\Counter} or
     *         {@code \Object(Instance)\Counter}
     */
    public static boolean isCounter(String text) {
        return text != null && text.startsWith("\\") && !text.startsWith("\\\\")
                && text.indexOf('\\', 1) > 1 && !text.endsWith("\\");
    }

    /**
     * @return {@code true} if something is read of each host as a whole
     */
    public boolean readsSystem() {
        return !systemMetrics.isEmpty() || !counters.isEmpty();
    }

    /**
     * @return how many collector jobs the plan becomes: one per host for the system metrics and
     *         the further counters, and one per host and process
     */
    public int jobCount() {
        return hosts.size() * ((readsSystem() ? 1 : 0) + (processMetrics.isEmpty() ? 0 : processes.size()));
    }

    /**
     * @return how many series the plan yields at least; more if it is {@link #isOpenEnded()}
     */
    public int seriesCount() {
        return hosts.size() * (systemMetrics.size() + counters.size() + processes.size() * processMetrics.size());
    }

    /**
     * @return {@code true} if a counter of the plan stands for as many as the host has - one
     *         for each network interface, or a wildcard - so that the number of series is only
     *         known when the hosts are read
     */
    public boolean isOpenEnded() {
        return systemMetrics.stream().anyMatch(WindowsMetric::isPerInstance)
                || counters.stream().anyMatch(counter -> counter.indexOf('*') >= 0);
    }

    @Override
    public String sourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public String endpoint() {
        if (hosts.size() <= 3) {
            return hosts.stream().map(HostAddress::name).collect(Collectors.joining(", "));
        }
        return hosts.get(0).name() + " ... " + hosts.get(hosts.size() - 1).name() + " (" + hosts.size() + " hosts)";
    }

    /**
     * @return {@code null}: the counters are read as the user the AWB runs as
     */
    @Override
    public String secret() {
        return null;
    }

    @Override
    public String toCollectorConfig(String storedProjectName) {
        return WindowsConfigCompiler.toYaml(new WindowsPlan(storedProjectName, hostGroup, hosts, processes,
                systemMetrics, processMetrics, counters, interval));
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
        // a metric with a series for each instance has no name that is known beforehand
        List<String> names = systemMetrics.stream().filter(metric -> !metric.isPerInstance())
                .map(WindowsMetric::seriesName).collect(Collectors.toList());
        if (names.isEmpty()) {
            counters.stream().filter(counter -> counter.indexOf('*') < 0).forEach(names::add);
        }
        if (names.isEmpty() && !processes.isEmpty()) {
            processMetrics.stream().map(WindowsMetric::seriesName).forEach(names::add);
        }
        return names;
    }
}
