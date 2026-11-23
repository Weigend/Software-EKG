package io.softwareecg.awb.datasource.adapter.ssh;

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
public record SshPlan(String projectName, String hostGroup, List<Host> hosts, String username, String password,
                      List<String> processes, List<TopMetric> systemMetrics, List<TopMetric> processMetrics,
                      Duration interval) implements CollectionPlan {

    /**
     * The id of the collector's counter source that reads {@code top}.
     */
    public static final String SOURCE_TYPE = "unix";

    /**
     * @param name    the name of the host in the analysis
     * @param address where it is reached: a name or address, optionally with the SSH port
     */
    public record Host(String name, String address) {

        /**
         * Reads a host as the user types it: {@code db-01}, {@code db-01:2222} or, with a name
         * of its own, {@code db-01=10.0.0.5}.
         */
        public static Host parse(String text) {
            String trimmed = text.trim();
            int equals = trimmed.indexOf('=');
            if (equals > 0) {
                return new Host(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
            }
            int colon = trimmed.lastIndexOf(':');
            boolean port = colon > 0 && trimmed.indexOf(':') == colon
                    && trimmed.substring(colon + 1).chars().allMatch(Character::isDigit);
            return new Host(port ? trimmed.substring(0, colon) : trimmed, trimmed);
        }

        /**
         * @return {@code true} if the host is this machine and read without SSH
         */
        public boolean isLocal() {
            return address.startsWith("localhost");
        }
    }

    /**
     * Reads a list of hosts as the user types it, separated by commas, semicolons, blanks or
     * line breaks. A range in square brackets is written out: {@code db-[01-03]} are the hosts
     * {@code db-01}, {@code db-02} and {@code db-03}.
     */
    public static List<Host> parseHosts(String text) {
        if (text == null) {
            return List.of();
        }
        List<Host> hosts = new java.util.ArrayList<>();
        for (String part : text.split("[,;\\s]+")) {
            if (part.isBlank()) {
                continue;
            }
            for (String expanded : expandRange(part.trim())) {
                Host host = Host.parse(expanded);
                if (hosts.stream().noneMatch(known -> known.name().equals(host.name()))) {
                    hosts.add(host);
                }
            }
        }
        return hosts;
    }

    private static List<String> expandRange(String text) {
        java.util.regex.Matcher range = java.util.regex.Pattern.compile("\\[(\\d+)-(\\d+)]").matcher(text);
        if (!range.find()) {
            return List.of(text);
        }
        int from = Integer.parseInt(range.group(1));
        int to = Integer.parseInt(range.group(2));
        if (to < from || to - from > 999) {
            return List.of(text);
        }
        // "01" keeps its leading zero: the numbers are as wide as the first one is written
        String format = range.group(1).startsWith("0") ? "%0" + range.group(1).length() + "d" : "%d";
        List<String> result = new java.util.ArrayList<>();
        for (int i = from; i <= to; i++) {
            String number = String.format(format, i);
            result.addAll(expandRange(text.substring(0, range.start()) + number + text.substring(range.end())));
        }
        return result;
    }

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
            return who + hosts.stream().map(Host::name).collect(Collectors.joining(", "));
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
