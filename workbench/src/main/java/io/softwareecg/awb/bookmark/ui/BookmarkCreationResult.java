package io.softwareecg.awb.bookmark.ui;

import io.softwareecg.awb.bookmark.model.Bookmark;
import io.softwareecg.awb.bookmark.model.BookmarkGroup;

/**
 * Container that holds the result of the {@link CreateBookmarkDialog}:
 * the newly created bookmark and the bookmark group it should be assigned to.
 *
 * @param <T> the concrete bookmark type
 * @since 04.03.2020
 */
public class BookmarkCreationResult<T extends Bookmark> {

    private T bookmark;

    private BookmarkGroup bookmarkGroup;

    public void setBookmark(T bookmark) {
        this.bookmark = bookmark;
    }

    public void setBookmarkGroup(BookmarkGroup group) {
        this.bookmarkGroup = group;
    }

    public T getBookmark() {
        return bookmark;
    }

    public BookmarkGroup getBookmarkGroup() {
        return bookmarkGroup;
    }
}
