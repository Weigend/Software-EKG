package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.bookmark.api.Bookmark;
import io.softwareecg.awb.bookmark.api.MetricsBookmarkService;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.explorer.metric.items.bookmarks.BookmarkGroupItem;
import io.softwareecg.awb.explorer.metric.items.bookmarks.BookmarkItem;
import io.softwareecg.awb.explorer.metric.items.bookmarks.ProjectBookmarksItem;
import io.softwareecg.awb.explorer.metric.items.bookmarks.ReproBookmarksItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * TreeItem builder that produces TreeItem instances of type BookmarkItem.
 * This builder acts with two different contexts:
 * As child of RepoBookmarkGroupItem it serves a list of BookmarkItem's that are assigned to the global bookmark space.
 * As child of BookmarkGroupItem it serves a list of BookmarkItem's that are assigned to this specific bookmark group.
 *
 * @since 19.08.2018
 */
public class BookmarkItemBuilder <T extends RepositoryBaseItem>  extends AbstractItemBuilder<T> {

    private static final String LOADING_MESSAGE = "Loading bookmarks";

    @Override
    protected List<BookmarkItem> getChildItemsFor(T parent, FilterContext context, ProgressNotifier notifier) {

        notifier.updateProgress(LOADING_MESSAGE, -1, 100);

        MetricsBookmarkService service = context.bookmarks();

        List<Bookmark> bookmarks = switch (parent) {
            case ProjectBookmarksItem project -> service.bookmarksOfProject(project.getProjectName());
            case BookmarkGroupItem group -> service.bookmarksIn(group.getBookmarkGroup());
            default -> service.globalBookmarks();
        };

        AtomicInteger i = new AtomicInteger(0);

        ObservableList<BookmarkItem> collect = bookmarks.stream()
                .sorted(Comparator.comparing(Bookmark::getName))
                .map(b -> new BookmarkItem(b, context))
                .peek(b -> notifier.updateProgress(LOADING_MESSAGE, i.getAndIncrement(), bookmarks.size()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        notifier.updateProgress(1, 1);

        return collect;
    }
}
