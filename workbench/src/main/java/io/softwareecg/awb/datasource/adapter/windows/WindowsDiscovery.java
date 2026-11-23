package io.softwareecg.awb.datasource.adapter.windows;

import io.softwareecg.awb.datasource.adapter.ssh.SshPlan;
import io.softwareecg.collector.source.wincount.WindowsCounterProbe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Looks at Windows hosts before anything is collected from them: reads their performance
 * counters the way the collector will, and tells whether that worked and which processes are
 * running. It answers "can I read these hosts, and what is there to watch?".
 */
public final class WindowsDiscovery {

    /** The counter that names the processes of a host; it has a value without waiting for a second look. */
    private static final String PROCESS_MEMORY = "\\Process(*)\\Working Set";

    private static final int MAX_PARALLEL = 4;

    private static final long TIMEOUT_SECONDS = 30;

    private static final int MAX_CANDIDATES = 12;

    private WindowsDiscovery() {
    }

    /**
     * What was found on one host.
     *
     * @param host      the host
     * @param reachable whether its counters could be read
     * @param problem   why not, {@code null} if they could
     * @param processes the processes running there, the biggest first
     */
    public record HostCheck(SshPlan.Host host, boolean reachable, String problem, List<ProcessCandidate> processes) {
    }

    /**
     * A program running on a host, as a candidate to watch.
     *
     * @param name        the name Windows gives the program, without {@code .exe}
     * @param instances   how many processes of it are running
     * @param memoryBytes the memory they use together
     */
    public record ProcessCandidate(String name, int instances, double memoryBytes) {
    }

    /**
     * Says why Windows performance counters cannot be read by this AWB.
     *
     * @return the reason in words for the user, {@code null} if they can be read
     */
    public static String problem() {
        if (!System.getProperty("os.name", "").startsWith("Win")) {
            return "Windows performance counters are read by a workbench that runs on Windows.";
        }
        if (!WindowsCounterProbe.isAvailable()) {
            return "The library that reads the performance counters (wpcaccess) is missing in this installation.";
        }
        return null;
    }

    /**
     * Looks at all hosts, several at a time.
     *
     * @return one result per host, in the order given
     */
    public static List<HostCheck> inspect(List<SshPlan.Host> hosts) {
        if (hosts.isEmpty()) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(MAX_PARALLEL, hosts.size()), runnable -> {
            Thread thread = new Thread(runnable, "inspect-windows-host");
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Future<HostCheck>> running = new ArrayList<>();
            for (SshPlan.Host host : hosts) {
                running.add(pool.submit(() -> inspect(host)));
            }
            List<HostCheck> result = new ArrayList<>();
            for (int i = 0; i < hosts.size(); i++) {
                try {
                    result.add(running.get(i).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.add(new HostCheck(hosts.get(i), false, "interrupted", List.of()));
                } catch (TimeoutException e) {
                    result.add(new HostCheck(hosts.get(i), false, "no answer within " + TIMEOUT_SECONDS + " s", List.of()));
                } catch (Exception e) {
                    result.add(new HostCheck(hosts.get(i), false, rootMessage(e), List.of()));
                }
            }
            return result;
        } finally {
            pool.shutdownNow();
        }
    }

    private static HostCheck inspect(SshPlan.Host host) {
        try {
            List<String> counters = WindowsCounterProbe.expand(host.address(), PROCESS_MEMORY);
            if (counters.isEmpty()) {
                return new HostCheck(host, false, host.isLocal()
                        ? "its performance counters show no processes"
                        : "its performance counters cannot be read from here - is it a Windows host with the"
                        + " service 'Remote Registry' running, and may your user read its counters?", List.of());
            }
            return new HostCheck(host, true, null, candidates(counters, WindowsCounterProbe.readPaths(counters)));
        } catch (RuntimeException e) {
            return new HostCheck(host, false, rootMessage(e), List.of());
        }
    }

    /**
     * Groups the processes of a host by program, the biggest first.
     *
     * @param counters the counters of the memory of each process, as the source names them:
     *                 {@code \\host\Process(java#1)\Working Set}
     * @param memory   the value of each counter
     */
    static List<ProcessCandidate> candidates(List<String> counters, double[] memory) {
        Map<String, double[]> byName = new LinkedHashMap<>();    // instances, memory
        for (int i = 0; i < counters.size(); i++) {
            String name = programOf(WindowsCounterProbe.instanceOf(counters.get(i)));
            // the sum of all processes, and the idle process, are nothing to watch
            if (name.isEmpty() || name.equals("_Total") || name.equals("Idle")) {
                continue;
            }
            double[] sums = byName.computeIfAbsent(name, key -> new double[2]);
            sums[0]++;
            sums[1] += Math.max(0, i < memory.length ? memory[i] : 0);
        }
        List<ProcessCandidate> candidates = new ArrayList<>();
        byName.forEach((name, sums) -> candidates.add(new ProcessCandidate(name, (int) sums[0], sums[1])));
        candidates.sort(Comparator.comparingDouble(ProcessCandidate::memoryBytes).reversed()
                .thenComparing(ProcessCandidate::name));
        return candidates.size() > MAX_CANDIDATES ? new ArrayList<>(candidates.subList(0, MAX_CANDIDATES)) : candidates;
    }

    /** The program of an instance: {@code java} for {@code java#1}, the second process of it. */
    private static String programOf(String instance) {
        if (instance == null) {
            return "";
        }
        int number = instance.lastIndexOf('#');
        boolean numbered = number > 0 && number < instance.length() - 1
                && instance.substring(number + 1).chars().allMatch(Character::isDigit);
        return numbered ? instance.substring(0, number) : instance;
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
