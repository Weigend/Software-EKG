//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.explorer.metric.items.series;

import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.repository.api.Repository;

/**
 * The explorerView tree item for a single series.
 *
 */
public class MeasurementItem extends ContextAwareItem {

    /**
     * The icon of the node in the explorerView tree
     */
    private static final String NODE_ICON = "/io/softwareecg/awb/explorer/metric/icons/series/counters.png";


    /**
     * Constructs a series from the given arguments
     *
     * @param measurement - the series
     * @param context     - the query context
     *                    @param repository the containing types.
     */
    public MeasurementItem(Measurement measurement, FilterContext context, Repository repository) {
        super(measurement.getName(), context, repository);
        super.setGraphic(getIconProvider().getMeasurementItemIcon(this));
    }
}