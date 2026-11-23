package io.softwareecg.awb.datasource.adapter.ssh;

/**
 * What the Unix command {@code top} tells about a host and its processes, under the names the
 * series get in an analysis. The names do not say where the values come from: the CPU usage of
 * a host is {@code cpu_usage_percent} whether it was read by SSH or from a Prometheus server,
 * so charts and bookmarks made for one fit the other.
 * <p>
 * Nor do the names say whose values they are - that is what the process of a series is for.
 * The CPU usage of a watched process has the same name as that of the host, whose values
 * stand under the process {@code system}. Choosing a process in a chart therefore shows the
 * same metric for that process, and choosing all of them compares the processes with each
 * other and with the host. One difference remains, as top reports it: the CPU of a process
 * is counted in percent of one core, that of the host in percent of all of them.
 */
public enum TopMetric {

    CPU_USAGE(Scope.SYSTEM, "cpu_usage_percent", "\\CPU\\used", true),
    CPU_USER(Scope.SYSTEM, "cpu_user_percent", "\\CPU\\us", true),
    CPU_SYSTEM(Scope.SYSTEM, "cpu_system_percent", "\\CPU\\sy", true),
    CPU_IOWAIT(Scope.SYSTEM, "cpu_iowait_percent", "\\CPU\\wa", true),
    LOAD_1M(Scope.SYSTEM, "load_1m", "\\Load\\one", true),
    LOAD_5M(Scope.SYSTEM, "load_5m", "\\Load\\five", false),
    LOAD_15M(Scope.SYSTEM, "load_15m", "\\Load\\fifteen", false),
    MEMORY_USED_PERCENT(Scope.SYSTEM, "memory_used_percent", "\\Memory\\usedPercent", true),
    MEMORY_USED_BYTES(Scope.SYSTEM, "memory_used_bytes", "\\Memory\\used", false),
    SWAP_USED_PERCENT(Scope.SYSTEM, "swap_used_percent", "\\Swap\\usedPercent", true),
    TASKS_RUNNING(Scope.SYSTEM, "tasks_running", "\\Tasks\\running", true),
    TASKS_TOTAL(Scope.SYSTEM, "tasks_total", "\\Tasks\\total", false),

    PROCESS_CPU(Scope.PROCESS, "cpu_usage_percent", "\\Process(${REF:procName})\\CPU", true),
    PROCESS_MEMORY(Scope.PROCESS, "memory_used_percent", "\\Process(${REF:procName})\\MEM", true);

    /**
     * What a metric describes.
     */
    public enum Scope {
        /** The host as a whole. */
        SYSTEM,
        /** One process on the host. */
        PROCESS
    }

    private final Scope scope;

    private final String seriesName;

    private final String counter;

    private final boolean essential;

    TopMetric(Scope scope, String seriesName, String counter, boolean essential) {
        this.scope = scope;
        this.seriesName = seriesName;
        this.counter = counter;
        this.essential = essential;
    }

    public Scope scope() {
        return scope;
    }

    /**
     * @return the name of the series in the analysis
     */
    public String seriesName() {
        return seriesName;
    }

    /**
     * @return the counter of the collector's Unix source the values are read from
     */
    public String counter() {
        return counter;
    }

    /**
     * @return {@code true} if the metric belongs to what is collected when nothing is changed
     */
    public boolean isEssential() {
        return essential;
    }
}
