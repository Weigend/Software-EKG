package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.ItemBuilder;
import io.softwareecg.awb.explorer.ui.items.common.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Process;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.ProcessGroup;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.ProcessGroupItem;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.ProcessItem;

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

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);

        // Check if process groups exist - if so, processes should only appear under ProcessGroupItem
        List<ProcessGroup> processGroups = metricDataAccess.getProcessGroups(new MetricQuery(context.getQueryParameter()));
        if (!(item instanceof ProcessGroupItem) &&
                !(processGroups.isEmpty() || (processGroups.size() == 1 && "*".equals(processGroups.get(0).getValueName())))) {
            return List.of();
        }

        List<Process> metricProcesses = metricDataAccess.getProcesses(new MetricQuery(context.getQueryParameter()));

        return ItemBuilder.getItems(metricProcesses.stream(),
                process -> {
                    QueryFilterParams parentParams = context.getQueryParameter();
                    QueryFilterParams childQueryParams = new QueryFilterParams.Builder(parentParams).withProcess(process).build();
                    return new ProcessItem(process, context.cloneWith(childQueryParams), item.getRepository());
                },
                notifier,
                metricProcesses.size(),
                "Prozess"
        );
    }
}