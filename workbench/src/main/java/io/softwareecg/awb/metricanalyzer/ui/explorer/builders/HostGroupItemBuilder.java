package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.common.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.Host;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.HostGroup;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.physical.HostGroupItem;
import io.softwareecg.awb.sdk.awbapi.project.ProjectViewFlavor;

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
            QueryFilterParams parentQueryParams = context.getQueryParameter();
            QueryFilterParams childQueryParams = new QueryFilterParams.Builder(parentQueryParams).withHostGroup(hostGroup.getValueName()).build();
            result.add(new HostGroupItem(hostGroup.getValueName(), context.cloneWith(childQueryParams), item.getRepository()));
        }

        return result;
    }
}