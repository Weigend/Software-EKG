package io.softwareecg.awb.metadata.port;

import java.util.List;

import io.softwareecg.awb.metadata.model.CollectionSourceRow;
/**
 * Stores the data sources of the projects and the history of their state. The sources of a
 * project are deleted together with it.
 */
public interface CollectionSourceStore {

    List<CollectionSourceRow> listAll();

    List<CollectionSourceRow> listByProject(String projectName);

    /**
     * @return the source, or {@code null} if it does not exist (anymore)
     */
    CollectionSourceRow find(String id);

    /**
     * Stores a new source. An id is assigned if the row has none.
     */
    CollectionSourceRow insert(CollectionSourceRow row);

    /**
     * Updates endpoint, configuration, secret reference and the enabled flag of a source.
     *
     * @return {@code false} if the source does not exist
     */
    boolean update(CollectionSourceRow row);

    void delete(String id);

    /**
     * Appends an entry to the history of a source. Only the newest entries are kept.
     */
    void addEvent(String sourceId, String state, String message);

    /**
     * @return the newest entries of the history of a source, newest first
     */
    List<CollectionSourceEvent> listEvents(String sourceId, int limit);
}
