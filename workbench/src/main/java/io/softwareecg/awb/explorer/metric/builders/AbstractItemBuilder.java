package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ProjectItem;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.explorer.items.RepositoryItem;
import io.softwareecg.awb.timeseries.dimension.NamedValueEntity;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Base class for all ContextAware ItemBuilder.
 * This abstract base class define convenience logic to serve a suitable filter context
 * also if the target TreeItem doesn't define one.
 *
 * @since 19.08.2018
 */
public abstract class AbstractItemBuilder<T extends RepositoryBaseItem> implements
        BiFunction<T, ProgressNotifier, List<? extends AbstractItem>> {

    /**
     * The logger used by the builder to log errors
     */
    private static final Logger LOGGER = EkgLogger.get();

    /* (non-Javadoc)
     * @see java.util.function.BiFunction#apply(T, U)
     * @see io.softwareecg.awb.uitoolkit.tree.api.ItemBuilderRegistry
     */
    @Override
    public List<? extends ContextAwareItem> apply(T parentItem, ProgressNotifier progressNotifier) {

        List<? extends ContextAwareItem> result =
                getChildItemsFor(parentItem, retrieveFilterContext(parentItem), progressNotifier);
        progressNotifier.updateProgress(result.size(), result.size());

        return result;
    }

    /**
     * Build a list of TreeItem instance of a specific type that is derived from ContextAwareItem.
     * The items will assigned to the given parent item  by the caller of the builder. The implementation
     * should consider the filters given by the QuerySpec context at the decision which items to build.
     *
     * @param parentItem the TreeItem that will be parent of the items to build
     * @param parentFilterContext the context that defines filters that restrict which child should be added
     * @param progressNotifier a progress notifier that can use to expose the loading/build state.
     *                         It will set to 100% by the caller.
     *
     * @return a list of TreeItem instances that can add to the specified parent node
     */
    protected abstract List<? extends ContextAwareItem> getChildItemsFor(T parentItem, FilterContext parentFilterContext,
                                                                         ProgressNotifier progressNotifier);

    /**
     * Checks if the given list of entities contains only placeholder values
     * (wildcard "*", dash "-", or blank) and therefore has no meaningful data
     * to display in the explorer tree.
     *
     * @param values the list of entities to check
     * @return true if the list is empty or contains only placeholder values
     */
    protected static boolean hasOnlyPlaceholderValues(List<? extends NamedValueEntity> values) {
        if (values.isEmpty()) {
            return true;
        }
        return values.stream()
                .map(NamedValueEntity::getValueName)
                .allMatch(name -> name == null || name.isBlank() || "*".equals(name) || "-".equals(name));
    }

    /**
     * Tries to retrieve the filter context from the target TreeItem
     * and return it. If the given parent does't define a filter context
     * a new one with suitable filter will created.
     *
     * @param parentItem the TreeItem instance that will used to retrieve the context.
     * @return the retrieved or created context.
     */
    protected FilterContext retrieveFilterContext(RepositoryBaseItem parentItem) {

        FilterContext filterContext;

        if (parentItem instanceof ProjectItem) {
            Project project = ((ProjectItem) parentItem).getProject();

            filterContext = new FilterContext(
                new QuerySpec.Builder().withProject(project).build(),
                ((ProjectItem) parentItem).getActiveViewFlavor(),
                parentItem.getRepository()
            );

        } else if (parentItem instanceof ContextAwareItem) {
            filterContext = ((ContextAwareItem) parentItem).getFilterContext();

        } else if (parentItem instanceof RepositoryItem) {

            filterContext= new FilterContext(
                    new QuerySpec.Builder().build(),
                    ProjectViewFlavor.PHYSICAL_VIEW,
                    parentItem.getRepository()
            );

        } else {
            String errorMsg = "The given parent item of type " + parentItem.getClass().getName()
                    + " isn't supported by this builder!";

            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        return filterContext;
    }
}
