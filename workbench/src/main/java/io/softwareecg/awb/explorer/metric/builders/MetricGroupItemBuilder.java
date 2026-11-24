package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.explorer.tree.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import io.softwareecg.awb.query.api.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.MetricGroupItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type MetricGroupItem
 *
 * @since 19.08.2018
 */
public class MetricGroupItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<MetricGroupItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = context.metrics();

        List<MetricGroup> metricGroups = metricDataAccess.getMetricGroups(context.getQueryParameter());

        return ItemBuilder.getItems(metricGroups.stream(),
                metricGroup -> {
                    QuerySpec parentParams = context.getQueryParameter();
                    QuerySpec childQueryParams = new QuerySpec.Builder(parentParams).withMetricGroup(metricGroup).build();
                    return new MetricGroupItem(metricGroup, context.cloneWith(childQueryParams));
                },
                notifier,
                metricGroups.size(),
                "MetricGroups"
        );
    }
}