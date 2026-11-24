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


import java.util.List;

/**
 * An interface that represents a bookmark service that provides
 * various methods to query and delete bookmarks. They are made in the {@link BookmarkDialogs}.
 *
 */
public interface MetricsBookmarkService {

    // ------------------------------------------------------------------------------------------------------
    // bookmarks
    // ------------------------------------------------------------------------------------------------------

    /**
     * @return the bookmarks that are in no group and belong to no project
     */
    List<Bookmark> globalBookmarks();

    /**
     * @param projectName the name of the project
     * @return the bookmarks of the project that are in no group
     */
    List<Bookmark> bookmarksOfProject(String projectName);

    /**
     * @param group the group
     * @return the bookmarks in the group
     */
    List<Bookmark> bookmarksIn(BookmarkGroup group);

    /**
     * Says what a bookmark brings back. The bookmark stays as it is.
     *
     * @param bookmark     the bookmark
     * @param projectInUse the project a global bookmark is opened for; {@code null} if there is none
     * @return its query, for its own project or - if it is global - the project in use, and its chart type
     */
    BookmarkContent contentOf(Bookmark bookmark, String projectInUse);

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
