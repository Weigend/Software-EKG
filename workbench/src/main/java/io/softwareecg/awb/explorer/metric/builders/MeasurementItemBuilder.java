package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.MeasurementItem;

import java.util.List;

/**
 * TreeItem builder that produces TreeItem instances of type MeasurementItem
 *
 * @since 19.08.2018
 */
public class MeasurementItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<MeasurementItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = context.metrics();

        List<Measurement> measurements = metricDataAccess.getMeasurements(context.getQueryParameter());

        return ItemBuilder.getItems(measurements.stream(),
                measurement -> {
                    QuerySpec parentParams = context.getQueryParameter();
                    QuerySpec childQueryParams = new QuerySpec.Builder(parentParams).withMeasurement(measurement).build();
                    return new MeasurementItem(measurement, context.cloneWith(childQueryParams));
                },
                notifier,
                measurements.size(),
                "Measurements"
        );
    }
}