package io.softwareecg.awb.explorer.metric.items.series.logical;

import io.softwareecg.awb.explorer.metric.api.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;

/**
 * todo insert comment here
 *
 * @since 21.08.2018
 */
public class PodGroupItem extends ContextAwareItem {

    /**
     * Creates a new PodGroupItem item based on the given context
     *
     * @param label      the name of the tree item that will used as displayed label in the view
     * @param context    bean that define filters that relating to series data or the view flavor and control the item rendering
     * @param repository the containing types.
     */
    public PodGroupItem(String label, FilterContext context, EkgRepository repository) {
        super(label, context, repository);
        super.setGraphic(getIconProvider().getPodItemIcon(this));
    }

    @Override
    protected int getOrderPriority() {
        return 40;
    }
}
