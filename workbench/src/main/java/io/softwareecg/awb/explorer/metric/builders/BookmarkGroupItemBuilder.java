package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.bookmark.MetricsBookmarkService;
import io.softwareecg.awb.bookmark.model.BookmarkGroup;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.bookmarks.BookmarkGroupItem;
import io.softwareecg.awb.explorer.metric.items.bookmarks.ProjectBookmarksItem;

import java.util.List;

/**
 * TreeItem builder that produces BookmarkGroupItem instances.
 * The builder creates a BookmarkGroup item for each group that exists in the repository
 * but not for the virtual group "_BOOKMARK_GLOBAL_".
 *
 * @since 19.08.2018
 */
public class BookmarkGroupItemBuilder<T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    private static final String ITEM_DESC = "BookmarkGroups";

    @Override
    protected List<BookmarkGroupItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricsBookmarkService bookmarkService = item.getRepository().getBoundedService(MetricsBookmarkService.class);

        List<BookmarkGroup> bookmarkGroups;
        if (item instanceof ProjectBookmarksItem projectBookmarksItem) {
            bookmarkGroups = bookmarkService.getBookmarkGroupsByProject(projectBookmarksItem.getProjectName());
        } else {
            bookmarkGroups = bookmarkService.getGlobalBookmarkGroups();
        }
        int amountOfGroups = bookmarkGroups.size();

        return ItemBuilder.getItems(
                bookmarkGroups.stream(),
                bookmarkGroup -> new BookmarkGroupItem(bookmarkGroup, null, item.getRepository()),
                notifier,
                amountOfGroups,
                ITEM_DESC
        );
    }
}