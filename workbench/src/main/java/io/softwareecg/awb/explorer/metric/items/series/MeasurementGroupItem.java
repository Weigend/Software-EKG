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

import io.softwareecg.awb.explorer.metric.api.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * The explorerView tree item to group the series items.
 *
 */
public class MeasurementGroupItem extends ContextAwareItem {

    /**
     * Constructs a measurements item
     *
     * @param context    - the query context
     * @param repository the containing types.
     */
    public MeasurementGroupItem(FilterContext context, EkgRepository repository) {
        super(isNotBlank(getProject(context).getDimensionAliasMeasurement()) ?
                getProject(context).getDimensionAliasMeasurement() : "Measurements", context, repository);

        super.setGraphic(getIconProvider().getMeasurementGroupItemIcon(this));
    }

    @Override
    protected int getOrderPriority() {
        return 50;
    }
}
