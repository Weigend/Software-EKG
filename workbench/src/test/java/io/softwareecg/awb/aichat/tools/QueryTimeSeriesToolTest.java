package io.softwareecg.awb.aichat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.avaje.inject.BeanScope;
import io.softwareecg.awb.query.execution.MetricDataAccessServiceImpl;
import io.softwareecg.awb.query.execution.QueryRunner;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.query.api.MetricQueryService;
import io.softwareecg.awb.query.pipeline.SeriesTransformPipeline;
import io.softwareecg.awb.query.pipeline.simplification.algorithms.DouglasPeuckerSimplificationService;
import io.softwareecg.awb.query.pipeline.smoothing.SmootherFactory;
import io.softwareecg.awb.repository.adapter.RepositoryAdapterBase;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.connection.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.persistence.BinaryTimeSeries;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.persistence.search.QueryStatus;
import io.softwareecg.awb.repository.persistence.search.SearchResult;
import io.softwareecg.awb.repository.persistence.search.facet.FacetResult;
import io.softwareecg.awb.timeseries.Value;
import io.softwareecg.awb.timeseries.codec.DateValuePairCodec;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What the assistant gets to see of the time series.
 */
public class QueryTimeSeriesToolTest {

    private final Map<Class<?>, Object> beans = new HashMap<>();

    private final TimeSeriesRepository store = mock(TimeSeriesRepository.class);

    @Before
    @SuppressWarnings("unchecked")
    public void startLookup() throws Exception {
        EventBus<java.util.EventObject> eventBus = new SimpleEventBus();
        SeriesTransformPipeline pipeline =
                new SeriesTransformPipeline(new DouglasPeuckerSimplificationService(), new SmootherFactory());
        beans.put(EventBus.class, eventBus);
        beans.put(BeanScope.class, mock(BeanScope.class));
        beans.put(SeriesTransformPipeline.class, pipeline);
        beans.put(MetricDataAccessService.class, new MetricDataAccessServiceImpl());
        beans.put(MetricQueryService.class, new QueryRunner(eventBus, pipeline));

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
        when(store.facet(any())).thenReturn(new FacetResult(List.of(), new QueryStatus(0, 1, null)));
        stored(List.of());
    }

    @After
    public void shutdownLookup() {
        Lookup.shutdown();
    }

    @Test
    public void aSeriesThatIsStoredInPartsIsOneSeries() throws Exception {
        stored(List.of(part("a", 1000, 1, 2), part("a", 3000, 3, 4), part("b", 1000, 5)));

        JsonNode answer = ask("{\"project\": \"p\", \"metric\": \"m\"}");

        assertEquals(2, answer.get("total_series_found").asInt());
        assertEquals(2, answer.get("series_returned").asInt());
        JsonNode first = answer.get("series").get(0);
        assertEquals("a", first.get("host").asText());
        assertEquals(4, first.get("points").asInt());
        assertEquals("1970-01-01 00:00:01", first.get("start_time").asText());
        assertEquals("1970-01-01 00:00:04", first.get("end_time").asText());
    }

    /** Nine values of ten are 0, so that the points that show the course are not typical of them. */
    @Test
    public void theStatisticsAreOfAllValues() throws Exception {
        double[] values = IntStream.range(0, 1000).mapToDouble(i -> i % 10 == 0 ? 100 : 0).toArray();
        stored(List.of(part("a", 0, values)));

        JsonNode series = ask("{\"project\": \"p\", \"metric\": \"m\", \"include_values\": true, \"max_points\": 50}")
                .get("series").get(0);

        JsonNode stats = series.get("stats");
        assertEquals(1000, stats.get("count").asInt());
        assertEquals(10.0, stats.get("avg").asDouble(), 0.001);
        assertEquals(0.0, stats.get("min").asDouble(), 0.001);
        assertEquals(100.0, stats.get("max").asDouble(), 0.001);
        assertTrue("values: " + series.get("values").size(), series.get("values").size() <= 50);
        assertTrue("values: " + series.get("values").size(), series.get("values").size() > 10);
    }

    @Test
    public void fewValuesAreAllGiven() throws Exception {
        stored(List.of(part("a", 1000, 1.234, 5)));

        JsonNode series = ask("{\"project\": \"p\", \"metric\": \"m\", \"include_values\": true}").get("series").get(0);

        assertEquals("[[\"1970-01-01 00:00:01\",1.23],[\"1970-01-01 00:00:02\",5.0]]", series.get("values").toString());
    }

    @Test
    public void valuesAreOnlyGivenWhenAskedFor() throws Exception {
        stored(List.of(part("a", 1000, 1, 2)));

        JsonNode series = ask("{\"project\": \"p\", \"metric\": \"m\"}").get("series").get(0);

        assertFalse(series.has("values"));
        assertEquals(2, series.get("stats").get("count").asInt());
    }

    @Test
    public void atMostFiftySeriesAreGiven() throws Exception {
        List<BinaryTimeSeries> parts = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            parts.add(part("host" + i, 1000, i));
        }
        stored(parts);

        JsonNode answer = ask("{\"project\": \"p\"}");

        assertEquals(50, answer.get("series").size());
        assertEquals(50, answer.get("series_returned").asInt());
        assertEquals("more than 50", answer.get("total_series_found").asText());
        assertTrue(answer.get("truncated").asBoolean());
        assertTrue(answer.get("hint").asText(), answer.get("hint").asText().contains("more specific filters"));
    }

    @Test
    public void fiftySeriesAreAllOfThem() throws Exception {
        List<BinaryTimeSeries> parts = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            parts.add(part("host" + i, 1000, i));
        }
        stored(parts);

        JsonNode answer = ask("{\"project\": \"p\"}");

        assertEquals(50, answer.get("total_series_found").asInt());
        assertEquals(50, answer.get("series").size());
        assertFalse(answer.has("truncated"));
    }

    @Test
    public void nothingFoundSaysHowToFindAMetric() throws Exception {
        JsonNode answer = ask("{\"project\": \"p\", \"metric\": \"m\"}");

        assertEquals(0, answer.get("total_series_found").asInt());
        assertTrue(answer.get("hint").asText().contains("list_available_data"));
    }

    @Test
    public void aRepositoryThatCannotBeSearchedIsAnError() throws Exception {
        when(store.search(any(), any())).thenThrow(new RepositoryException("no connection"));

        assertTrue(ask("{\"project\": \"p\", \"metric\": \"m\"}").get("error").asText().contains("no connection"));
    }

    @Test
    public void aProjectAndARepositoryAreNeeded() throws Exception {
        assertTrue(ask("{}").get("error").asText().contains("project"));
        assertTrue(new ObjectMapper().readTree(tool(null).execute("{\"project\": \"p\"}"))
                .get("error").asText().contains("No repository connection"));
    }

    private JsonNode ask(String arguments) throws Exception {
        return new ObjectMapper().readTree(tool(new StoreRepository(store)).execute(arguments));
    }

    private static QueryTimeSeriesTool tool(Repository repository) {
        return new QueryTimeSeriesTool(
                () -> repository == null ? null : repository.getBoundedService(MetricDataAccessService.class),
                () -> repository == null ? null : repository.getBoundedService(MetricQueryService.class),
                () -> null, () -> ZoneId.of("UTC"));
    }

    private void stored(List<BinaryTimeSeries> parts) throws RepositoryException {
        when(store.search(eq(BinaryTimeSeries.class), any()))
                .thenReturn(new SearchResult<>(parts, new QueryStatus(parts.size(), 1, "*")));
    }

    /** A part of a series with a value every second. */
    private static BinaryTimeSeries part(String host, long start, double... values) {
        BinaryTimeSeries part = new BinaryTimeSeries();
        part.setProjectName("p");
        part.setHost(host);
        part.setMetric("m");
        part.setData(DateValuePairCodec.compressAndEncodeDataPlainBytes(IntStream.range(0, values.length)
                .mapToObj(i -> new Value(start + i * 1000L, values[i])).toList()));
        return part;
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
