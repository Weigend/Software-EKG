package io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.physical;

import io.softwareecg.awb.metricanalyzer.ui.api.ContextAwareItem;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;
import org.apache.commons.lang3.StringUtils;

/**
 * The root TreeItem that contains all HostGroups as direct child this
 * within the current tree-view branch.
 *
 * HostsRootItem(1) -> HostGroupItem(n) -> HostItem(n)
 *
 * or as real-world example at physical IT-systems:
 *
 * Cluster -> PSMG-Dev1-Cluster -> Node XYZ
 *
 * @since 13.05.2020
 */
public class HostsRootItem extends ContextAwareItem {

    /**
     * Creates a new HostsRootItem based on the given context using 'Hosts' as name
     *
     * @param aliasLabel   the dimension alias label if defined
     * @param defaultLabel the label of this node that should displayed in the tree-view if no dimension alias is defined
     * @param context      the filter context.
     * @param repository   the containing types.
     */
    public HostsRootItem(String aliasLabel, String defaultLabel, FilterContext context, EkgRepository repository) {
        super(StringUtils.isNotBlank(aliasLabel) ? aliasLabel: defaultLabel, context, repository);
        super.setGraphic(getIconProvider().getHostGroupItemIcon(this));
    }

    @Override
    protected int getOrderPriority() {
        return 10;
    }
}
