package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.ProcessGroup;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.ProcessGroupItem;
import io.softwareecg.awb.explorer.metric.items.series.ProcessItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type ProcessItem.
 * When ProcessGroups exist and the parent is not a ProcessGroupItem, returns empty list
 * so that processes are only shown under their respective ProcessGroup.
 *
 * @since 19.08.2018
 */
public class ProcessItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<ProcessItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = context.metrics();

        // Check if process groups exist - if so, processes should only appear under ProcessGroupItem
        List<ProcessGroup> processGroups = metricDataAccess.getProcessGroups(new MetricQuery(context.getQueryParameter()));
        if (!(item instanceof ProcessGroupItem) &&
                !(processGroups.isEmpty() || (processGroups.size() == 1 && "*".equals(processGroups.get(0).getValueName())))) {
            return List.of();
        }

        List<Process> metricProcesses = metricDataAccess.getProcesses(new MetricQuery(context.getQueryParameter()));

        return ItemBuilder.getItems(metricProcesses.stream(),
                process -> {
                    QuerySpec parentParams = context.getQueryParameter();
                    QuerySpec childQueryParams = new QuerySpec.Builder(parentParams).withProcess(process).build();
                    return new ProcessItem(process, context.cloneWith(childQueryParams));
                },
                notifier,
                metricProcesses.size(),
                "Prozess"
        );
    }
}