package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.Host;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.HostGroup;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.physical.HostsRootItem;
import io.softwareecg.awb.sdk.awbapi.project.ProjectViewFlavor;

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

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);
        MetricQuery query = new MetricQuery(context.getQueryParameter());

        List<Host> hosts = metricDataAccess.getHosts(query);
        if (hasOnlyPlaceholderValues(hosts)) {
            return List.of();
        }

        List<HostGroup> hostGroups = metricDataAccess.getHostGroups(query);

        if (hostGroups.isEmpty() || (hostGroups.size() == 1 && "*".equals(hostGroups.get(0).getValueName()))) {
            String aliasLabel = context.getQueryParameter().getProject().getDimensionAliasHost();
            return List.of(new HostsRootItem(aliasLabel, "Hosts", context, item.getRepository()));
        } else {
            String aliasLabel = context.getQueryParameter().getProject().getDimensionAliasHostGroup();
            return List.of(new HostsRootItem(aliasLabel,"Clusters/Devices", context, item.getRepository()));
        }
    }
}
