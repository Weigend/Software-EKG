package io.softwareecg.awb.repository.model;

import io.softwareecg.awb.runtime.NamedEnum;

/**
 * The currently available types implementations.
 *
 */
public enum EkgRepositoryDbType implements NamedEnum {

    /**
     * An AWB embedded types.
     */
    SOLR_EMBEDDED("Solr Embedded"),

    /**
     * A zookeeper managed solr cluster.
     */
    SOLR_CLOUD("Solr Cloud"),

    /**
     * A remote solr server.
     */
    SOLR_STANDALONE("Solr (Classic)"),

    /**
     * A remote instance of ElasticSearch
     */
    ELASTICSEARCH_STANDALONE("ElasticSearch Standalone"),

    /**
     * A remote ClickHouse cluster for high-performance time series storage.
     */
    CLICKHOUSE_CLOUD("ClickHouse Cloud");

    private final String name;

    EkgRepositoryDbType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
