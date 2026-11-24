package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.ProcessGroup;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.ProcessGroupItem;

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

        MetricDataAccessService metricDataAccess = context.metrics();
        List<ProcessGroup> processGroups = metricDataAccess.getProcessGroups(new MetricQuery(context.getQueryParameter()));

        if (processGroups.isEmpty() || (processGroups.size() == 1 && "*".equals(processGroups.get(0).getValueName()))) {
            return List.of();
        }

        List<ProcessGroupItem> result = new ArrayList<>();
        for (ProcessGroup processGroup : processGroups) {
            QuerySpec parentQueryParams = context.getQueryParameter();
            QuerySpec childQueryParams = new QuerySpec.Builder(parentQueryParams).withProcessGroup(processGroup.getValueName()).build();
            result.add(new ProcessGroupItem(processGroup.getValueName(), context.cloneWith(childQueryParams)));
        }

        return result;
    }
}
