package io.softwareecg.awb.datasource.model;

/**
 * Where a collected series stands in the dimensions of the EKG, whatever source it came from.
 * Series of similar systems line up for comparison because they differ in the host only.
 *
 * @param hostGroup    the group of systems
 * @param host         the system
 * @param processGroup the group of processes
 * @param process      the process
 * @param metricGroup  the group of the metric
 * @param metric       the metric
 */
public record SeriesDimensions(String hostGroup, String host, String processGroup, String process,
                               String metricGroup, String metric) {

    /**
     * The group a metric belongs to by its name: the part before the first underscore
     * ({@code cpu} for {@code cpu_usage_percent}). The same rule for every source, so that a
     * metric of the same name stands at the same place wherever it was collected.
     */
    public static String metricGroupOf(String metric) {
        int underscore = metric.indexOf('_');
        return underscore > 0 ? metric.substring(0, underscore) : metric;
    }
}
