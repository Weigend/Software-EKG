package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.ItemBuilder;
import io.softwareecg.awb.explorer.ui.items.common.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.logical.Service;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.logical.ServiceItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type NamespaceItem.
 *
 * @since 20.08.2018
 */
public class ServiceItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<ServiceItem> getChildItemsFor(T item, FilterContext parentContext, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);
        List<Service> services = metricDataAccess.getServices(new MetricQuery(parentContext.getQueryParameter()));

        return ItemBuilder.getItems(services.stream(),
                service -> {
                    QueryFilterParams parentQueryParams = parentContext.getQueryParameter();
                    QueryFilterParams childQueryParams = new QueryFilterParams.Builder(parentQueryParams).withService(service).build();
                    return new ServiceItem(service.getValueName(), parentContext.cloneWith(childQueryParams), item.getRepository());
                },
                notifier,
                services.size(),
                "Cloud Service"
        );
    }
}