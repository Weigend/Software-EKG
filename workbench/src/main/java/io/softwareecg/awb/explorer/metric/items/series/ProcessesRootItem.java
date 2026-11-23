package io.softwareecg.awb.explorer.metric.items.series;

import io.softwareecg.awb.explorer.metric.api.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;
import org.apache.commons.lang3.StringUtils;

/**
 * The root TreeItem that contains all ProcessGroups as direct child within
 * the current tree-view branch.
 *
 * ProcessesRootItem(1) -> ProcessGroupItem(n) -> ProcessItem(n)
 *
 * or as real-world example:
 *
 * Processes -> Application-Backend -> Instance 1
 *
 * @since 7.0
 */
public class ProcessesRootItem extends ContextAwareItem {

    /**
     * Creates a new ProcessesRootItem based on the given context using 'Processes' as name
     *
     * @param aliasLabel   the dimension alias label if defined
     * @param defaultLabel the label of this node that should displayed in the tree-view if no dimension alias is defined
     * @param context      the filter context.
     * @param repository   the containing repository.
     */
    public ProcessesRootItem(String aliasLabel, String defaultLabel, FilterContext context, EkgRepository repository) {
        super(StringUtils.isNotBlank(aliasLabel) ? aliasLabel: defaultLabel, context, repository);
        super.setGraphic(getIconProvider().getProcessGroupItemIcon(this));
    }

    @Override
    protected int getOrderPriority() {
        return 60;
    }
}
