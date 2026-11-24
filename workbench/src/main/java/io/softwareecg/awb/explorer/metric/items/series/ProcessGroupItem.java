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
import io.softwareecg.awb.repository.api.Repository;

/**
 * The explorerView item that groups single processes within the current branch.
 * Analogous to HostGroupItem for hosts.
 *
 */
public class ProcessGroupItem extends ContextAwareItem {

    /**
     * Creates a new ProcessGroupItem based on the given context
     *
     * @param label      the process group name that will be displayed in the tree view
     * @param context    the filter context.
     * @param repository the containing repository.
     */
    public ProcessGroupItem(String label, FilterContext context, Repository repository) {
        super(label, context, repository);
        super.setGraphic(getIconProvider().getProcessGroupItemIcon(this));
    }

    @Override
    protected int getOrderPriority() {
        return 70;
    }
}
