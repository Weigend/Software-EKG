package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.series.CounterItem;

import java.util.List;

/**
 * TreeItem builder that produces CounterItem TreeItem instances.
 *
 * @since 19.08.2018
 */
public class CounterItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    @Override
    protected List<CounterItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {
        return List.of(new CounterItem(context, item.getRepository()));
    }
}