package io.softwareecg.collector.repository.rdb.api;

import io.softwareecg.collector.repository.rdb.api.querymodel.admin.CollectionProperties;

import java.util.Collection;

/**
 * This interface represents proxy clients for remote databases like
 * Apache Solr or ElasticSearch. The this admin proxy provide a simple small API to
 * execute low level administration requests.
 *
 * @since 28.10.2018
 */
public interface RdbRepositoryAdmin {

    /**
     * Create a an new index in the remote database with the EKG v6.1 schema
     * as strict data model.
     * The implementation doesn't check if the index already exists. If it does
     * an RdbRepositoryException will thrown.
     *
     * @param indexName the name of the index/collection that should created
     * @return a boolean flag that indicates if the index was created successfully or not.
     */
    boolean createEkgRepository(String indexName) throws RdbRepositoryException;

    /**
     * Returns the {@link CollectionProperties}s for all existing collections.
     *
     * @return the {@link CollectionProperties}s of all existing collections
     * @throws RdbRepositoryException on errors accessing the search index
     */
    Collection<CollectionProperties> listRepositories() throws RdbRepositoryException;

}
