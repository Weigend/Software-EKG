package io.softwareecg.awb.repository.port.search;

import io.softwareecg.awb.repository.schema.Field;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

/**
 * Encapsulate the sort information for a search query.
 *
 */
public record SortField(Field field, SortMode mode) {

    /**
     * Instantiate a new sort info.
     *
     * @param field the field
     * @param mode  the mode
     */
    public SortField {
        Validate.notNull(field);
        Validate.notNull(mode);
    }

    /**
     * The sorting order.
     */
    public enum SortMode {
        /**
         * Ascending order.
         */
        ASC,
        /**
         * Descending order.
         */
        DESC;

        @Override
        public String toString() {
            return StringUtils.lowerCase(name());
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortField sortField = (SortField) o;
        return Objects.equals(field, sortField.field) &&
                mode == sortField.mode;
    }

}
