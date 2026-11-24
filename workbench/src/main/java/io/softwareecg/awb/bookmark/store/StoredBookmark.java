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
package io.softwareecg.awb.bookmark.store;

import io.softwareecg.awb.bookmark.api.Bookmark;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * Represents a bookmark. It contains the minimal information that is needed for a bookmark.
 * Bookmark metadata is stored in the H2 metadata database.
 *
 */
public abstract class StoredBookmark implements Bookmark, Serializable {

    private static final long serialVersionUID = -3504999081713331250L;

    /**
     * The unique id.
     */
    private String id;

    /**
     * The name.
     */
    private String name;

    /**
     * The unique id of the bookmark group this bookmark instance belongs to.
     * if it is not explicitly assigned to a group, this value will be '_BOOKMARK_GLOBAL_'
     * per default.
     */
    private String bookmarkGroupId;

    /**
     * The project name this bookmark belongs to.
     * For global bookmarks this is null.
     */
    private String project;

    /**
     * Protected constructor.
     */
    protected StoredBookmark() {
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBookmarkGroupId() {
        return bookmarkGroupId;
    }

    public void setBookmarkGroupId(String bookmarkGroupId) {
        this.bookmarkGroupId = bookmarkGroupId;
    }

    @Override
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof StoredBookmark bookmark)) {
            return false;
        }
        return new EqualsBuilder()
                .append(getId(), bookmark.getId())
                .append(getName(), bookmark.getName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getId())
                .append(getName())
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("groupId", bookmarkGroupId)
                .append("project", project)
                .toString();
    }

    /**
     * Builder to create a bookmark instance.
     *
     * @param <T> The actual type of the created {@link Bookmark}.
     */
    public abstract static class Builder<T extends StoredBookmark> {
        protected String id;
        protected String name;
        protected String bookmarkGroupId;
        protected String project;

        /**
         * Default constructor.
         */
        public Builder() {
        }

        /**
         * Create a builder from an existing bookmark.
         *
         * @param bookmark the initial values for the builder.
         */
        protected Builder(StoredBookmark bookmark) {
            this.id = bookmark.getId();
            this.name = bookmark.getName();
            this.bookmarkGroupId = bookmark.getBookmarkGroupId();
            this.project = bookmark.getProject();
        }

        /**
         * Add id to builder.
         *
         * @param id the id name
         * @return this for fluent interface
         */
        public Builder<T> withId(String id) {
            this.id = id;
            return this;
        }

        /**
         * Adds the id of the bookmark group to the builder
         * the constructed bookmark will assigned to.
         *
         * @param groupId the id of the bookmark group
         * @return this for fluent interface
         */
        public Builder<T> withBookmarkGroupId(String groupId) {
            this.bookmarkGroupId = groupId;
            return this;
        }

        /**
         * Add name to builder.
         *
         * @param name the name name
         * @return this for fluent interface
         */
        public Builder<T> withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Add project name to builder.
         *
         * @param project the project name
         * @return this for fluent interface
         */
        public Builder<T> withProject(String project) {
            this.project = project;
            return this;
        }

        /**
         * Build the real bookmark instance.
         *
         * @return the built bookmark
         */
        public abstract T build();

        /**
         * Instantiates a new bookmark with the given bookmark values.
         *
         * @param bookmark The new bookmark
         */
        protected void build(final T bookmark) {
            StoredBookmark b = bookmark;
            b.setId(this.id);
            b.name = this.name;
            b.bookmarkGroupId = this.bookmarkGroupId;
            b.project = this.project;
        }


    }
}
