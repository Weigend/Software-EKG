package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.MeasurementGroupItem;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;

import java.util.List;

/**
 * TreeItem builder that produces Measurements TreeItem instances.
 * The node is hidden when the project has no meaningful measurement data.
 *
 * @since 19.08.2018
 */
public class MeasurementGroupItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {


    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return parentFilterContext.getQueryParameter().getMeasurement() == Measurement.DEFAULT &&
                  parentFilterContext.getActiveViewFlavor() == ProjectViewFlavor.PHYSICAL_VIEW;
    }

    @Override
    protected List<MeasurementGroupItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);
        List<Measurement> measurements = metricDataAccess.getMeasurements(new MetricQuery(context.getQueryParameter()));
        if (hasOnlyPlaceholderValues(measurements)) {
            return List.of();
        }

        return List.of(new MeasurementGroupItem(context, item.getRepository()));
    }
}
