package io.softwareecg.awb.metricanalyzer.bl.bookmarks.et;

import java.io.Serializable;
import java.util.Objects;

/**
 * A POJO that represents a bookmark group that wraps
 * bookmarks that belongs to a specific domain or topic.
 * Stored in the H2 metadata database.
 *
 * @since 26.02.2020
 */
public class BookmarkGroup implements Serializable {

    /**
     * The id of the 'virtual' bookmark group if an bookmark
     * isn't assigned to a specific group and exists in the global space.
     */
    public static final String DEFAULT_GLOBAL_GROUP_ID = "_BOOKMARK_GLOBAL_";

    /**
     * The unique id of the bookmark group.
     */
    private String id;

    /**
     * The name of the bookmark group (shown in the UI)
     */
    private String name;

    /**
     * The project this bookmark group belongs to.
     */
    private String projectName;

    /**
     * Creates a new instance of BookmarkGroup
     */
    public BookmarkGroup() {
    }

    /**
     * Constructs a new BookmarkGroup that
     * has the given name.
     *
     * @param name the display name of the bookmark group
     */
    public BookmarkGroup(String name) {
        this.name = name;
    }

    /**
     * Constructs a new BookmarkGroup with
     * the given name and id
     *
     * @param name the readable alias name of the group
     * @param id the unique id of the group
     */
    public BookmarkGroup(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Returns a boolean value that indicates
     * if this BookmarkGroup is new (never persisted before)
     * or not.
     *
     * @return TRUE if the BookmarkGroup is a new created one without id.
     */
    public boolean isNew() {
        return id == null;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isEmptyGroup() {
        return DEFAULT_GLOBAL_GROUP_ID.equals(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookmarkGroup that = (BookmarkGroup) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
