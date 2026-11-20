package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.ItemBuilder;
import io.softwareecg.awb.explorer.ui.items.common.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Measurement;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.MeasurementItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type MeasurementItem
 *
 * @since 19.08.2018
 */
public class MeasurementItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<MeasurementItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);

        List<Measurement> measurements = metricDataAccess.getMeasurements(new MetricQuery(context.getQueryParameter()));

        return ItemBuilder.getItems(measurements.stream(),
                measurement -> {
                    QueryFilterParams parentParams = context.getQueryParameter();
                    QueryFilterParams childQueryParams = new QueryFilterParams.Builder(parentParams).withMeasurement(measurement).build();
                    return new MeasurementItem(measurement, context.cloneWith(childQueryParams), item.getRepository());
                },
                notifier,
                measurements.size(),
                "Measurements"
        );
    }
}