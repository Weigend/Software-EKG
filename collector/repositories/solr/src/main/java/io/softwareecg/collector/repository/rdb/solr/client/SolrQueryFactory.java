package io.softwareecg.collector.repository.rdb.solr.client;

import io.softwareecg.collector.repository.rdb.api.querymodel.delete.DeleteParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.expr.Expression;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SortField;
import io.softwareecg.collector.repository.rdb.api.schema.Field;
import io.softwareecg.collector.repository.rdb.api.schema.FieldReader;
import io.softwareecg.collector.repository.rdb.solr.client.expression.SolrExpressionRenderer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ShardParams;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A factory for Solr export, facet, or select queries.
 *
 * @since 17/07/2018
 */
/* package-private */ class SolrQueryFactory {

    /**
     * Cached Solr fields for {@link #getFieldList(Class, Collection)}.
     */
    private static final ConcurrentHashMap<Class<?>, Set<String>> cachedSolrFields = new ConcurrentHashMap<>();

    private final SolrExpressionRenderer renderer;

    /**
     * Constructor.
     */
    public SolrQueryFactory() {
        this(new SolrExpressionRenderer());
    }

    /* package-private */ SolrQueryFactory(SolrExpressionRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Creates a Solr select query based on the given entity type and search parameters.
     * <p/>
     * The entity type is used to reduce the number of fetched fields to those annotated with
     * {@link Field} in the given entity type.
     *
     * @param type         The type of entity to fetch.
     * @param searchParams The search parameters.
     * @return The Solr query.
     */
    public SolrQuery createSelectQuery(Class<?> type, SearchParams searchParams) {
        return createBasicSolrQuery()
                .setStart(searchParams.getStartRow())
                .setRows(searchParams.getMaxRows())
                .setFields(getFieldList(type, searchParams.getSortFields()))
                .setSorts(getSortClauses(searchParams.getSortFields()))
                .setFilterQueries(getFilterQueries(searchParams.getFilterQueries()));
    }

    /**
     * Creates a deleteQuery String in the Solr query language.
     * Each expression in the given parameters will concat via 'AND'.
     * Only records that match to each of the expressions will deleted.
     *
     * @param deleteParams a DeleteParams instance with the filter expressions
     * @return the content of the DeleteParams bean as filter query in Solr query language
     */
    public String createDeleteQuery(DeleteParams deleteParams) {

        if (deleteParams.getFilterQueries() == null) {
            return "";
        }

        return deleteParams.getFilterQueries().stream()
                .map(renderer::render).reduce((exp1, exp2) -> exp1 + " AND " + exp2).orElse("");
    }


    /**
     * Creates a basic "match all" Solr query.
     *
     * @return The Solr query.
     */
    private SolrQuery createBasicSolrQuery() {
        return new SolrQuery("*:*")
                .setParam(CommonParams.WT, CommonParams.JAVABIN)
                .setParam(ShardParams.SHARDS_PREFERENCE, CommonParams.TRUE);
    }

    /**
     * Returns all Solr fields required for binding documents to the given entity class. Additionally, the given
     * {@link SortField} will also be included, even if it is not part of the entity class.
     *
     * @param type       the type of the entities
     * @param sortFields the field on which to sort
     * @return the Solr fields
     */
    private String[] getFieldList(Class<?> type, Collection<SortField> sortFields) {
        Set<String> solrFields = new HashSet<>(cachedSolrFields.computeIfAbsent(type,
                clazz -> FieldReader.getSchemaFields(clazz).keySet()));

        if (solrFields.isEmpty()) {
            throw new IllegalStateException(
                    "No Solr field annotations found on class " + type.getCanonicalName());
        }

        // The /export handler requires that all sort attributes are in the field list. Thus the sort fields must be
        // included even if they are not part of the entity class.
        for (SortField sortField : sortFields) {
            solrFields.add(sortField.field().getName());
        }

        return solrFields.toArray(new String[0]);
    }

    /**
     * Converts the given PSMG-Hub sort fields to Solr sort clauses.
     *
     * @param sortFields The sort fields or {@code null}.
     * @return The sort clauses.
     */
    private List<SolrQuery.SortClause> getSortClauses(Collection<SortField> sortFields) {
        if (sortFields == null) {
            return Collections.emptyList();
        }

        return sortFields.stream()
                .map(this::toSortClause)
                .collect(Collectors.toList());
    }

    /**
     * Renders the given filter expressions into Solr filter queries.
     *
     * @param expressions The filter expressions or {@code null}.
     * @return The filter queries.
     */
    private String[] getFilterQueries(Collection<Expression> expressions) {
        if (expressions == null) {
            return new String[0];
        }

        return expressions.stream()
                .map(renderer::render)
                .toArray(String[]::new);
    }

    /**
     * Extracts the names of the given fields.
     *
     * @param fields The fields or {@code null}.
     * @return The field names.
     */
    private String[] extractFieldNames(Collection<Field> fields) {
        if (fields == null) {
            return new String[0];
        }

        return fields.stream()
                .map(Field::getName)
                .toArray(String[]::new);
    }

    /**
     * Converts a sort field into a Solr sort clause.
     *
     * @param sortField The sort field.
     * @return The sort clause.
     */
    private SolrQuery.SortClause toSortClause(SortField sortField) {
        return new SolrQuery.SortClause(
                sortField.field().getName(),
                sortField.mode().toString());
    }
}
