package io.softwareecg.awb.metricanalyzer.ui.bookmarks;

import io.softwareecg.awb.common.ui.components.FilterableComboBoxStringConverter;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.BookmarkGroup;
import org.apache.commons.lang3.StringUtils;

/**
 * A simple converter that converts a String into a BookmarkGroup
 * and vice versa.
 * The conversion form String isn't complete because the BookmarkID is missing
 *
 * @since 06.03.2020
 */
public class BookmarkGroupStringConverter extends FilterableComboBoxStringConverter<BookmarkGroup> {

    @Override
    public String toString(BookmarkGroup bookmarkGroup) {
        if (bookmarkGroup != null) {
            return bookmarkGroup.getName();
        }

        return "NULL";
    }

    @Override
    public BookmarkGroup fromString(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }

        BookmarkGroup bookmarkGroup = super.selectValue(s);
        if (bookmarkGroup != null) {
            return bookmarkGroup;
        }

        return new BookmarkGroup(s);
    }
}
