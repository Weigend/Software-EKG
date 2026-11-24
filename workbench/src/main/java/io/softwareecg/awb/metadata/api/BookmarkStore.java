package io.softwareecg.awb.metadata.api;


import java.util.List;

/** Persistence port for bookmarks and bookmark groups. */
public interface BookmarkStore {
    List<MetricQueryBookmarkRow> listQueryBookmarks(String bookmarkGroupId);
    void insertQueryBookmark(MetricQueryBookmarkRow row);
    void deleteQueryBookmark(String id);

    List<BookmarkGroupRow> listGlobalGroups();
    List<BookmarkGroupRow> listGroups(String projectName);
    void insertGroup(BookmarkGroupRow row);
    void deleteGroup(String groupId);
}
