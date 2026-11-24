package io.softwareecg.awb.bookmark.api;

/**
 * A bookmark as the other components see it. What it brings back is in {@link BookmarkContent}.
 */
public interface Bookmark {

    /**
     * @return the unique id
     */
    String getId();

    /**
     * @return the name the user gave it
     */
    String getName();

    /**
     * @return the name of the project it belongs to; {@code null} for a global bookmark
     */
    String getProject();
}
