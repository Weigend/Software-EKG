package io.softwareecg.awb.explorer.metric.items.series.logical;

import io.softwareecg.awb.explorer.metric.api.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;

/**
 * todo insert comment here
 *
 * @since 20.08.2018
 */
public class ServiceItem extends ContextAwareItem {

    /**
     * Creates a new Service item based on the given context and the project
     *
     * @param label      the name of the tree item that will used as displayed label in the view
     * @param context    bean that define filters that relating to series data or the view flavor and control the item rendering
     * @param repository the containing types.
     */
    public ServiceItem(String label, FilterContext context, EkgRepository repository) {
        super(label, context, repository);
        super.setGraphic(getIconProvider().getServiceItemIcon(this));
    }
}
