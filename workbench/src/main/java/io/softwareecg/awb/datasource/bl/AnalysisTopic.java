package io.softwareecg.awb.datasource.bl;

import java.util.List;

/**
 * What an analysis is about, as the user thinks of it: "my Linux servers", "my Java processes".
 * A topic is the first thing chosen for a new analysis - before any address or technology.
 * <p>
 * A topic is a template. It says which systems of an environment belong to it and what their
 * essential metrics are; where the data comes from (a Prometheus server today) is a separate
 * question. Topics that have no template yet are listed all the same, marked as not
 * {@link #isAvailable() available}: they show what the analysis is meant to cover.
 */
public enum AnalysisTopic {

    LINUX(Category.INFRASTRUCTURE, "Linux / Unix", "Servers: CPU, load, memory, disk, network", true),

    WINDOWS(Category.INFRASTRUCTURE, "Windows Server", "Servers: CPU, memory, disk, network", false),

    KUBERNETES(Category.INFRASTRUCTURE, "Kubernetes", "Clusters: namespaces, pods, containers", false),

    JVM(Category.APPLICATIONS, "Java / JVM", "Processes: heap, garbage collection, threads", true),

    DATABASES(Category.APPLICATIONS, "Databases", "PostgreSQL, MySQL, Oracle ...", false),

    SERVICES(Category.APPLICATIONS, "Services", "Requests, errors, latencies", false);

    /**
     * The two kinds of topics, shown as two rows of choices.
     */
    public enum Category {

        INFRASTRUCTURE("Infrastructure"),

        APPLICATIONS("Applications");

        private final String title;

        Category(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final Category category;

    private final String title;

    private final String description;

    private final boolean available;

    AnalysisTopic(Category category, String title, String description, boolean available) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.available = available;
    }

    public Category category() {
        return category;
    }

    /**
     * @return the name of the topic as shown to the user
     */
    public String title() {
        return title;
    }

    /**
     * @return one line on what the topic covers
     */
    public String description() {
        return description;
    }

    /**
     * @return {@code false} for a topic that is planned but cannot be chosen yet
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * One way to collect the topic.
     *
     * @param source      where the data comes from
     * @param description what this way needs and gives for this topic, in a sentence or two
     */
    public record SourceOption(AnalysisSource source, String description) {
    }

    /**
     * @return the ways this topic can be collected, the usual one first. Ways that are planned
     *         but not built are part of the list ({@link AnalysisSource#isAvailable()}).
     */
    public List<SourceOption> sources() {
        return switch (this) {
            case LINUX -> List.of(
                    new SourceOption(AnalysisSource.PROMETHEUS,
                            "A server already collects from all hosts (node exporter). "
                                    + "The hosts are found for you, their history comes along."),
                    new SourceOption(AnalysisSource.SSH_TOP,
                            "Each host is read with top. Needs only an SSH login, no Prometheus. "
                                    + "Shows single processes too. No history."));
            case JVM -> List.of(
                    new SourceOption(AnalysisSource.PROMETHEUS,
                            "Metrics of Micrometer or the JMX exporter, through a server. "
                                    + "The processes are found for you, their history comes along."),
                    new SourceOption(AnalysisSource.JOLOKIA, "Each JVM is read directly over HTTP."),
                    new SourceOption(AnalysisSource.JMX, "Each JVM is read directly over JMX."));
            case WINDOWS -> List.of(
                    new SourceOption(AnalysisSource.PROMETHEUS, "One server that collects from all hosts (windows exporter)."),
                    new SourceOption(AnalysisSource.WINDOWS_COUNTERS, "Each host is read directly."));
            case DATABASES -> List.of(
                    new SourceOption(AnalysisSource.PROMETHEUS, "One server that collects from the database exporters."),
                    new SourceOption(AnalysisSource.POSTGRES, "Each database is asked directly."));
            case KUBERNETES, SERVICES -> List.of(
                    new SourceOption(AnalysisSource.PROMETHEUS, "One server that collects from the cluster."));
        };
    }
}
