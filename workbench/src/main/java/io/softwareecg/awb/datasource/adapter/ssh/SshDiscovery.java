package io.softwareecg.awb.datasource.adapter.ssh;

import io.softwareecg.collector.source.unix.UnixSourceConfig;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;
import io.softwareecg.collector.source.unix.executor.local.SyncLocalExecutor;
import io.softwareecg.collector.source.unix.executor.ssh.SyncSshExecutor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks at hosts before anything is collected from them: logs in the way the collector will,
 * runs {@code top} once and tells whether that worked and which processes are running. This is
 * what a discovery is for a source that has no inventory - it answers "can I read these hosts,
 * and what is there to watch?".
 */
public final class SshDiscovery {

    /** The command the collector's Unix source runs as well. */
    private static final String TOP_COMMAND = "env COLUMNS=512 top -c -n 1 -b";

    private static final int MAX_PARALLEL = 8;

    private static final long TIMEOUT_SECONDS = 20;

    private static final int MAX_CANDIDATES = 12;

    private static final Pattern DEFAULT_PORT = Pattern.compile("^(.+):(\\d+)$");

    private SshDiscovery() {
    }

    /**
     * What was found on one host.
     *
     * @param host      the host
     * @param reachable whether {@code top} could be run on it
     * @param problem   why not, {@code null} if it could
     * @param processes the processes running there, the busiest first
     */
    public record HostCheck(SshPlan.Host host, boolean reachable, String problem, List<ProcessCandidate> processes) {
    }

    /**
     * A program running on a host, as a candidate to watch.
     *
     * @param name          the name of the program, without path and arguments
     * @param instances     how many processes of it are running
     * @param cpuPercent    the CPU they use together
     * @param memoryPercent the memory they use together
     */
    public record ProcessCandidate(String name, int instances, double cpuPercent, double memoryPercent) {
    }

    /**
     * Looks at all hosts, several at a time.
     *
     * @param username the user to log in with, or {@code null}
     * @param password the password, or {@code null} to log in with the SSH key
     * @return one result per host, in the order given
     */
    public static List<HostCheck> inspect(List<SshPlan.Host> hosts, String username, String password) {
        if (hosts.isEmpty()) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(MAX_PARALLEL, hosts.size()), runnable -> {
            Thread thread = new Thread(runnable, "inspect-host");
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Future<HostCheck>> running = new ArrayList<>();
            for (SshPlan.Host host : hosts) {
                running.add(pool.submit(() -> inspect(host, username, password)));
            }
            List<HostCheck> result = new ArrayList<>();
            for (int i = 0; i < hosts.size(); i++) {
                try {
                    result.add(running.get(i).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.add(new HostCheck(hosts.get(i), false, "interrupted", List.of()));
                } catch (Exception e) {
                    running.get(i).cancel(true);
                    result.add(new HostCheck(hosts.get(i), false,
                            e instanceof java.util.concurrent.TimeoutException ? "no answer within " + TIMEOUT_SECONDS + " s"
                                    : rootMessage(e), List.of()));
                }
            }
            return result;
        } finally {
            pool.shutdownNow();
        }
    }

    private static HostCheck inspect(SshPlan.Host host, String username, String password) {
        UnixSourceConfig config = new UnixSourceConfig();
        Matcher port = DEFAULT_PORT.matcher(host.address());
        config.setHost(port.matches() ? port.group(1) : host.address());
        config.setPort(port.matches() ? Integer.parseInt(port.group(2)) : UnixSourceConfig.DEFAULT_SSH_PORT);
        config.setUsername(blankToNull(username));
        config.setPassword(blankToNull(password));

        ClosableSshExecutor ssh = host.isLocal() ? null : new ClosableSshExecutor();
        CommandExecutor executor = ssh != null ? ssh : new SyncLocalExecutor();
        try {
            String output = executor.executeCommand(TOP_COMMAND, config);
            List<ProcessCandidate> processes = parseProcesses(output);
            if (processes == null) {
                return new HostCheck(host, false, "the host answered, but not with the output of top", List.of());
            }
            return new HostCheck(host, true, null, processes);
        } catch (Exception e) {
            return new HostCheck(host, false, rootMessage(e), List.of());
        } finally {
            if (ssh != null) {
                ssh.close();
            }
        }
    }

    /**
     * Reads the processes from the output of {@code top}: grouped by program, the busiest first.
     *
     * @return the candidates, or {@code null} if the text is not the output of top
     */
    static List<ProcessCandidate> parseProcesses(String output) {
        String[] lines = output == null ? new String[0] : output.split("\\R");
        int header = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("PID ")) {
                header = i;
                break;
            }
        }
        if (header < 0) {
            return null;
        }

        Map<String, double[]> byName = new LinkedHashMap<>();    // instances, cpu, memory
        for (int i = header + 1; i < lines.length; i++) {
            String[] columns = lines[i].trim().split("\\s+");
            if (columns.length < 12) {
                continue;
            }
            String name = programOf(columns[11]);
            // kernel threads, and top itself, are nothing to watch
            if (name.isEmpty() || name.startsWith("[") || name.equals("top") || name.equals("env")) {
                continue;
            }
            double[] sums = byName.computeIfAbsent(name, key -> new double[3]);
            sums[0]++;
            sums[1] += number(columns[8]);
            sums[2] += number(columns[9]);
        }

        List<ProcessCandidate> candidates = new ArrayList<>();
        byName.forEach((name, sums) -> candidates.add(new ProcessCandidate(name, (int) sums[0], sums[1], sums[2])));
        candidates.sort(Comparator.comparingDouble((ProcessCandidate candidate) ->
                candidate.cpuPercent() + candidate.memoryPercent()).reversed().thenComparing(ProcessCandidate::name));
        return candidates.size() > MAX_CANDIDATES ? new ArrayList<>(candidates.subList(0, MAX_CANDIDATES)) : candidates;
    }

    /** The program of a command line: {@code java} for {@code /usr/bin/java}, {@code sshd} for {@code sshd:}. */
    private static String programOf(String command) {
        String program = command;
        int slash = program.lastIndexOf('/');
        if (slash >= 0 && !program.startsWith("[")) {
            program = program.substring(slash + 1);
        }
        return program.endsWith(":") ? program.substring(0, program.length() - 1) : program;
    }

    private static double number(String text) {
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    /** Why a host could not be read, in words for the person who named it. */
    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof java.nio.channels.UnresolvedAddressException || root instanceof java.net.UnknownHostException) {
            return "there is no host of this name";
        }
        if (root instanceof java.net.ConnectException) {
            return "no SSH server answers there" + (root.getMessage() != null ? " (" + root.getMessage() + ")" : "");
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    /** The collector's SSH executor keeps its connection; a single look at a host must not. */
    private static final class ClosableSshExecutor extends SyncSshExecutor {

        void close() {
            if (sshConnection != null) {
                try {
                    sshConnection.close();
                } catch (Exception e) {
                    // nothing to do about a connection that does not close
                }
                sshConnection = null;
            }
        }
    }
}
