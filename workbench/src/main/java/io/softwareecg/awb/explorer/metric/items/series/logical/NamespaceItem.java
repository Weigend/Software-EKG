package io.softwareecg.awb.explorer.metric.items.series.logical;

import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;

/**
 * todo enter comment here
 *
 * @since 18.06.2018
 */
public class NamespaceItem extends ContextAwareItem {


    /**
     * Creates a new namespace item based on the given context and the project
     *
     * @param namespace  the Namespace entity that will represented by the tree item and contains the label that will shown.
     * @param context    bean that define filters that relating to series data or the view flavor and control the item rendering
     */
    public NamespaceItem(Namespace namespace, FilterContext context) {
        super(namespace.getValueName(), context);
        super.setGraphic(getIconProvider().getNamespaceItemIcon(this));
    }
}
