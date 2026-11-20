package io.softwareecg.awb.metricanalyzer.bl.bookmarks;

import io.softwareecg.awb.da.h2.H2BookmarkRepository;
import io.softwareecg.awb.da.h2.H2BookmarkRepository.BookmarkGroupRow;
import io.softwareecg.awb.da.h2.H2BookmarkRepository.MetricBookmarkRow;
import io.softwareecg.awb.da.h2.H2BookmarkRepository.MetricQueryBookmarkRow;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.Bookmark;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.BookmarkGroup;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.MetricBookmark;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.MetricQueryBookmark;
import io.softwareecg.awb.da.api.RepositoryClient;
import io.avaje.inject.External;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
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

    private final H2BookmarkRepository h2Repo;

    @Inject
    public MetricsBookmarkServiceImpl(@External H2BookmarkRepository h2Repo) {
        this.h2Repo = h2Repo;
    }

    @Override
    public void initializeService(RepositoryClient client) {
        // no-op: H2 repository is initialized by DI
    }

    // =========================================================================
    // MetricBookmark actions
    // =========================================================================

    @Override
    public List<MetricBookmark> getMetricBookmarks(String bookmarkGroupId) {
        return h2Repo.listBookmarks(bookmarkGroupId).stream()
                .map(MetricsBookmarkServiceImpl::toMetricBookmark)
                .collect(Collectors.toList());
    }

    @Override
    public List<MetricQueryBookmark> getMetricQueryBookmarks(String bookmarkGroupId) {
        return h2Repo.listQueryBookmarks(bookmarkGroupId).stream()
                .map(MetricsBookmarkServiceImpl::toMetricQueryBookmark)
                .collect(Collectors.toList());
    }

    @Override
    public void persistNewBookmark(Bookmark bookmark) {
        if (bookmark instanceof MetricQueryBookmark queryBookmark) {
            h2Repo.insertQueryBookmark(toQueryBookmarkRow(queryBookmark));
            return;
        }
        if (bookmark instanceof MetricBookmark metricBookmark) {
            h2Repo.insertBookmark(toBookmarkRow(metricBookmark));
            return;
        }
        LOGGER.warn("Unsupported bookmark type for H2 persistence: {}", bookmark.getClass().getName());
    }

    @Override
    public void updateBookmark(Bookmark bookmark) {
        if (bookmark.getId() != null) {
            deleteBookmark(bookmark);
        }
        persistNewBookmark(bookmark);
    }

    @Override
    public void deleteBookmark(Bookmark bookmark) {
        if (bookmark instanceof MetricQueryBookmark) {
            h2Repo.deleteQueryBookmark(bookmark.getId());
        } else {
            h2Repo.deleteBookmark(bookmark.getId());
        }
    }

    // =========================================================================
    // BookmarkGroup actions
    // =========================================================================

    @Override
    public List<BookmarkGroup> getBookmarkGroups() {
        return h2Repo.listGroups().stream()
                .map(MetricsBookmarkServiceImpl::toBookmarkGroup)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookmarkGroup> getGlobalBookmarkGroups() {
        return h2Repo.listGlobalGroups().stream()
                .map(MetricsBookmarkServiceImpl::toBookmarkGroup)
                .filter(g -> !g.isEmptyGroup())
                .collect(Collectors.toList());
    }

    @Override
    public List<BookmarkGroup> getBookmarkGroupsByProject(String projectName) {
        return h2Repo.listGroups(projectName).stream()
                .map(MetricsBookmarkServiceImpl::toBookmarkGroup)
                .filter(g -> !g.isEmptyGroup())
                .collect(Collectors.toList());
    }

    @Override
    public void persistNewBookmarkGroup(BookmarkGroup bookmarkGroup) {
        h2Repo.insertGroup(toGroupRow(bookmarkGroup));
    }

    @Override
    public void updateBookmark(BookmarkGroup bookmarkGroup) {
        h2Repo.updateGroup(toGroupRow(bookmarkGroup));
    }

    @Override
    public void deleteBookmarkGroup(String groupId) {
        h2Repo.deleteGroup(groupId);
    }

    // =========================================================================
    // mapping helpers
    // =========================================================================

    private static MetricBookmark toMetricBookmark(MetricBookmarkRow row) {
        return new MetricBookmark.Builder()
                .withCommandProtocol(row.commandProtocol)
                .withProject(row.projectName)
                .withId(row.id)
                .withName(row.name)
                .withBookmarkGroupId(row.bookmarkGroupId)
                .build();
    }

    private static MetricBookmarkRow toBookmarkRow(MetricBookmark bookmark) {
        MetricBookmarkRow row = new MetricBookmarkRow();
        row.id = bookmark.getId();
        row.name = bookmark.getName();
        row.bookmarkGroupId = bookmark.getBookmarkGroupId();
        row.commandProtocol = bookmark.getSerializedCommandProtocol();
        row.projectName = bookmark.getProject();
        return row;
    }

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
