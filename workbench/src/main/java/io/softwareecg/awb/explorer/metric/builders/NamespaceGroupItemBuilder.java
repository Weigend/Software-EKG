package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.logical.NamespaceGroupItem;
import io.softwareecg.awb.projectcontext.model.CloudPlatformType;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type NamespaceGroupItem.
 *
 * @since 20.08.2018
 */
public class NamespaceGroupItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {

    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return parentFilterContext.getQueryParameter().getNamespace() == Namespace.DEFAULT
                && parentFilterContext.getActiveViewFlavor() == ProjectViewFlavor.LOGICAL_VIEW;
    }

    @Override
    protected List<NamespaceGroupItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        CloudPlatformType platformType = context.getQueryParameter().getProject().getCloudPlatformType();
        final String itemName = platformType == CloudPlatformType.OPEN_SHIFT ? "Projekte (Namespaces)" : "Namespaces";

        return List.of(new NamespaceGroupItem(itemName, context));
    }
}