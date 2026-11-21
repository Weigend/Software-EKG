package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.ItemBuilder;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.MetricsBookmarkService;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.BookmarkGroup;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.bookmarks.BookmarkGroupItem;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.bookmarks.ProjectBookmarksItem;

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