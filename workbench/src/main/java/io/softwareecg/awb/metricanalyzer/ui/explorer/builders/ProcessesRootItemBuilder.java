package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Process;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.ProcessGroup;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.ProcessesRootItem;

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
