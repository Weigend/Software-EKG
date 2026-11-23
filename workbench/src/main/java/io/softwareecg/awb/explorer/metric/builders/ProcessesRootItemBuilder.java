package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.ProcessGroup;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.ProcessesRootItem;

import java.util.List;

/**
 * TreeItem builder that produces a ProcessesRootItem that acts as
 * parent for all ProcessGroupItem's.
 * The node is hidden when the project has no meaningful process data.
 *
 * @since 7.0
 */
public class ProcessesRootItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {

    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return true;
    }

    @Override
    protected List<ProcessesRootItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);
        MetricQuery query = new MetricQuery(context.getQueryParameter());

        List<Process> processes = metricDataAccess.getProcesses(query);
        if (hasOnlyPlaceholderValues(processes)) {
            return List.of();
        }

        String aliasLabel = context.getQueryParameter().getProject().getDimensionAliasProcess();
        return List.of(new ProcessesRootItem(aliasLabel, "Processes", context, item.getRepository()));
    }
}
