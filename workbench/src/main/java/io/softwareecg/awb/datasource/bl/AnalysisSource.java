package io.softwareecg.awb.datasource.bl;

/**
 * A way the data of a topic can come into an analysis. Most topics can be collected in more
 * than one way - Linux hosts through a Prometheus server that already gathered their values, or
 * directly from each host. The ways differ in what they need and what they give, which is why
 * the choice is the user's:
 * <ul>
 *   <li>an <b>aggregating</b> source is one address behind which many systems stand; it knows
 *       them, so they are found rather than listed, and it usually keeps their history;</li>
 *   <li>a <b>direct</b> source is read system by system; the systems have to be named, nothing
 *       has to be running besides them, and there are only the values collected from now on.</li>
 * </ul>
 */
public enum AnalysisSource {

    PROMETHEUS("Prometheus", true, true),

    SSH_TOP("SSH / top", false, true),

    JOLOKIA("Jolokia", false, false),

    JMX("JMX", false, false),

    WINDOWS_COUNTERS("Performance Counters", false, true),

    POSTGRES("PostgreSQL", false, false);

    private final String title;

    private final boolean aggregating;

    private final boolean available;

    AnalysisSource(String title, boolean aggregating, boolean available) {
        this.title = title;
        this.aggregating = aggregating;
        this.available = available;
    }

    public String title() {
        return title;
    }

    /**
     * @return {@code true} if the source is one address that holds many systems, {@code false}
     *         if every system is read by itself
     */
    public boolean isAggregating() {
        return aggregating;
    }

    /**
     * @return {@code false} for a source that is planned but cannot be chosen yet
     */
    public boolean isAvailable() {
        return available;
    }
}
