package io.softwareecg.awb.bookmark;

import io.softwareecg.awb.bookmark.api.MetricsBookmarkService;

import io.softwareecg.awb.metadata.model.BookmarkGroupRow;
import io.softwareecg.awb.metadata.model.MetricQueryBookmarkRow;
import io.softwareecg.awb.metadata.port.BookmarkStore;
import io.softwareecg.awb.bookmark.model.Bookmark;
import io.softwareecg.awb.bookmark.model.BookmarkGroup;
import io.softwareecg.awb.bookmark.model.MetricQueryBookmark;
import io.avaje.inject.External;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Metrics bookmark service implementation that stores bookmarks
 * and bookmark groups in the local H2 metadata database.
 *
 * @since 19.08.2018
 */
@Singleton
public class MetricsBookmarkServiceImpl implements MetricsBookmarkService {

    private final Logger LOGGER = EkgLogger.get();

    private final BookmarkStore bookmarkStore;

    @Inject
    public MetricsBookmarkServiceImpl(@External BookmarkStore bookmarkStore) {
        this.bookmarkStore = bookmarkStore;
    }

    // =========================================================================
    // Bookmark actions
    // =========================================================================

    @Override
    public List<MetricQueryBookmark> getMetricQueryBookmarks(String bookmarkGroupId) {
        return bookmarkStore.listQueryBookmarks(bookmarkGroupId).stream()
                .map(MetricsBookmarkServiceImpl::toMetricQueryBookmark)
                .collect(Collectors.toList());
    }

    @Override
    public void persistNewBookmark(Bookmark bookmark) {
        if (bookmark instanceof MetricQueryBookmark queryBookmark) {
            bookmarkStore.insertQueryBookmark(toQueryBookmarkRow(queryBookmark));
            return;
        }
        LOGGER.warn("Unsupported bookmark type for H2 persistence: {}", bookmark.getClass().getName());
    }

    @Override
    public void deleteBookmark(Bookmark bookmark) {
        bookmarkStore.deleteQueryBookmark(bookmark.getId());
    }

    // =========================================================================
    // BookmarkGroup actions
    // =========================================================================

    @Override
    public List<BookmarkGroup> getGlobalBookmarkGroups() {
        return bookmarkStore.listGlobalGroups().stream()
                .map(MetricsBookmarkServiceImpl::toBookmarkGroup)
                .filter(g -> !g.isEmptyGroup())
                .collect(Collectors.toList());
    }

    @Override
    public List<BookmarkGroup> getBookmarkGroupsByProject(String projectName) {
        return bookmarkStore.listGroups(projectName).stream()
                .map(MetricsBookmarkServiceImpl::toBookmarkGroup)
                .filter(g -> !g.isEmptyGroup())
                .collect(Collectors.toList());
    }

    @Override
    public void persistNewBookmarkGroup(BookmarkGroup bookmarkGroup) {
        bookmarkStore.insertGroup(toGroupRow(bookmarkGroup));
    }

    @Override
    public void deleteBookmarkGroup(String groupId) {
        bookmarkStore.deleteGroup(groupId);
    }

    // =========================================================================
    // mapping helpers
    // =========================================================================

    private static BookmarkGroup toBookmarkGroup(BookmarkGroupRow row) {
        BookmarkGroup group = new BookmarkGroup(row.name, row.id);
        group.setProjectName(row.projectName);
        return group;
    }

    private static BookmarkGroupRow toGroupRow(BookmarkGroup group) {
        BookmarkGroupRow row = new BookmarkGroupRow();
        row.id = group.getId();
        row.name = group.getName();
        row.projectName = group.getProjectName();
        return row;
    }

    private static MetricQueryBookmark toMetricQueryBookmark(MetricQueryBookmarkRow row) {
        MetricQueryBookmark.Builder builder = MetricQueryBookmark.fromJson(row.queryData);
        builder.withProject(row.projectName)
                .withId(row.id)
                .withName(row.name)
                .withBookmarkGroupId(row.bookmarkGroupId);
        return builder.build();
    }

    private static MetricQueryBookmarkRow toQueryBookmarkRow(MetricQueryBookmark bookmark) {
        MetricQueryBookmarkRow row = new MetricQueryBookmarkRow();
        row.id = bookmark.getId();
        row.name = bookmark.getName();
        row.bookmarkGroupId = bookmark.getBookmarkGroupId();
        row.projectName = bookmark.getProject();
        row.queryData = bookmark.toJson();
        return row;
    }
}
