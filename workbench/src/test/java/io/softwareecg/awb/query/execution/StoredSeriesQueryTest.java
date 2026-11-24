package io.softwareecg.awb.query.execution;

import io.avaje.inject.BeanScope;
import io.softwareecg.awb.datasource.model.RepositorySeriesEnds;
import io.softwareecg.awb.datasource.model.SeriesDimensions;
import io.softwareecg.awb.query.spi.RemoteSeriesSource;
import io.softwareecg.awb.projectcontext.api.Project;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.query.api.QuerySpec;
import io.softwareecg.awb.repository.adapter.RepositoryAdapterBase;
import io.softwareecg.awb.repository.adapter.solr.expression.SolrExpressionRenderer;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.persistence.BinaryTimeSeries;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.persistence.search.QueryStatus;
import io.softwareecg.awb.repository.persistence.search.SearchParams;
import io.softwareecg.awb.repository.persistence.search.SearchResult;
import io.softwareecg.awb.repository.persistence.search.expr.Expression;
import io.softwareecg.awb.repository.persistence.search.facet.Facet;
import io.softwareecg.awb.repository.persistence.search.facet.FacetEntry;
import io.softwareecg.awb.repository.persistence.search.facet.FacetParams;
import io.softwareecg.awb.repository.persistence.search.facet.FacetResult;
import io.softwareecg.awb.repository.persistence.schema.EkgSchemaField;
import io.softwareecg.awb.repository.connection.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;
import io.softwareecg.awb.timeseries.codec.DateValuePairCodec;
import io.softwareecg.awb.timeseries.dimension.Metric;
import io.softwareecg.awb.timeseries.dimension.NamedValueEntity;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What a query asks the store of a repository for, and what becomes of the answer. The filters
 * are compared the way Solr gets to see them.
 */
public class StoredSeriesQueryTest {

    private final Map<Class<?>, Object> beans = new HashMap<>();

    private final TimeSeriesRepository store = mock(TimeSeriesRepository.class);

    private final StoreRepository repository = new StoreRepository(store);

    private MetricDataAccessServiceImpl service;

    @Before
    public void startLookup() throws Exception {
        beans.put(BeanScope.class, mock(BeanScope.class));
        beans.put(MetricDataAccessService.class, new MetricDataAccessServiceImpl());

        Lookup.init(new LookupStrategy() {
            @Override
            public <T> T lookup(Class<T> clazz) {
                return clazz.cast(beans.get(clazz));
            }

            @Override
            public <T> T lookup(TypeRef<T> type) {
                return null;
            }

            @Override
            public <T> List<T> lookupAll(Class<T> clazz) {
                T bean = lookup(clazz);
                return bean == null ? List.of() : List.of(bean);
            }

            @Override
            public <T> List<T> lookupAll(TypeRef<T> type) {
                return List.of();
            }
        });
        service = (MetricDataAccessServiceImpl) repository.getBoundedService(MetricDataAccessService.class);
        answerSearch(0, null);
        answerFacet();
    }

    @After
    public void shutdownLookup() {
        Lookup.shutdown();
    }

    // ---------------------------------------------------------------------------------- filters

    @Test
    public void everyDimensionAndTheIntervalFilter() throws Exception {
        assertEquals(List.of(
                "type:(TIME_SERIES)", "project_name:(p)", "ts_hostGroup:(hg)", "ts_host:(h*)", "ts_namespace:(ns)",
                "ts_service:(svc)", "ts_pod:(pod)", "ts_container:(con)", "ts_process:(pr)", "ts_processGroup:(pg)",
                "ts_measurement:(me)", "ts_metricGroup:(mg)", "ts_metricName:(m)",
                "ts_end:[1970-01-01T00:00:01Z TO *]", "ts_start:[* TO 1970-01-01T00:00:02Z]"), filtersOf(everything().build()));
    }

    @Test
    public void onlyTheProjectFiltersWhenNothingIsChosen() throws Exception {
        assertEquals(List.of("type:(TIME_SERIES)", "project_name:(p)"),
                filtersOf(spec().withHost("*").withProcess("").build()));
    }

    @Test
    public void aPatternCanExcludeAndCanNameSeveral() throws Exception {
        assertEquals(List.of("type:(TIME_SERIES)", "project_name:(p)", "(*:* AND NOT ts_host:(web*))", "ts_pod:(one)",
                        "(ts_process:(java) OR ts_process:(postgres*))"),
                filtersOf(spec().withHost("-(web*)").withProcess("|(java|postgres*)").withPod("|(one)").build()));
    }

    @Test
    public void severalMetricsAndAnExcludedOne() throws Exception {
        assertEquals(List.of("type:(TIME_SERIES)", "project_name:(p)", "(*:* AND NOT ts_metricName:(cpu.idle))",
                "(ts_metricName:(cpu.user) OR ts_metricName:(cpu.sys*))"), filtersOf(spec().withExclude("cpu.idle")
                .withMetrics(metrics("cpu.user", "cpu.sys*")).build()));
        assertEquals(List.of("type:(TIME_SERIES)", "project_name:(p)", "ts_metricName:(cpu.user)"),
                filtersOf(spec().withMetrics(metrics("cpu.user")).build()));
        assertEquals(List.of("type:(TIME_SERIES)", "project_name:(p)", "(*:* AND NOT ts_metricName:(cpu*))"),
                filtersOf(spec().withMetric("-(cpu*)").withMetrics(metrics("a", "b")).build()));
    }

    // ------------------------------------------------------------------------------------ pages

    @Test
    public void readsPageByPageInTheOrderOfTime() throws Exception {
        answerSearch(2, "next", series(1, 2, 3));

        TimeSeriesQueryResponse first = service.queryTimeSeriesData(query(spec().build(), "*", 10));

        assertEquals("*", lastSearch().getCursorMark());
        assertEquals("[SortField[field=TS_START, mode=asc], SortField[field=ID, mode=asc]]",
                lastSearch().getSortFields().toString());
        assertEquals("next", first.getCursorId());
        assertEquals(2, first.getTotalHits());
        assertFalse(first.isRequestAborted());
        assertEquals(1, first.getData().size());

        TimeSeriesQueryResponse last = service.queryTimeSeriesData(query(spec().build(), "next", 10));
        assertEquals("next", lastSearch().getCursorMark());
        assertNull(last.getCursorId());
    }

    @Test
    public void tooManySeriesAreNotRead() throws Exception {
        answerSearch(11, "next", series(1));

        TimeSeriesQueryResponse response = service.queryTimeSeriesData(query(spec().build(), "*", 10));

        assertTrue(response.isRequestAborted());
        assertEquals(11, response.getTotalHits());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    public void aSeriesHasItsDimensionsAndTheValuesInsideTheInterval() throws Exception {
        answerSearch(1, null, series(100, 200, 300, 400));

        TimeSeries series = service.queryTimeSeriesData(
                query(spec().withStart(100).withStop(400).build(), "*", 10)).getData().iterator().next();

        assertArrayEquals(new long[]{200, 300}, series.getTimestamps());
        assertEquals(List.of("p", "hg", "h", "ns", "svc", "pod", "con", "me", "pg", "pr", "mg", "m", "key"),
                List.of(series.getProject(), series.getHostGroup(), series.getHost(), series.getNamespace(),
                        series.getService(), series.getPod(), series.getContainer(), series.getMeasurement(),
                        series.getProcessGroup(), series.getProcess(), series.getMetricGroup(),
                        series.getMetricName(), series.getRemoteSeriesKey()));
    }

    @Test
    public void theValuesOfARemoteProjectComeFromItsSource() throws Exception {
        answerSearch(1, null, series());
        RemoteSeriesSource remoteSource = mock(RemoteSeriesSource.class);
        beans.put(RemoteSeriesSource.class, remoteSource);
        when(remoteSource.getImporterID()).thenReturn("importer");
        when(remoteSource.fetchSeries(any(), eq("key"), anyLong(), anyLong()))
                .thenReturn(new Value[]{new Value(7, 1.5)});
        Project remote = new Project("p").setIsSplitSourceProject(true);
        remote.setImporterId("importer");

        TimeSeries series = service.queryTimeSeriesData(query(new QuerySpec.Builder<>().withProject(remote)
                .withMetric("*").withImporterSourceRepository(new TimeSeriesImportSource()).build(), "*", 10))
                .getData().iterator().next();

        assertArrayEquals(new long[]{7}, series.getTimestamps());
    }

    // --------------------------------------------------------------------------- values and ends

    @Test
    public void theValuesOfADimensionAreNotRestrictedByItself() throws Exception {
        Map<EkgSchemaField, BiFunction<MetricDataAccessService, QuerySpec, List<? extends NamedValueEntity>>> values = Map.ofEntries(
                Map.entry(EkgSchemaField.TS_HOST_GROUP_NAME, MetricDataAccessService::getHostGroups),
                Map.entry(EkgSchemaField.TS_HOST_NAME, MetricDataAccessService::getHosts),
                Map.entry(EkgSchemaField.TS_NAMESPACE_NAME, MetricDataAccessService::getNamespaces),
                Map.entry(EkgSchemaField.TS_SERVICE_NAME, MetricDataAccessService::getServices),
                Map.entry(EkgSchemaField.TS_POD_NAME, MetricDataAccessService::getPods),
                Map.entry(EkgSchemaField.TS_CONTAINER_NAME, MetricDataAccessService::getContainers),
                Map.entry(EkgSchemaField.TS_MEASUREMENT, MetricDataAccessService::getMeasurements),
                Map.entry(EkgSchemaField.TS_PROCESS_GROUP_NAME, MetricDataAccessService::getProcessGroups),
                Map.entry(EkgSchemaField.TS_PROCESS_NAME, MetricDataAccessService::getProcesses),
                Map.entry(EkgSchemaField.TS_METRIC_GROUP, MetricDataAccessService::getMetricGroups),
                Map.entry(EkgSchemaField.TS_METRIC_NAME, MetricDataAccessService::getMetricsNames));

        for (var entry : values.entrySet()) {
            clearInvocations(store);
            List<? extends NamedValueEntity> found = entry.getValue().apply(service, everything().build());

            ArgumentCaptor<FacetParams> facet = ArgumentCaptor.forClass(FacetParams.class);
            verify(store).facet(facet.capture());
            FacetParams asked = facet.getValue();
            assertEquals(List.of(entry.getKey()), asked.getFacetFields());
            assertEquals("index", asked.getFacetSort());
            assertEquals(2500, asked.getLimit());
            assertEquals(1, asked.getMinCount());
            String filter = render(asked.getFilterQueries()).toString();
            assertFalse(entry.getKey() + " in " + filter, filter.contains(entry.getKey().getName() + ":"));
            assertEquals(List.of("*", "a", "b"), found.stream().map(NamedValueEntity::getValueName).toList());
        }
    }

    @Test
    public void thereAreNoValuesWhileTheRepositoryIsOffline() throws Exception {
        when(store.facet(any())).thenThrow(new RepositoryException("offline"));

        assertEquals(List.of(), service.getHosts(spec().build()));
    }

    @Test
    public void countsTheValuesOfAProject() throws Exception {
        when(store.sumFieldValue(any(), any())).thenReturn(42L);

        assertEquals(42, service.getAmountMeasuredPointsInProject("p"));
        assertEquals(0, service.getAmountMeasuredPointsInProject(" "));
    }

    @Test
    public void aSeriesEndsWhereItsNewestPartEnds() throws Exception {
        RepositorySeriesEnds ends = new RepositorySeriesEnds(repository);
        SeriesDimensions series = new SeriesDimensions("hg", "h*", "pg", "pr", "mg", "m");
        assertEquals(0, ends.endOf("p", "me", series));

        answerSearch(3, null, series(100, 200));
        assertEquals(200, ends.endOf("p", "me", series));

        assertEquals(List.of("type:(TIME_SERIES)", "project_name:(p)", "ts_measurement:(me)", "ts_hostGroup:(hg)",
                "ts_host:(h\\*)", "ts_processGroup:(pg)", "ts_process:(pr)", "ts_metricGroup:(mg)", "ts_metricName:(m)"), render(lastSearch().getFilterQueries()));
        assertEquals(1, lastSearch().getMaxRows());
        assertEquals("[SortField[field=TS_STOP, mode=desc]]", lastSearch().getSortFields().toString());
    }

    // ------------------------------------------------------------------------------------ tools

    private static QuerySpec.Builder<QuerySpec> spec() {
        return new QuerySpec.Builder<>().withProject(new Project("p")).withMetric("");
    }

    private static QuerySpec.Builder<QuerySpec> everything() {
        return spec().withHostGroup("hg").withHost("h*").withNamespace("ns").withService("svc").withPod("pod")
                .withContainer("con").withProcess("pr").withProcessGroup("pg").withMeasurement("me")
                .withMetricGroup("mg").withMetric("m").withStart(1000).withStop(2000);
    }

    private static Set<Metric> metrics(String... names) {
        Set<Metric> metrics = new LinkedHashSet<>();
        for (String name : names) {
            metrics.add(new Metric(name));
        }
        return metrics;
    }

    private static TimeSeriesQuery query(QuerySpec spec, String cursor, int limit) {
        TimeSeriesQuery query = new TimeSeriesQuery(spec, limit);
        query.setCursorId(cursor);
        return query;
    }

    private List<String> filtersOf(QuerySpec spec) throws Exception {
        service.queryTimeSeriesData(query(spec, "*", 10));
        return render(lastSearch().getFilterQueries());
    }

    private static List<String> render(List<Expression> expressions) {
        return expressions.stream().map(new SolrExpressionRenderer()::render).toList();
    }

    private SearchParams lastSearch() throws RepositoryException {
        ArgumentCaptor<SearchParams> search = ArgumentCaptor.forClass(SearchParams.class);
        verify(store, atLeastOnce()).search(eq(BinaryTimeSeries.class), search.capture());
        return search.getValue();
    }

    private void answerSearch(long hits, String cursor, BinaryTimeSeries... rows) throws RepositoryException {
        when(store.search(eq(BinaryTimeSeries.class), any()))
                .thenReturn(new SearchResult<>(List.of(rows), new QueryStatus(hits, 1, cursor)));
    }

    private void answerFacet() throws RepositoryException {
        Facet facet = new Facet("f");
        facet.addEntry(new FacetEntry("a", 1));
        facet.addEntry(new FacetEntry("b", 1));
        when(store.facet(any())).thenReturn(new FacetResult(List.of(facet), new QueryStatus(2, 1, null)));
    }

    private static BinaryTimeSeries series(long... timestamps) {
        BinaryTimeSeries series = new BinaryTimeSeries();
        series.setProjectName("p");
        series.setHostGroup("hg");
        series.setHost("h");
        series.setNamespace("ns");
        series.setService("svc");
        series.setPod("pod");
        series.setContainer("con");
        series.setMeasurement("me");
        series.setProcessGroup("pg");
        series.setProcess("pr");
        series.setMetricGroup("mg");
        series.setMetric("m");
        series.setRemoteSeriesKey("key");
        if (timestamps.length > 0) {
            series.setStart(timestamps[0]);
            series.setEnd(timestamps[timestamps.length - 1]);
            series.setData(DateValuePairCodec.compressAndEncodeDataPlainBytes(
                    Arrays.stream(timestamps).mapToObj(time -> new Value(time, time)).toList()));
        }
        return series;
    }

    private static class StoreRepository extends RepositoryAdapterBase {

        StoreRepository(TimeSeriesRepository store) {
            this.id = "store";
            this.repositoryName = "store";
            this.cachedClient = store;
        }

        @Override
        protected TimeSeriesRepositoryProvider getRepositoryClientFactory() {
            return null;
        }
    }
}
