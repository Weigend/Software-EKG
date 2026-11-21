package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.ItemBuilder;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.Host;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.HostGroup;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.physical.HostItem;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.physical.HostsRootItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type HostItem
 *
 * @since 19.08.2018
 */
public class HostItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<HostItem> getChildItemsFor(T item, FilterContext parentContext, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);

        List<HostGroup> hostGroups = metricDataAccess.getHostGroups(new MetricQuery(parentContext.getQueryParameter()));
        if (item instanceof HostsRootItem &&
                !(hostGroups.isEmpty() || (hostGroups.size() == 1 && "*".equals(hostGroups.get(0).getValueName())))) {
            return List.of();
        }

        List<Host> metricHosts = metricDataAccess.getHosts(new MetricQuery(parentContext.getQueryParameter()));
        int amountFetchedHosts = metricHosts.size();

        return ItemBuilder.getItems(metricHosts.stream().map(Host::getName),
              host -> {
                  QueryFilterParams parentQueryParams = parentContext.getQueryParameter();
                  QueryFilterParams childQueryParams = new QueryFilterParams.Builder(parentQueryParams).withHost(host).build();

                  return new HostItem(host, parentContext.cloneWith(childQueryParams), item.getRepository());

                },
              notifier, amountFetchedHosts, "Hosts"
        );
    }
}