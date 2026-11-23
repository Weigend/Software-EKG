package io.softwareecg.awb.repository.port.query.search;

import io.softwareecg.awb.repository.port.query.status.QueryStatus;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A result of a regular search.
 *
 * @param <T> the type of the returned entities
 */
public record SearchResult<T>(List<T> rows, QueryStatus queryStatus) {

    /**
     * Constructs a new {@link SearchResult}.
     *
     * @param rows        the row
     * @param queryStatus the query status
     */
    public SearchResult(List<T> rows, QueryStatus queryStatus) {
        Validate.notNull(rows);

        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.queryStatus = queryStatus;
    }

    /**
     * Returns the rows.
     *
     * @return the rows
     */
    @Override
    public List<T> rows() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * Returns the Status information of the executed query.
     *
     * @return the query status
     */
    @Override
    public QueryStatus queryStatus() {
        return queryStatus;
    }

    /**
     * Tells whether this search result is empty.
     *
     * @return whether this search result is empty
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Get the actual result size.
     *
     * @return actual result size. Equal to {@code getRows().size()}
     */
    public int size() {
        return rows.size();
    }

}
