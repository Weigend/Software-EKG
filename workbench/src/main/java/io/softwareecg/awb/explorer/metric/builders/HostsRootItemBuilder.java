package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.physical.HostsRootItem;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;

import java.util.List;

/**
 * TreeItem builder that produces a HostGroupsItem that acts as
 * parent for all HostGroupItem's.
 * The node is hidden when the project has no meaningful host data.
 *
 * @since 13.05.2020
 */
public class HostsRootItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {

    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return parentFilterContext.getActiveViewFlavor() == ProjectViewFlavor.PHYSICAL_VIEW;
    }

    @Override
    protected List<HostsRootItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = context.metrics();
        MetricQuery query = new MetricQuery(context.getQueryParameter());

        List<Host> hosts = metricDataAccess.getHosts(query);
        if (hasOnlyPlaceholderValues(hosts)) {
            return List.of();
        }

        List<HostGroup> hostGroups = metricDataAccess.getHostGroups(query);

        if (hostGroups.isEmpty() || (hostGroups.size() == 1 && "*".equals(hostGroups.get(0).getValueName()))) {
            String aliasLabel = context.getQueryParameter().getProject().getDimensionAliasHost();
            return List.of(new HostsRootItem(aliasLabel, "Hosts", context));
        } else {
            String aliasLabel = context.getQueryParameter().getProject().getDimensionAliasHostGroup();
            return List.of(new HostsRootItem(aliasLabel,"Clusters/Devices", context));
        }
    }
}
