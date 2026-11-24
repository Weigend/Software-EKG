package io.softwareecg.awb.explorer.metric;

import io.softwareecg.awb.bookmark.api.MetricsBookmarkService;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.query.api.QuerySpec;
import io.softwareecg.awb.projectcontext.api.ProjectViewFlavor;
import io.softwareecg.wfx.lookup.api.Lookup;

import java.util.Objects;

/**
 * A bean that define the context with different kind of filter
 * that relating to the series data or the view flavor the user
 * has selected.
 *
 * @since 19.08.2018
 */
public class FilterContext {

    /**
     * A query param instance that stores all filters that are used
     * to define a branch in the tree.
     * Example: a filter set with project X, host Y and process Z but * on measurement and metric groups
     * to define nodes displayed as child of a process Z that is a child of host Y that belongs to project X.
     */
    private final QuerySpec queryParams;

    /**
     * The project view flavor that is currently in use (logical view vs. physical view)
     * Depending on the flavor items will rendered different or removed/added.
     */
    private final ProjectViewFlavor activeViewFlavor;

    /**
     * The repository the data of this branch of the tree comes from.
     */
    private final Repository repository;

    /**
     * Constructs a new instance of FilterContext
     *
     * @param queryParams the filter parameters that are active the for tree item
     * @param activeViewFlavor the active view flavor of the node
     * @param repository the repository the data comes from
     */
    public FilterContext(QuerySpec queryParams, ProjectViewFlavor activeViewFlavor, Repository repository) {
        this.queryParams = queryParams;
        this.activeViewFlavor = activeViewFlavor;
        this.repository = repository;
    }

    /**
     * Returns the filter context that all child nodes
     * has to consider.
     *
     * @return the context as QuerySpec instance that can use for filter queries and more
     */
    public QuerySpec getQueryParameter() {
        return queryParams;
    }

    /**
     * Returns the active view flavor of the node
     *
     * @return the view flavor that is currently in use
     */
    public ProjectViewFlavor getActiveViewFlavor() {
        return activeViewFlavor;
    }

    /**
     * Clones and returns the FilterContext instance and set the given
     * query parameters to the cloned instance.
     *
     * @param newQueryParams the QuerySpec that should used for the cloned context.
     * @return the cloned context
     */
    public FilterContext cloneWith(QuerySpec newQueryParams) {
        return new FilterContext(newQueryParams, activeViewFlavor, repository);
    }


    /**
     * @return the repository the data of this branch of the tree comes from
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * @return what the repository holds: hosts, processes, metrics; {@code null} if it cannot be reached
     */
    public MetricDataAccessService metrics() {
        return repository.getBoundedService(MetricDataAccessService.class);
    }

    /**
     * @return the bookmarks; they are stored in the workbench, so they are there without a connection
     */
    public MetricsBookmarkService bookmarks() {
        return Lookup.lookup(MetricsBookmarkService.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterContext context = (FilterContext) o;
        return Objects.equals(queryParams, context.queryParams) &&
                activeViewFlavor == context.activeViewFlavor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryParams, activeViewFlavor);
    }
}
