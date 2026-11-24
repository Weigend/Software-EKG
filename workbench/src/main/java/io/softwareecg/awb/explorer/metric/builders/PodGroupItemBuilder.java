package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.explorer.tree.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.logical.PodGroupItem;
import io.softwareecg.awb.projectcontext.api.ProjectViewFlavor;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type PodGroupItem
 *
 * @since 21.08.2018
 */
public class PodGroupItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {

    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return parentFilterContext.getQueryParameter().getPod() == Pod.DEFAULT
                && parentFilterContext.getActiveViewFlavor() == ProjectViewFlavor.LOGICAL_VIEW;
    }

    @Override
    protected List<PodGroupItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {
        return List.of(new PodGroupItem("Pods", context));
    }
}