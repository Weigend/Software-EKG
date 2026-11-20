package io.softwareecg.awb.metricanalyzer.bl.tsquery.query;

/**
 * todo enter comment here
 *
 * @since 02.06.2018
 */
public class SortClause {

    private final String field;

    private final SortOrder order;

    private SortClause(String field, SortOrder order) {
        this.field = field;
        this.order = order;
    }

    public static SortClause asc(String field) {
        return new SortClause(field, SortOrder.ASC);
    }

    public static SortClause dsc(String field) {
        return new SortClause(field, SortOrder.DSC);
    }

    public String getField() {
        return field;
    }

    public SortOrder getOrder() {
        return order;
    }
}
