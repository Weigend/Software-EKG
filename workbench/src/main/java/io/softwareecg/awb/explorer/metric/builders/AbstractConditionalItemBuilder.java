package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;

import java.util.Collections;
import java.util.List;

/**
 * Base class for ContextAware ItemBuilder that only create child items for
 * parents than a (from the derived builders) implemented condition results in TRUE.
 *
 * @since 19.08.2018
 */
public abstract class AbstractConditionalItemBuilder<T extends RepositoryBaseItem> extends AbstractItemBuilder<T> {

    @Override
    public List<? extends ContextAwareItem> apply(T parentItem, ProgressNotifier progressNotifier) {

        FilterContext filterContext = retrieveFilterContext(parentItem);

        if (isValidParentForChilds(parentItem, filterContext)) {
            List<? extends ContextAwareItem> result = getChildItemsFor(parentItem, filterContext, progressNotifier);
            progressNotifier.updateProgress(result.size(), result.size());
            return result;
        }

        return Collections.emptyList();
    }

    /**
     * Validates if the TreeItems create by this ItemBuilder are assignable to
     * the given parent item with the specified filters.
     *
     * @param parentItem the TreeItem of the explorer tree that will be the target/parent of the child if valid
     * @param queryParams the context with query filter the belongs to the  possible parent node
     *
     * @return true if the TreeItems build by this Builder can assigned to the tested parent TreeItem
     */
    protected abstract boolean isValidParentForChilds(T parentItem, FilterContext queryParams);
}
