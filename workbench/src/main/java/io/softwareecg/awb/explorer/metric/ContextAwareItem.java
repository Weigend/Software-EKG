package io.softwareecg.awb.explorer.metric;

import io.softwareecg.awb.explorer.icons.TreeIcons;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.projectcontext.model.Project;

/**
 * The base tree item class of all time series related tree items that
 * defines the context like the concrete project and additional filters
 * a series items belongs to.
 *
 * @since 19.08.2018
 */
public abstract class ContextAwareItem extends RepositoryBaseItem {

    private final FilterContext filterContext;

    /**
     * Creates a new ContextAwareItem item based on the given context and the project
     *
     * @param label the name of the tree item that will used as displayed label in the view
     * @param context bean that define filters that relating to series data or the view flavor and control the item rendering
     */
    public ContextAwareItem(String label, FilterContext context) {
        super(label, context.getRepository());
        this.filterContext = context;
    }

    /**
     * Returns the context with different kind of filter that relating to the
     * series data or the view flavor the user has selected.
     *
     * @return the context bean with the filter and post compute settings
     */
    public FilterContext getFilterContext() {
        return filterContext;
    }

    /**
     * Returns the project this explorer tree item belongs to.
     * This project is the context for all sub nodes in the tree.
     *
     * @return the project that is the data base of all sub nodes
     */
    public Project getProject() {
        return getProject(filterContext);
    }

    /**
     * Helper method that extract the project from the given filter
     * context to write less clue code in deriving classes.
     *
     * @param context the context bean with the filter (incl. the project) and post compute settings
     * @return the project extract from the context or null if no project is inside (should never be the case)
     */
    public static Project getProject(FilterContext context) {
        return context.getQueryParameter().getProject();
    }

    /**
     * @return the icons of the tree
     */
    protected TreeIcons getIconProvider() {
        return TreeIcons.DEFAULT;
    }
}
