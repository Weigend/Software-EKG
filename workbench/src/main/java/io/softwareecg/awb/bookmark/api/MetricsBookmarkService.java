//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.bookmark.api;

import io.softwareecg.awb.bookmark.model.Bookmark;
import io.softwareecg.awb.bookmark.model.BookmarkGroup;
import io.softwareecg.awb.bookmark.model.MetricBookmark;
import io.softwareecg.awb.bookmark.model.MetricQueryBookmark;

import java.util.List;

/**
 * An interface that represents a bookmark service that provides
 * various methods to create, update, delete und query bookmarks.
 *
 */
public interface MetricsBookmarkService {

    // ------------------------------------------------------------------------------------------------------
    // MetricBookmark actions
    // ------------------------------------------------------------------------------------------------------

    /**
     * Returns a sorted list of (metric) bookmarks that exists in the underlying repository
     * and is related to the specified bookmark group.
     * Depending on the group id the service will have the following behaviour:
     *  - bookmarkGroupId == null or "" : the service will return any MetricBookmark that exists in the repository
     *  - bookmarkGroupId == {@link BookmarkGroup#DEFAULT_GLOBAL_GROUP_ID } : the service will return all bookmark not assigned to a specific group
     *  - bookmarkGroupId == CONCRETE ID : the service will return bookmarks that are assigned to the specified group with the CONCRETE ID
     *
     * @param bookmarkGroupId the unique id of the bookmark group the searched bookmarks belongs to
     * @return a list of MetricBookmark instances that are assigned to the given group id (if specified)
     */
    List<MetricBookmark> getMetricBookmarks(String bookmarkGroupId);

    /**
     * Returns a sorted list of query-based bookmarks that are related to the specified bookmark group.
     * Semantics are the same as {@link #getMetricBookmarks(String)}.
     *
     * @param bookmarkGroupId the unique id of the bookmark group
     * @return a list of MetricQueryBookmark instances
     */
    List<MetricQueryBookmark> getMetricQueryBookmarks(String bookmarkGroupId);

    // ------------------------------------------------------------------------------------------------------
    // generic bookmark actions
    // ------------------------------------------------------------------------------------------------------

    /**
     * Stores a bookmark for the query context.
     *
     * @param bookmark the new bookmark to store.
     */
    void persistNewBookmark(Bookmark bookmark);

    /**
     * Deletes a bookmark.
     *
     * @param bookmark the bookmark.
     */
    void deleteBookmark(Bookmark bookmark);

    // ------------------------------------------------------------------------------------------------------
    // BookmarkGroup actions
    // ------------------------------------------------------------------------------------------------------

    /**
     * Returns bookmark groups where projectName is NULL (global groups).
     * The implicit "GLOBAL" group is excluded from the result.
     *
     * @return a sorted list of global BookmarkGroup items
     */
    List<BookmarkGroup> getGlobalBookmarkGroups();

    /**
     * Returns bookmark groups filtered by the given project name.
     * The implicit "GLOBAL" group is excluded from the result.
     *
     * @param projectName the project name to filter by
     * @return a sorted list of BookmarkGroup items for the given project
     */
    List<BookmarkGroup> getBookmarkGroupsByProject(String projectName);

    /**
     * Creates a new BookmarkGroup record in the repository.
     * The id of the given instance will used for consistency checks.
     * If not available the procedure will fail. The same if already an bookmark exists with that id.
     *
     * @param bookmarkGroup the bookmark group item to persist
     */
    void persistNewBookmarkGroup(BookmarkGroup bookmarkGroup);

    /**
     * Deletes a BookmarkGroup and all corresponding (Metric)Bookmarks.
     * The method will return true if all objects deleted successfully.
     * False if no bookmark group with the specified id exists.
     *
     * @param bookmarkId the unique id of the bookmark group to delete
     */
    void deleteBookmarkGroup(String bookmarkId);

}
