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
import io.softwareecg.awb.repository.api.EkgRepository;

/**
 * The explorerView item that shows a single host.
 *
 */
public class HostItem extends ContextAwareItem {

    /**
     * Creates a HostItem with the given name and the Context.
     *
     * @param name    the item's name
     * @param context the context.
     * @param repository   the containing types.
     */
    public HostItem(String name, FilterContext context, EkgRepository repository) {
        super(name, context, repository);
        super.setGraphic(getIconProvider().getHostItemIcon(this));
    }
}