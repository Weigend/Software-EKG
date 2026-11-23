package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.explorer.metric.items.bookmarks.ReproBookmarksItem;

import java.util.List;

/**
 * TreeItem builder that produces ReproBookmarksItem instances.
 * This item instance (should a single instance per repository) is just an empty
 * tree node with the visual style of bookmarks, that acts as parent for
 * the bookmark groups or (if assigned to global space) the bookmarks itself.
 *
 * @since 26.02.2020
 */
public class ReproBookmarksItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    private static final String ITEM_DESC = "Global Bookmarks";

    @Override
    protected List<ReproBookmarksItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {
        return List.of(new ReproBookmarksItem(ITEM_DESC, context, item.getRepository()));
    }
}