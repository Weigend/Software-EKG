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

import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Explorer tree item to group the counter groups.
 *
 */
public class CounterItem extends ContextAwareItem {

    /**
     * The Constructor to create a CounterItem.
     *
     * @param context    the context to use for creating
     */
    public CounterItem(FilterContext context) {
        super(isNotBlank(getProject(context).getDimensionAliasMetricGroup()) ?
                getProject(context).getDimensionAliasMetricGroup() : "Metrics", context);
        super.setGraphic(getIconProvider().getCounterItemIcon());
    }

    @Override
    protected int getOrderPriority() {
        return 80;
    }
}
