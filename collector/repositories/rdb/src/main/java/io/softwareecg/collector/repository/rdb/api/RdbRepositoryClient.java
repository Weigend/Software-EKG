package io.softwareecg.collector.repository.rdb.api;

import io.softwareecg.collector.repository.rdb.api.querymodel.delete.DeleteParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchResult;

import java.util.Collection;

/**
 * Interface that represents various client proxies that can use to query,
 * write and delete data at EKG remote database (RDB) repositories.
 * This interface will implement by client like the Solr client that is used
 * by the collector to handle Solr Classic/Solr Cloud based repositories.
 *
 * @since 28.10.2018
 */
public interface RdbRepositoryClient {

    /**
     * Searches for entities and returns them as Collection. Use this method for searches with
     * {@link SearchParams#maxRows} smaller than or equal to {@link SearchParams#MAX_ROWS_LIMIT}.
     *
     * @param type         the {@link Class} of the entities
     * @param searchParams the search parameters
     * @param <T>          the type of the entities
     * @return the entities as Collection plus meta data
     * @throws RdbRepositoryException on errors accessing the search index
     */
    <T> SearchResult<T> search(Class<T> type, SearchParams searchParams) throws RdbRepositoryException;

    /**
     * Adds entities to the search index. Will only be visible after a {@link #commit()}.
     *
     * @param entities the entities
     * @throws RdbRepositoryException on errors accessing the search index
     */
    void add(Collection<TimeSeries> entities) throws RdbRepositoryException;

    /**
     * Adds (persists) a single TimeSeries entity to the EKG repository
     * without executing an commit.
     *
     * @param entity the TimeSeries entity that should stored in the repository.
     * @throws RdbRepositoryException thrown than an error occur during the update process
     */
    void add(TimeSeries entity) throws RdbRepositoryException;

    /**
     * Delete every records from the EKG repository that matches to the filters
     * defined by the given {@link DeleteParams} instance.
     *
     * @param deleteParams a DeleteParams bean that can contain various FilterExpression to selected the records to delete
     * @throws RdbRepositoryException thrown than an error occur during the update process
     */
    void delete(DeleteParams deleteParams) throws RdbRepositoryException;

    /**
     * Commits the current transaction.
     * <p>
     * Note on transactions: If the search index does only support "global" transactions like SolrCloud, the commit will
     * apply all changes of all threads/processes/cluster nodes. To avoid unexpected behaviour, it is mandatory to
     * only allow write access to one thread in one process in one cluster node at the same time.
     *
     * @throws RdbRepositoryException on errors accessing the search index
     */
    void commit() throws RdbRepositoryException;

    /**
     * Closes the {@link RdbRepositoryClient}.
     *
     * @throws RdbRepositoryException on errors accessing the search index
     */
    void close() throws RdbRepositoryException;
}
