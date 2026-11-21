package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.logical.Namespace;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.logical.NamespaceGroupItem;
import io.softwareecg.awb.sdk.awbapi.project.CloudPlatformType;
import io.softwareecg.awb.sdk.awbapi.project.ProjectViewFlavor;

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

        return List.of(new NamespaceGroupItem(itemName, context, item.getRepository()));
    }
}