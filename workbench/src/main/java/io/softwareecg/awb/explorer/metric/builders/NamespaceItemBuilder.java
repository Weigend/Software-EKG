package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.logical.NamespaceItem;
import io.softwareecg.awb.projectcontext.model.CloudPlatformType;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type NamespaceItem.
 *
 * @since 20.08.2018
 */
public class NamespaceItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<NamespaceItem> getChildItemsFor(T item, FilterContext parentContext, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = parentContext.metrics();

        List<Namespace> namespaces = metricDataAccess.getNamespaces(new MetricQuery(parentContext.getQueryParameter()));

        CloudPlatformType platformType = parentContext.getQueryParameter().getProject().getCloudPlatformType();
        final String itemName = platformType == CloudPlatformType.OPEN_SHIFT ? "Projekt (Namespace)" : "Namespace";

        return ItemBuilder.getItems(namespaces.stream(),
                namespace -> {
                    QuerySpec parentQueryParams = parentContext.getQueryParameter();
                    QuerySpec childQueryParams = new QuerySpec.Builder(parentQueryParams).withNamespace(namespace).build();
                    return new NamespaceItem(namespace, parentContext.cloneWith(childQueryParams));
                },
                notifier,
                namespaces.size(),
                itemName
        );
    }
}