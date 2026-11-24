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
import io.softwareecg.awb.bookmark.model.MetricQueryBookmark;

import java.util.List;

/**
 * An interface that represents a bookmark service that provides
 * various methods to query and delete bookmarks. They are made in the {@link BookmarkDialogs}.
 *
 */
public interface MetricsBookmarkService {

    // ------------------------------------------------------------------------------------------------------
    // MetricQueryBookmark actions
    // ------------------------------------------------------------------------------------------------------

    /**
     * Returns a sorted list of the bookmarks that are related to the specified bookmark group.
     *
     * @param bookmarkGroupId the unique id of the bookmark group
     * @return a list of MetricQueryBookmark instances
     */
    List<MetricQueryBookmark> getMetricQueryBookmarks(String bookmarkGroupId);

    // ------------------------------------------------------------------------------------------------------
    // generic bookmark actions
    // ------------------------------------------------------------------------------------------------------

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

}
