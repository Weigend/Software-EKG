package io.softwareecg.awb.explorer.metric.items.series.logical;

import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;

/**
 * todo enter comment here
 *
 * @since 18.06.2018
 */
public class ContainerItem extends ContextAwareItem {
    /**
     * Creates a new Container item based on the given context and the project
     *
     * @param label      the name of the tree item that will used as displayed label in the view
     * @param context    bean that define filters that relating to series data or the view flavor and control the item rendering
     */
    public ContainerItem(String label, FilterContext context) {
        super(label, context);
        super.setGraphic(getIconProvider().getContainerItemIcon(this));
    }
}
