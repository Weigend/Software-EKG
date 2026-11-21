package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Process;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.ProcessGroup;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.ProcessGroupItem;

import java.util.ArrayList;
import java.util.List;

/**
 * TreeItem builder that produces ProcessGroup TreeItem instances.
 * Analogous to HostGroupItemBuilder.
 *
 * @since 19.08.2018
 */
public class ProcessGroupItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {

    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return parentFilterContext.getQueryParameter().getProcess() == Process.DEFAULT
                && parentFilterContext.getQueryParameter().getProcessGroup() == ProcessGroup.DEFAULT;
    }

    @Override
    protected List<ProcessGroupItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);
        List<ProcessGroup> processGroups = metricDataAccess.getProcessGroups(new MetricQuery(context.getQueryParameter()));

        if (processGroups.isEmpty() || (processGroups.size() == 1 && "*".equals(processGroups.get(0).getValueName()))) {
            return List.of();
        }

        List<ProcessGroupItem> result = new ArrayList<>();
        for (ProcessGroup processGroup : processGroups) {
            QueryFilterParams parentQueryParams = context.getQueryParameter();
            QueryFilterParams childQueryParams = new QueryFilterParams.Builder(parentQueryParams).withProcessGroup(processGroup.getValueName()).build();
            result.add(new ProcessGroupItem(processGroup.getValueName(), context.cloneWith(childQueryParams), item.getRepository()));
        }

        return result;
    }
}
