package io.softwareecg.awb.repository.series;

import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.api.SeriesFilter;
import io.softwareecg.awb.repository.api.SeriesPage;
import io.softwareecg.awb.repository.api.SeriesSearch;
import io.softwareecg.awb.repository.persistence.BinaryTimeSeries;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.persistence.search.SearchParams;
import io.softwareecg.awb.repository.persistence.search.SearchResult;
import io.softwareecg.awb.repository.persistence.search.SortField;
import io.softwareecg.awb.repository.persistence.search.expr.Expression;
import io.softwareecg.awb.repository.persistence.search.facet.FacetEntry;
import io.softwareecg.awb.repository.persistence.search.facet.FacetParams;
import io.softwareecg.awb.repository.persistence.schema.DocumentType;
import io.softwareecg.awb.repository.persistence.schema.EkgSchemaField;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.codec.DateValuePairCodec;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.softwareecg.awb.repository.persistence.search.expr.ExprFactory.and;
import static io.softwareecg.awb.repository.persistence.search.expr.ExprFactory.dateTimeRangeFilter;
import static io.softwareecg.awb.repository.persistence.search.expr.ExprFactory.exactFilter;
import static io.softwareecg.awb.repository.persistence.search.expr.ExprFactory.not;
import static io.softwareecg.awb.repository.persistence.search.expr.ExprFactory.or;
import static io.softwareecg.awb.repository.persistence.search.expr.ExprFactory.wildcardFilter;

/**
 * Searches the store of a repository: it knows the fields the series are stored in and how their
 * values are encoded.
 */
public class StoredSeriesSearch implements SeriesSearch {

    private static final Logger LOGGER = EkgLogger.get();

    /** The cursor the stores start with. */
    private static final String FIRST_PAGE = "*";

    private static final int MAX_VALUES_OF_A_DIMENSION = 2500;

    private final TimeSeriesRepository store;

    public StoredSeriesSearch(TimeSeriesRepository store) {
        this.store = store;
    }

    @Override
    public SeriesPage find(SeriesFilter filter, String cursor, long maxSeries) throws RepositoryException {
        String startOfPage = cursor == null ? FIRST_PAGE : cursor;

        SearchParams search = new SearchParams();
        search.withFilterQueries(expressionsOf(filter));
        search.withSortFields(List.of(
                new SortField(EkgSchemaField.TS_START, SortField.SortMode.ASC),
                new SortField(EkgSchemaField.ID, SortField.SortMode.ASC)));
        search.withCursor(startOfPage);

        long t0 = System.currentTimeMillis();
        SearchResult<BinaryTimeSeries> result = store.search(BinaryTimeSeries.class, search);
        long tSearch = System.currentTimeMillis() - t0;

        long totalHits = result.queryStatus().getNumberOfHits();
        // a store that is at its end answers with the cursor it was asked with
        String next = result.queryStatus().getCursor();
        String nextCursor = startOfPage.equals(next) ? null : next;
        if (totalHits > maxSeries) {
            return new SeriesPage(totalHits, List.of(), nextCursor);
        }

        long t1 = System.currentTimeMillis();
        List<TimeSeries> series = new ArrayList<>(result.rows().size());
        for (BinaryTimeSeries stored : result.rows()) {
            series.add(decode(stored));
        }
        LOGGER.info("Fetch chunk: search={}ms, decode={}ms, series={}",
                tSearch, System.currentTimeMillis() - t1, series.size());
        return new SeriesPage(totalHits, series, nextCursor);
    }

    @Override
    public List<String> valuesOf(FilterDimension dimension, SeriesFilter filter) throws RepositoryException {
        FacetParams facets = new FacetParams()
                .withLimit(MAX_VALUES_OF_A_DIMENSION)
                .withMinCount(1)
                .withSort(FacetParams.FacetSort.LEXICOGRAPHIC)
                .withFacetFields(fieldOf(dimension));
        facets.withFilterQueries(and(expressionsOf(filter)));

        return store.facet(facets).getFacets().stream()
                .flatMap(facet -> facet.entries().stream())
                .map(FacetEntry::getName)
                .toList();
    }

    @Override
    public long endOf(SeriesFilter filter) throws RepositoryException {
        SearchParams search = new SearchParams();
        search.withFilterQueries(expressionsOf(filter));
        search.withMaxRows(1);
        search.withSortField(EkgSchemaField.TS_STOP, SortField.SortMode.DESC);

        SearchResult<BinaryTimeSeries> result = store.search(BinaryTimeSeries.class, search);
        if (result.queryStatus().getNumberOfHits() == 0 || result.rows().isEmpty()) {
            return 0;
        }
        return result.rows().get(0).getEnd();
    }

    private static Expression[] expressionsOf(SeriesFilter filter) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(exactFilter(EkgSchemaField.DOC_TYPE, DocumentType.TIME_SERIES.toString()));
        expressions.add(exactFilter(EkgSchemaField.PROJECT_NAME, filter.projectName()));

        for (SeriesFilter.Condition condition : filter.conditions()) {
            expressions.add(expressionOf(condition));
        }

        // every part of a series that reaches into the interval: it ends after the start and
        // begins before the end. The bounds are taken as they are. They used to be moved inwards
        // by a minute each, which dropped the parts at the edges and, in a window of about a
        // minute, nearly all of them.
        if (filter.start() > 0) {
            expressions.add(dateTimeRangeFilter(EkgSchemaField.TS_STOP, Instant.ofEpochMilli(filter.start()), null));
        }
        if (filter.end() > 0) {
            expressions.add(dateTimeRangeFilter(EkgSchemaField.TS_START, null, Instant.ofEpochMilli(filter.end())));
        }
        return expressions.toArray(new Expression[0]);
    }

    private static Expression expressionOf(SeriesFilter.Condition condition) {
        EkgSchemaField field = fieldOf(condition.dimension());
        List<Expression> alternatives = new ArrayList<>();
        for (String value : condition.values()) {
            alternatives.add(switch (condition.comparison()) {
                case IS -> exactFilter(field, value);
                case MATCHES -> wildcardFilter(field, value);
                case MATCHES_NOT -> not(wildcardFilter(field, value));
            });
        }
        return alternatives.size() == 1 ? alternatives.get(0) : or(alternatives.toArray(new Expression[0]));
    }

    private static EkgSchemaField fieldOf(FilterDimension dimension) {
        return switch (dimension) {
            case HOST_GROUP -> EkgSchemaField.TS_HOST_GROUP_NAME;
            case HOST -> EkgSchemaField.TS_HOST_NAME;
            case NAMESPACE -> EkgSchemaField.TS_NAMESPACE_NAME;
            case SERVICE -> EkgSchemaField.TS_SERVICE_NAME;
            case POD -> EkgSchemaField.TS_POD_NAME;
            case CONTAINER -> EkgSchemaField.TS_CONTAINER_NAME;
            case MEASUREMENT -> EkgSchemaField.TS_MEASUREMENT;
            case PROCESS_GROUP -> EkgSchemaField.TS_PROCESS_GROUP_NAME;
            case PROCESS -> EkgSchemaField.TS_PROCESS_NAME;
            case METRIC_GROUP -> EkgSchemaField.TS_METRIC_GROUP;
            case METRIC_NAME -> EkgSchemaField.TS_METRIC_NAME;
        };
    }

    private static TimeSeries decode(BinaryTimeSeries stored) {
        TimeSeries series = new TimeSeries(
                stored.getProjectName(),
                stored.getHostGroup(),
                stored.getHost(),
                stored.getNamespace(),
                stored.getService(),
                stored.getPod(),
                stored.getContainer(),
                stored.getMeasurement(),
                stored.getProcessGroup(),
                stored.getProcess(),
                stored.getMetricGroup(),
                stored.getMetricName());
        series.setRemoteSeriesKey(stored.getRemoteSeriesKey());

        // the series of a remote project have no values here
        if (stored.getData() != null) {
            // directly to primitive arrays - no Value object allocation
            Object[] arrays = DateValuePairCodec.decodeToPrimitiveArrays(stored.getData());
            series.setSortedArrays((long[]) arrays[0], (double[]) arrays[1]);
        }
        return series;
    }
}
