package io.softwareecg.awb.query;

import com.sun.net.httpserver.HttpServer;
import io.softwareecg.awb.importer.adapter.prometheus.PrometheusImportModule;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.query.model.TimeSeriesQuery;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.SeriesPage;
import io.softwareecg.awb.repository.api.SeriesSearch;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The values of a remote project are read from the server it was imported from, through the
 * importer that knows how.
 */
public class RemoteProjectQueryTest {

    private final List<HttpServer> servers = new ArrayList<>();

    private final List<Object> beans = new ArrayList<>();

    private MetricDataAccessServiceImpl service;

    @Before
    public void startLookup() throws Exception {
        beans.add(new SimpleEventBus());
        beans.add(new PrometheusImportModule());
        Lookup.init(new LookupStrategy() {
            @Override
            public <T> T lookup(Class<T> clazz) {
                return lookupAll(clazz).stream().findFirst().orElse(null);
            }

            @Override
            public <T> T lookup(TypeRef<T> type) {
                return null;
            }

            @Override
            public <T> List<T> lookupAll(Class<T> clazz) {
                return beans.stream().filter(clazz::isInstance).map(clazz::cast).toList();
            }

            @Override
            public <T> List<T> lookupAll(TypeRef<T> type) {
                return List.of();
            }
        });

        SeriesSearch series = mock(SeriesSearch.class);
        when(series.find(any(), any(), anyLong())).thenAnswer(call -> {
            TimeSeries stored = new TimeSeries("up");
            stored.setRemoteSeriesKey("{\"METRIC_NAME\": \"up\"}");
            return new SeriesPage(1, List.of(stored), null);
        });
        Repository repository = mock(Repository.class);
        when(repository.series()).thenReturn(series);
        service = new MetricDataAccessServiceImpl(repository);
    }

    @After
    public void stop() {
        servers.forEach(server -> server.stop(0));
        Lookup.shutdown();
    }

    @Test
    public void everyProjectIsReadFromItsOwnServer() throws Exception {
        TimeSeriesImportSource first = prometheusWith(1);
        TimeSeriesImportSource second = prometheusWith(2);

        assertArrayEquals(new double[]{1}, read(first, PrometheusImportModule.IMPORT_MODULE_ID), 0);
        assertArrayEquals(new double[]{2}, read(second, PrometheusImportModule.IMPORT_MODULE_ID), 0);
        assertArrayEquals(new double[]{1}, read(first, PrometheusImportModule.IMPORT_MODULE_ID), 0);
    }

    @Test
    public void anImporterThatIsNotThereIsAnError() throws Exception {
        TimeSeriesImportSource source = prometheusWith(1);

        Exception e = assertThrows(IllegalStateException.class, () -> read(source, "NoSuchImporter"));
        assertTrue(e.getMessage(), e.getMessage().contains("NoSuchImporter"));
    }

    private double[] read(TimeSeriesImportSource source, String importerId) throws Exception {
        Project project = new Project("p").setIsSplitSourceProject(true);
        project.setImporterId(importerId);
        QuerySpec query = new QuerySpec.Builder<>().withProject(project).withMetric("up")
                .withImporterSourceRepository(source).withStart(1000).withStop(2000).build();

        List<TimeSeries> series = new ArrayList<>(service.queryTimeSeriesData(new TimeSeriesQuery(query, 10)).getData());

        assertEquals(1, series.size());
        return series.get(0).getValuesArray();
    }

    /** A Prometheus server whose every series has this one value. */
    private TimeSeriesImportSource prometheusWith(int value) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = ("{\"status\": \"success\", \"data\": {\"resultType\": \"matrix\", \"result\": "
                    + "[{\"metric\": {}, \"values\": [[1.5, \"" + value + "\"]]}]}}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        servers.add(server);

        TimeSeriesImportSource source = new TimeSeriesImportSource();
        source.setId("server " + value);
        source.setRepositoryUrl("http://127.0.0.1:" + server.getAddress().getPort());
        source.setAuthType(ResourceAuthType.NONE);
        return source;
    }
}
