package io.softwareecg.awb.bookmark.store;

import io.softwareecg.awb.bookmark.api.Bookmark;
import io.softwareecg.awb.bookmark.api.BookmarkContent;
import io.softwareecg.awb.bookmark.api.MetricsBookmarkService;

import io.softwareecg.awb.metadata.model.BookmarkGroupRow;
import io.softwareecg.awb.metadata.model.MetricQueryBookmarkRow;
import io.softwareecg.awb.metadata.port.BookmarkStore;
import io.softwareecg.awb.bookmark.api.BookmarkGroup;
import io.softwareecg.awb.query.api.QuerySpec;
import io.avaje.inject.External;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Metrics bookmark service implementation that stores bookmarks
 * and bookmark groups in the local H2 metadata database.
 *
 * @since 19.08.2018
 */
@Singleton
public class MetricsBookmarkServiceImpl implements MetricsBookmarkService {

    private final BookmarkStore bookmarkStore;

    @Inject
    public MetricsBookmarkServiceImpl(@External BookmarkStore bookmarkStore) {
        this.bookmarkStore = bookmarkStore;
    }

    // =========================================================================
    // Bookmark actions
    // =========================================================================

    @Override
    public List<Bookmark> globalBookmarks() {
        return withoutGroup(bookmark -> isBlank(bookmark.getProject()));
    }

    @Override
    public List<Bookmark> bookmarksOfProject(String projectName) {
        return withoutGroup(bookmark -> projectName.equals(bookmark.getProject()));
    }

    @Override
    public List<Bookmark> bookmarksIn(BookmarkGroup group) {
        return bookmarksInGroup(group.getId()).collect(Collectors.toList());
    }

    @Override
    public BookmarkContent contentOf(Bookmark bookmark, String projectInUse) {
        MetricQueryBookmark stored = (MetricQueryBookmark) bookmark;
        QuerySpec query = stored.asQueryContext();
        if (projectInUse != null && isGlobal(stored)) {
            query = new QuerySpec.Builder<>(query).withProject(projectInUse).build();
        }
        return new BookmarkContent(query, stored.getChartType());
    }

    private List<Bookmark> withoutGroup(Predicate<Bookmark> condition) {
        return bookmarksInGroup(BookmarkGroup.DEFAULT_GLOBAL_GROUP_ID).filter(condition).collect(Collectors.toList());
    }

    private Stream<MetricQueryBookmark> bookmarksInGroup(String groupId) {
        return bookmarkStore.listQueryBookmarks(groupId).stream().map(MetricsBookmarkServiceImpl::toMetricQueryBookmark);
    }

    /** A global bookmark is of no project, or in a group of no project - whatever project it was made in. */
    private boolean isGlobal(MetricQueryBookmark bookmark) {
        String groupId = bookmark.getBookmarkGroupId();
        return isBlank(bookmark.getProject())
                || groupId != null && getGlobalBookmarkGroups().stream().anyMatch(group -> groupId.equals(group.getId()));
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * Stores a new bookmark, and its group if that is new as well.
     *
     * @param bookmark the bookmark
     * @param group    the group the user chose for it
     */
    public void persistNewBookmark(MetricQueryBookmark bookmark, BookmarkGroup group) {

        if (group.isNew() && !group.isEmptyGroup()) {
            group.setId(UUID.randomUUID().toString());
            persistNewBookmarkGroup(group);
            bookmark.setBookmarkGroupId(group.getId());
        }

        // a group says which project its bookmarks belong to: none, if it is a global one.
        // Without a group the bookmark stays with the project it was made in.
        if (!group.isEmptyGroup()) {
            bookmark.setProject(group.getProjectName());
        }

        bookmarkStore.insertQueryBookmark(toQueryBookmarkRow(bookmark));
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

    public void persistNewBookmarkGroup(BookmarkGroup bookmarkGroup) {
        bookmarkStore.insertGroup(toGroupRow(bookmarkGroup));
    }

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
