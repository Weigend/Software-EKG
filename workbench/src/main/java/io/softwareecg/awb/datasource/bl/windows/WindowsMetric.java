package io.softwareecg.awb.datasource.bl.windows;

/**
 * What the Windows performance counters tell about a host and its processes, under the names
 * the series get in an analysis. As for every source, the names do not say where the values
 * come from: the CPU usage of a host is {@code cpu_usage_percent} whether it is a Windows
 * counter, the output of {@code top} or a metric of a Prometheus server.
 * <p>
 * The counters are written with their English names. The collector reads them on a Windows of
 * every language.
 * <p>
 * Nor do the names say whose values they are - that is what the process of a series is for. The
 * values of the host stand under the process {@code system}, those of a watched process under
 * its name. As Windows reports it, the CPU of a process is counted in percent of one core,
 * that of the host in percent of all of them.
 */
public enum WindowsMetric {

    CPU_USAGE(Scope.SYSTEM, "cpu_usage_percent", "\\Processor(_Total)\\% Processor Time", true, false),
    CPU_USER(Scope.SYSTEM, "cpu_user_percent", "\\Processor(_Total)\\% User Time", true, false),
    CPU_SYSTEM(Scope.SYSTEM, "cpu_system_percent", "\\Processor(_Total)\\% Privileged Time", true, false),
    CPU_QUEUE(Scope.SYSTEM, "cpu_queue_length", "\\System\\Processor Queue Length", true, false),
    MEMORY_COMMITTED(Scope.SYSTEM, "memory_committed_percent", "\\Memory\\% Committed Bytes In Use", true, false),
    MEMORY_AVAILABLE(Scope.SYSTEM, "memory_available_bytes", "\\Memory\\Available Bytes", true, false),
    MEMORY_PAGES(Scope.SYSTEM, "memory_pages_per_second", "\\Memory\\Pages/sec", false, false),
    DISK_BUSY(Scope.SYSTEM, "disk_busy_percent", "\\PhysicalDisk(_Total)\\% Disk Time", true, false),
    DISK_READ(Scope.SYSTEM, "disk_read_bytes_per_second", "\\PhysicalDisk(_Total)\\Disk Read Bytes/sec", true, false),
    DISK_WRITTEN(Scope.SYSTEM, "disk_written_bytes_per_second", "\\PhysicalDisk(_Total)\\Disk Write Bytes/sec", true, false),
    DISK_QUEUE(Scope.SYSTEM, "disk_queue_length", "\\PhysicalDisk(_Total)\\Current Disk Queue Length", false, false),
    DISK_FREE(Scope.SYSTEM, "disk_free_percent", "\\LogicalDisk(_Total)\\% Free Space", false, false),
    // Windows has no sum over the network interfaces: one series for each of them
    NETWORK_RECEIVE(Scope.SYSTEM, "network_receive_bytes_per_second", "\\Network Interface(*)\\Bytes Received/sec", true, true),
    NETWORK_TRANSMIT(Scope.SYSTEM, "network_transmit_bytes_per_second", "\\Network Interface(*)\\Bytes Sent/sec", true, true),
    TASKS_TOTAL(Scope.SYSTEM, "tasks_total", "\\System\\Processes", false, false),
    THREADS_TOTAL(Scope.SYSTEM, "threads_total", "\\System\\Threads", false, false),

    PROCESS_CPU(Scope.PROCESS, "cpu_usage_percent", "\\Process(${REF:procName})\\% Processor Time", true, false),
    PROCESS_MEMORY(Scope.PROCESS, "memory_used_bytes", "\\Process(${REF:procName})\\Working Set", true, false),
    PROCESS_PRIVATE(Scope.PROCESS, "memory_private_bytes", "\\Process(${REF:procName})\\Private Bytes", false, false),
    PROCESS_THREADS(Scope.PROCESS, "threads_total", "\\Process(${REF:procName})\\Thread Count", false, false),
    PROCESS_HANDLES(Scope.PROCESS, "handles_total", "\\Process(${REF:procName})\\Handle Count", false, false);

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

    private final boolean perInstance;

    WindowsMetric(Scope scope, String seriesName, String counter, boolean essential, boolean perInstance) {
        this.scope = scope;
        this.seriesName = seriesName;
        this.counter = counter;
        this.essential = essential;
        this.perInstance = perInstance;
    }

    public Scope scope() {
        return scope;
    }

    /**
     * @return the name of the series in the analysis; for a metric with one series
     *         {@link #isPerInstance() for each instance} the name they all begin with
     */
    public String seriesName() {
        return seriesName;
    }

    /**
     * @return the Windows performance counter the values are read from, with its English names
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

    /**
     * @return {@code true} if the counter stands for several - one for every network interface -
     *         and each becomes a series of its own, named with its instance
     */
    public boolean isPerInstance() {
        return perInstance;
    }
}
