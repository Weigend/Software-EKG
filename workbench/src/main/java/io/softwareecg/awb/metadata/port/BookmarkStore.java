package io.softwareecg.awb.metadata.port;

import io.softwareecg.awb.metadata.model.BookmarkGroupRow;
import io.softwareecg.awb.metadata.model.MetricBookmarkRow;
import io.softwareecg.awb.metadata.model.MetricQueryBookmarkRow;

import java.util.List;

/** Persistence port for bookmarks and bookmark groups. */
public interface BookmarkStore {
    List<MetricBookmarkRow> listBookmarks(String bookmarkGroupId);
    List<MetricBookmarkRow> listBookmarksByProject(String projectName);
    void insertBookmark(MetricBookmarkRow row);
    void deleteBookmark(String id);

    List<MetricQueryBookmarkRow> listQueryBookmarks(String bookmarkGroupId);
    void insertQueryBookmark(MetricQueryBookmarkRow row);
    void deleteQueryBookmark(String id);

    List<BookmarkGroupRow> listGroups();
    List<BookmarkGroupRow> listGlobalGroups();
    List<BookmarkGroupRow> listGroups(String projectName);
    void insertGroup(BookmarkGroupRow row);
    void updateGroup(BookmarkGroupRow row);
    void deleteGroup(String groupId);
}
