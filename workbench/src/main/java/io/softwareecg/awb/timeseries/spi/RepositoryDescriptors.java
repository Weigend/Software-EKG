package io.softwareecg.awb.timeseries.spi;

import java.net.URI;
import java.util.regex.Pattern;

/** Built-in descriptor factories kept with their technology providers, not in the repository UI. */
public final class RepositoryDescriptors {
    private static final Pattern ZOOKEEPER = Pattern.compile(
            "^([a-z0-9][a-z0-9.-]*:[1-9][0-9]{1,4})(,[a-z0-9][a-z0-9.-]*:[1-9][0-9]{1,4})*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICKHOUSE = Pattern.compile(
            "^jdbc:c(h|lickhouse)://[a-zA-Z0-9._\\-]+(:[1-9][0-9]{1,4})?"
                    + "(,[a-zA-Z0-9._\\-]+(:[1-9][0-9]{1,4})?)*"
                    + "(/[a-zA-Z0-9_]*)?(\\?.*)?$", Pattern.CASE_INSENSITIVE);

    private RepositoryDescriptors() {}

    public static RepositoryTypeDescriptor solrStandalone() {
        return descriptor("SOLR_STANDALONE", "Solr (Classic)", "icons/repository-db-type-solr.svg",
                "Repository URL:", "Absolute Solr URL including core name", "Index Name:", "", true,
                RepositoryDescriptors::httpUrl);
    }

    public static RepositoryTypeDescriptor solrCloud() {
        return descriptor("SOLR_CLOUD", "Solr Cloud", "icons/repository-db-type-solr-cloud.svg",
                "ZooKeeper Host/Port:", "host:port,host:port", "Collection:", "Solr collection", false,
                value -> ZOOKEEPER.matcher(value).matches());
    }

    public static RepositoryTypeDescriptor embeddedSolr() {
        return new RepositoryTypeDescriptor("SOLR_EMBEDDED", "Solr Embedded",
                "icons/repository-db-type-solr.svg", false, "Repository URL:", "", "Index Name:", "", true,
                value -> true);
    }

    public static RepositoryTypeDescriptor elasticsearch() {
        return descriptor("ELASTICSEARCH_STANDALONE", "Elasticsearch",
                "icons/repository-db-type-elasticsearch.svg", "Repository URL:",
                "Elasticsearch URL including index", "Index Name:", "Index", false,
                RepositoryDescriptors::httpUrl);
    }

    public static RepositoryTypeDescriptor clickhouse() {
        return descriptor("CLICKHOUSE_CLOUD", "ClickHouse Cloud", "icons/repository-db-type-clickhouse.svg",
                "ClickHouse JDBC URL:", "jdbc:clickhouse://node1:8123/database", "Table Name:",
                "Optional table name", false,
                value -> CLICKHOUSE.matcher(value).matches());
    }

    private static RepositoryTypeDescriptor descriptor(String id, String name, String icon,
                                                       String connectionLabel, String connectionPrompt,
                                                       String indexLabel, String indexPrompt,
                                                       boolean indexDisabled,
                                                       java.util.function.Predicate<String> validator) {
        return new RepositoryTypeDescriptor(id, name, icon, true, connectionLabel, connectionPrompt,
                indexLabel, indexPrompt, indexDisabled, validator);
    }

    private static boolean httpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
