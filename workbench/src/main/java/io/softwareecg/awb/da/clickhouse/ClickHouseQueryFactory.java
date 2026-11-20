package io.softwareecg.awb.da.clickhouse;

import io.softwareecg.awb.da.clickhouse.expression.ClickHouseExpressionRenderer;
import io.softwareecg.awb.da.clickhouse.utils.ClickHouseObjectBinder;
import io.softwareecg.awb.da.api.dataobject.expr.Expression;
import io.softwareecg.awb.da.api.dataobject.facet.FacetParams;
import io.softwareecg.awb.da.api.dataobject.search.ExportParams;
import io.softwareecg.awb.da.api.dataobject.search.SearchParams;
import io.softwareecg.awb.da.api.dataobject.search.SortField;
import io.softwareecg.awb.da.api.schema.EkgSchemaField;
import io.softwareecg.awb.da.api.schema.Field;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Factory for building ClickHouse SQL queries from the repository API's search/export/facet parameters.
 *
 */
class ClickHouseQueryFactory {

    private static final ConcurrentHashMap<Class<?>, Set<String>> cachedColumns = new ConcurrentHashMap<>();

    private final ClickHouseExpressionRenderer renderer;

    public ClickHouseQueryFactory() {
        this(new ClickHouseExpressionRenderer());
    }

    ClickHouseQueryFactory(ClickHouseExpressionRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Creates a SELECT query for paginated search.
     */
    public String createSelectQuery(String tableName, Class<?> type, SearchParams searchParams) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(getColumnList(type, searchParams.getSortFields()));
        sql.append(" FROM ").append(escapeTableName(tableName));
        appendWhereClause(sql, searchParams.getFilterQueries());
        appendOrderByClause(sql, searchParams.getSortFields());
        sql.append(" LIMIT ").append(searchParams.getMaxRows());
        sql.append(" OFFSET ").append(searchParams.getStartRow());
        return sql.toString();
    }

    /**
     * Creates a COUNT query to get the total number of matching rows.
     */
    public String createCountQuery(String tableName, List<Expression> filterQueries) {
        StringBuilder sql = new StringBuilder("SELECT count() AS cnt FROM ");
        sql.append(escapeTableName(tableName));
        appendWhereClause(sql, filterQueries);
        return sql.toString();
    }

    /**
     * Creates a SELECT query for unbounded streaming export.
     */
    public String createExportQuery(String tableName, Class<?> type, ExportParams exportParams) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(getColumnList(type, exportParams.getSortFields()));
        sql.append(" FROM ").append(escapeTableName(tableName));
        appendWhereClause(sql, exportParams.getFilterQueries());
        appendOrderByClause(sql, exportParams.getSortFields());
        return sql.toString();
    }

    /**
     * Creates a SELECT query with collapse/group semantics.
     * ClickHouse equivalent: use argMax/argMin for selecting representative rows within groups.
     */
    public String createCollapseQuery(String tableName, Class<?> type, SearchParams searchParams,
                                      Field groupingField, SortField groupSortField) {
        Set<String> columns = getColumns(type, searchParams.getSortFields());
        String groupFieldName = groupingField.getName();

        // Use GROUP BY with any() for non-grouped columns (picks an arbitrary value from the group)
        // and argMax/argMin for the representative row based on sort
        StringBuilder sql = new StringBuilder("SELECT ");
        String columnList = columns.stream()
                .map(col -> {
                    if (col.equals(groupFieldName)) {
                        return col;
                    }
                    if (groupSortField != null) {
                        String func = groupSortField.mode() == SortField.SortMode.DESC ? "argMax" : "argMin";
                        return func + "(" + col + ", " + groupSortField.field().getName() + ") AS " + col;
                    }
                    return "any(" + col + ") AS " + col;
                })
                .collect(Collectors.joining(", "));

        sql.append(columnList);
        sql.append(" FROM ").append(escapeTableName(tableName));
        appendWhereClause(sql, searchParams.getFilterQueries());
        sql.append(" GROUP BY ").append(groupFieldName);
        appendOrderByClause(sql, searchParams.getSortFields());
        sql.append(" LIMIT ").append(searchParams.getMaxRows());
        sql.append(" OFFSET ").append(searchParams.getStartRow());
        return sql.toString();
    }

    /**
     * Creates a SUM aggregation query.
     */
    public String createSumQuery(String tableName, EkgSchemaField field, List<Expression> filterQueries) {
        StringBuilder sql = new StringBuilder("SELECT sum(");
        sql.append(field.getName());
        sql.append(") AS total FROM ");
        sql.append(escapeTableName(tableName));
        appendWhereClause(sql, filterQueries);
        return sql.toString();
    }

    /**
     * Creates a facet query using GROUP BY with counts.
     */
    public String createFacetQuery(String tableName, FacetParams facetParams) {
        List<String> queries = new ArrayList<>();

        for (Field facetField : facetParams.getFacetFields()) {
            StringBuilder sql = new StringBuilder("SELECT '");
            sql.append(facetField.getName());
            sql.append("' AS facet_field, ");

            boolean includeMissing = facetParams.getFacetMissing().contains(facetField);
            if (includeMissing) {
                sql.append("COALESCE(toString(").append(facetField.getName()).append("), '<NULL>') AS facet_value");
            } else {
                sql.append("toString(").append(facetField.getName()).append(") AS facet_value");
            }

            sql.append(", count() AS facet_count FROM ");
            sql.append(escapeTableName(tableName));
            appendWhereClause(sql, facetParams.getFilterQueries());

            if (!includeMissing) {
                if (facetParams.getFilterQueries() != null && !facetParams.getFilterQueries().isEmpty()) {
                    sql.append(" AND ");
                } else {
                    sql.append(" WHERE ");
                }
                sql.append(facetField.getName()).append(" IS NOT NULL");
            }

            sql.append(" GROUP BY facet_value");

            if (facetParams.getMinCount() > 0) {
                sql.append(" HAVING facet_count >= ").append(facetParams.getMinCount());
            }

            String sortOrder = "count".equals(facetParams.getFacetSort())
                    ? " ORDER BY facet_count DESC"
                    : " ORDER BY facet_value ASC";
            sql.append(sortOrder);

            if (facetParams.getLimit() > 0) {
                sql.append(" LIMIT ").append(facetParams.getLimit());
            }

            queries.add(sql.toString());
        }

        return String.join(";\n", queries);
    }

    /**
     * Creates a DELETE query.
     */
    public String createDeleteQuery(String tableName, String filterQueryString) {
        return "ALTER TABLE " + escapeTableName(tableName) + " DELETE WHERE " + filterQueryString;
    }

    /**
     * Creates a DELETE ALL query.
     */
    public String createDeleteAllQuery(String tableName) {
        return "TRUNCATE TABLE " + escapeTableName(tableName);
    }

    private String getColumnList(Class<?> type, Collection<SortField> sortFields) {
        return String.join(", ", getColumns(type, sortFields));
    }

    private Set<String> getColumns(Class<?> type, Collection<SortField> sortFields) {
        Set<String> columns = new LinkedHashSet<>(cachedColumns.computeIfAbsent(type,
                ClickHouseObjectBinder::getSelectColumns));

        if (columns.isEmpty()) {
            throw new IllegalStateException(
                    "No field annotations found on class " + type.getCanonicalName());
        }

        // Sort fields must be included in the column list
        if (sortFields != null) {
            for (SortField sortField : sortFields) {
                columns.add(sortField.field().getName());
            }
        }

        return columns;
    }

    private void appendWhereClause(StringBuilder sql, Collection<Expression> filterQueries) {
        if (filterQueries == null || filterQueries.isEmpty()) {
            return;
        }

        String whereClause = filterQueries.stream()
                .map(renderer::render)
                .collect(Collectors.joining(" AND "));

        sql.append(" WHERE ").append(whereClause);
    }

    private void appendOrderByClause(StringBuilder sql, Collection<SortField> sortFields) {
        if (sortFields == null || sortFields.isEmpty()) {
            return;
        }

        String orderBy = sortFields.stream()
                .map(sf -> sf.field().getName() + " " + sf.mode().toString())
                .collect(Collectors.joining(", "));

        sql.append(" ORDER BY ").append(orderBy);
    }

    private String escapeTableName(String tableName) {
        if (tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return tableName;
        }
        return "`" + tableName.replace("`", "``") + "`";
    }
}
