package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.logical.Service;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.logical.ServiceItem;

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
                    QuerySpec parentQueryParams = parentContext.getQueryParameter();
                    QuerySpec childQueryParams = new QuerySpec.Builder(parentQueryParams).withService(service).build();
                    return new ServiceItem(service.getValueName(), parentContext.cloneWith(childQueryParams), item.getRepository());
                },
                notifier,
                services.size(),
                "Cloud Service"
        );
    }
}