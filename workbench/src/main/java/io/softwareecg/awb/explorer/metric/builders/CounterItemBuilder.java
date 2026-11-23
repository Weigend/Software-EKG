package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.CounterItem;

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