package io.softwareecg.awb.bookmark.api;

import io.softwareecg.awb.bookmark.model.BookmarkGroup;
import io.softwareecg.awb.query.model.QuerySpec;
import javafx.stage.Window;

/**
 * Asks the user about bookmarks, and stores what the user decides. To be called on the JavaFX
 * thread; the methods return when the dialog is closed.
 */
public interface BookmarkDialogs {

    /**
     * Makes a bookmark, and the group the user enters for it if there is none of that name.
     *
     * @param owner     the window the dialog belongs to
     * @param query     what the bookmark brings back
     * @param chartType the name of the chart type it brings back
     * @return whether the user made the bookmark
     */
    boolean createBookmark(Window owner, QuerySpec query, String chartType);

    /**
     * @param owner       the window the dialog belongs to
     * @param projectName the project of the group; {@code null} for a global one
     * @return whether the user made the group
     */
    boolean createGroup(Window owner, String projectName);

    /**
     * Deletes a group with all bookmarks in it if the user confirms it.
     *
     * @param owner the window the dialog belongs to
     * @param group the group
     * @return whether the group was deleted
     */
    boolean deleteGroup(Window owner, BookmarkGroup group);
}
