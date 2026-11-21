package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Measurement;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.MetricQuery;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.MeasurementGroupItem;
import io.softwareecg.awb.sdk.awbapi.project.ProjectViewFlavor;

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
