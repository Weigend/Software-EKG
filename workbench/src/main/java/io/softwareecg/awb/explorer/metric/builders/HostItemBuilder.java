package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.query.api.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.physical.HostItem;
import io.softwareecg.awb.explorer.metric.items.series.physical.HostsRootItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type HostItem
 *
 * @since 19.08.2018
 */
public class HostItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<HostItem> getChildItemsFor(T item, FilterContext parentContext, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = parentContext.metrics();

        List<HostGroup> hostGroups = metricDataAccess.getHostGroups(parentContext.getQueryParameter());
        if (item instanceof HostsRootItem &&
                !(hostGroups.isEmpty() || (hostGroups.size() == 1 && "*".equals(hostGroups.get(0).getValueName())))) {
            return List.of();
        }

        List<Host> metricHosts = metricDataAccess.getHosts(parentContext.getQueryParameter());
        int amountFetchedHosts = metricHosts.size();

        return ItemBuilder.getItems(metricHosts.stream().map(Host::getName),
              host -> {
                  QuerySpec parentQueryParams = parentContext.getQueryParameter();
                  QuerySpec childQueryParams = new QuerySpec.Builder(parentQueryParams).withHost(host).build();

                  return new HostItem(host, parentContext.cloneWith(childQueryParams));

                },
              notifier, amountFetchedHosts, "Hosts"
        );
    }
}