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

import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;

/**
 * Explorer menu item to group metric counters.
 *
 */
public class MetricGroupItem extends ContextAwareItem {

    /**
     * Creates a MetricGroupItem with the given parameter.
     *
     * @param metricGroup   the metricGroup to use as name
     * @param context the context
     */
    public MetricGroupItem(MetricGroup metricGroup, FilterContext context) {
        super(metricGroup.getName(), context);
        super.setGraphic(getIconProvider().getMetricGroupItemIcon());
    }
}
