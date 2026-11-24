package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.logical.PodItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type PodItem
 *
 * @since 19.08.2018
 */
public class PodItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<PodItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = context.metrics();

        List<Pod> pods = metricDataAccess.getPods(new MetricQuery(context.getQueryParameter()));

        return ItemBuilder.getItems(pods.stream(),
                pod -> {
                    QuerySpec parentParams = context.getQueryParameter();
                    QuerySpec childQueryParams = new QuerySpec.Builder(parentParams).withPod(pod).build();
                    return new PodItem(pod.getValueName(), context.cloneWith(childQueryParams));
                },
                notifier,
                pods.size(),
                "Pods"
        );
    }
}