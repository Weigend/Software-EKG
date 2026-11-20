package io.softwareecg.collector.source.wincount;

import java.util.ArrayList;
import java.util.List;

/**
 * A look at the Windows performance counters of a machine before anything is collected: can
 * they be read, and what is there? For whoever sets up a collection - the collecting itself is
 * done by {@link WindowsPerformanceCounterSource}.
 * <p>
 * Counters are named as in a collector configuration: without the machine, with English names
 * or with the names of this Windows, e.g. {@code \Processor(_Total)\% Processor Time}.
 */
public final class WindowsCounterProbe {

    private static WindowsPerformanceCounterAccess access;

    private static CounterNames names;

    private WindowsCounterProbe() {
    }

    private static synchronized WindowsPerformanceCounterAccess access() {
        if (access == null) {
            access = new WindowsPerformanceCounterAccess();
            names = new CounterNames(access);
        }
        return access;
    }

    /**
     * @return {@code true} if this is a Windows and the native library of the counters is loaded
     */
    public static boolean isAvailable() {
        return access().isAvailable();
    }

    /**
     * Resolves a counter with wildcards into the counters it stands for.
     *
     * @param host    the name or address of the machine; {@code localhost} is this one
     * @param counter the counter, e.g. {@code \Process(*)\Working Set}
     * @return the counters as they are named on the machine, each with its machine in front;
     *         empty if there is none
     * @throws RuntimeException      if the machine or the object cannot be read
     * @throws IllegalStateException if the counters are not {@link #isAvailable() available}
     */
    public static List<String> expand(String host, String counter) {
        String[] counters = access().resolveWildcards(path(host, counter));
        return counters == null ? List.of() : List.of(counters);
    }

    /**
     * Reads counters once.
     *
     * @param host     the name or address of the machine; {@code localhost} is this one
     * @param counters the counters, without wildcards
     * @return a value for each counter, {@code -1} for one that has none. A counter that is a
     *         rate has no value at the first look.
     * @throws IllegalStateException if the counters are not {@link #isAvailable() available}
     */
    public static double[] read(String host, List<String> counters) {
        List<String> paths = new ArrayList<>();
        for (String counter : counters) {
            paths.add(path(host, counter));
        }
        return readPaths(paths);
    }

    /**
     * Reads counters once.
     *
     * @param paths the counters as {@link #expand(String, String)} returns them
     * @return a value for each counter, {@code -1} for one that has none
     */
    public static double[] readPaths(List<String> paths) {
        WindowsPerformanceCounterAccess wpc = requireAccess();
        double[] values = new double[paths.size()];
        if (paths.isEmpty()) {
            return values;
        }
        long query = wpc.queryCounter(0, paths.toArray(new String[0]), values);
        wpc.deleteQuery(query);
        return values;
    }

    /**
     * @return the instance of a counter - the {@code java} of {@code \\host\Process(java)\Working Set} -
     *         or {@code null} if it has none
     */
    public static String instanceOf(String path) {
        int start = path.indexOf('(');
        int end = path.lastIndexOf(")\\");
        return start < 0 || end < start ? null : path.substring(start + 1, end);
    }

    private static String path(String host, String counter) {
        requireAccess();
        return "\\\\" + WindowsPerformanceCounterSource.machineName(host) + names.localize(counter);
    }

    private static WindowsPerformanceCounterAccess requireAccess() {
        WindowsPerformanceCounterAccess wpc = access();
        if (!wpc.isAvailable()) {
            throw new IllegalStateException("The Windows performance counters are not available here.");
        }
        return wpc;
    }
}
