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

import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;

/**
 * The explorerView item to show a single process.
 *
 *
 * @since 08.04.2014
 */
public class ProcessItem extends ContextAwareItem {

    /**
     * Constructs a process item
     *
     * @param process    process
     * @param context    the query context
     */
    public ProcessItem(Process process, FilterContext context) {
        super(process.getName(), context);
        super.setGraphic(getIconProvider().getProcessItemIcon());
    }
}