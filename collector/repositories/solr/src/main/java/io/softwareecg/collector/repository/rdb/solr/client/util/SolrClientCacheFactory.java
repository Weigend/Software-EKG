package io.softwareecg.collector.repository.rdb.solr.client.util;

import org.apache.solr.client.solrj.io.SolrClientCache;

/**
 * Factory for {@link SolrClientCache}s.
 *
 * @since 27.02.2018
 */
public class SolrClientCacheFactory {

    /**
     * Creates and returns a new {@link SolrClientCache}.
     *
     * @return the {@link SolrClientCache}.
     */
    public SolrClientCache create() {
        return new SolrClientCache();
    }
}
