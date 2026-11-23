package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.physical.HostGroupItem;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;

import java.util.ArrayList;
import java.util.List;

/**
 * TreeItem builder that produces HostGroup TreeItem instances.
 *
 * @since 19.08.2018
 */
public class HostGroupItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {

    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return parentFilterContext.getQueryParameter().getHost() == Host.DEFAULT
                && parentFilterContext.getActiveViewFlavor() == ProjectViewFlavor.PHYSICAL_VIEW;
    }

    @Override
    protected List<HostGroupItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);
        List<HostGroup> hostGroups = metricDataAccess.getHostGroups(new MetricQuery(context.getQueryParameter()));

        if (hostGroups.isEmpty() || (hostGroups.size() == 1 && "*".equals(hostGroups.get(0).getValueName()))) {
            return List.of();
        }

        List<HostGroupItem> result = new ArrayList<>();
        for (HostGroup hostGroup : hostGroups) {
            QuerySpec parentQueryParams = context.getQueryParameter();
            QuerySpec childQueryParams = new QuerySpec.Builder(parentQueryParams).withHostGroup(hostGroup.getValueName()).build();
            result.add(new HostGroupItem(hostGroup.getValueName(), context.cloneWith(childQueryParams), item.getRepository()));
        }

        return result;
    }
}