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

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Process;
import io.softwareecg.awb.metricanalyzer.ui.api.ContextAwareItem;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;

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
     * @param repository the containing types.
     */
    public ProcessItem(Process process, FilterContext context, EkgRepository repository) {
        super(process.getName(), context, repository);
        super.setGraphic(getIconProvider().getProcessItemIcon(this));
    }
}