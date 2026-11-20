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
package io.softwareecg.awb.metricanalyzer.ui.explorer.items.series;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.MetricGroup;
import io.softwareecg.awb.metricanalyzer.ui.api.ContextAwareItem;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;

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
     *                @param repository the containing types.
     */
    public MetricGroupItem(MetricGroup metricGroup, FilterContext context, EkgRepository repository) {
        super(metricGroup.getName(), context, repository);
        super.setGraphic(getIconProvider().getMetricGroupItemIcon(this));
    }
}
