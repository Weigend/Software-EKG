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
package io.softwareecg.awb.explorer.metric.items.series.physical;

import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.repository.api.Repository;

/**
 * The explorerView item that groups single hosts within the current branch.
 *
 */
public class HostGroupItem extends ContextAwareItem {

    /**
     * Creates a new HostGroupItem based on the given context using 'Hosts' as name
     *
     * @param label      the alias name that will displayed in the tree view that the node is shown
     * @param context    the filter context.
     * @param repository the containing types.
     */
    public HostGroupItem(String label, FilterContext context, Repository repository) {
        super(label, context, repository);
        super.setGraphic(getIconProvider().getHostGroupItemIcon(this));
    }

    @Override
    protected int getOrderPriority() {
        return 10;
    }
}