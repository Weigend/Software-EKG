package io.softwareecg.awb.metadata.adapter.h2;

import io.softwareecg.awb.metadata.api.CollectionSourceEvent;
import io.softwareecg.awb.metadata.api.CollectionSourceRow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The storage of data sources against a real H2 database with the real schema, in memory.
 */
public class H2CollectionSourceRepositoryTest {

    private H2MetadataDataSource dataSource;

    private H2CollectionSourceStore store;

    @Before
    public void openDatabase() throws Exception {
        dataSource = new H2MetadataDataSource("jdbc:h2:mem:sources-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        store = new H2CollectionSourceStore(dataSource);
        execute("INSERT INTO projects (id, name) VALUES ('p1', 'Shop'), ('p2', 'Billing')");
    }

    @After
    public void closeDatabase() {
        dataSource.shutdown();
    }

    @Test
    public void aProjectCanHaveSeveralSourcesEachWithItsOwnIdentity() {
        CollectionSourceRow first = store.insert(source("Shop", "http://a:9090", "source-a"));
        CollectionSourceRow second = store.insert(source("Shop", "http://b:9090", null));
        store.insert(source("Billing", "http://c:9090", null));

        assertNotNull(first.id);
        assertFalse(first.id.equals(second.id));
        assertEquals(List.of("http://a:9090", "http://b:9090"),
                store.listByProject("Shop").stream().map(row -> row.endpoint).collect(Collectors.toList()));
        assertEquals(3, store.listAll().size());

        CollectionSourceRow read = store.find(first.id);
        assertEquals("Shop", read.projectName);
        assertEquals("prometheus", read.sourceType);
        assertEquals("EMBEDDED", read.executionMode);
        assertEquals("project: Shop", read.config);
        assertEquals("source-a", read.secretRef);
        assertTrue(read.enabled);
        assertNotNull(read.createdDate);
        assertNull(store.find("unknown"));
    }

    @Test
    public void theNameOfItsSeriesStaysWithASourceWhateverElseChanges() {
        CollectionSourceRow source = source("Shop", "http://a:9090", null);
        source.seriesNamespace = "live-a";
        store.insert(source);
        assertEquals("live-a", store.find(source.id).seriesNamespace);
        assertNull("a source stored without one has none",
                store.insert(source("Shop", "http://b:9090", null)).seriesNamespace);

        source.endpoint = "http://moved:9090";
        source.seriesNamespace = "something-else";
        store.update(source);

        assertEquals("http://moved:9090", store.find(source.id).endpoint);
        assertEquals("live-a", store.find(source.id).seriesNamespace);
    }

    @Test
    public void connectionAndEnabledFlagCanBeChanged() {
        CollectionSourceRow source = store.insert(source("Shop", "http://a:9090", "source-a"));

        source.endpoint = "http://moved:9090";
        source.config = "project: Moved";
        source.secretRef = null;
        source.enabled = false;
        assertTrue(store.update(source));

        CollectionSourceRow read = store.find(source.id);
        assertEquals("http://moved:9090", read.endpoint);
        assertEquals("project: Moved", read.config);
        assertNull(read.secretRef);
        assertFalse(read.enabled);

        source.id = "unknown";
        assertFalse(store.update(source));
    }

    @Test
    public void historyIsKeptNewestFirstAndBounded() {
        CollectionSourceRow source = store.insert(source("Shop", "http://a:9090", null));

        store.addEvent(source.id, "STARTING", null);
        store.addEvent(source.id, "ERROR", "connection refused");
        store.addEvent(source.id, "RUNNING", null);

        List<CollectionSourceEvent> events = store.listEvents(source.id, 2);
        assertEquals(List.of("RUNNING", "ERROR"), events.stream().map(CollectionSourceEvent::state).collect(Collectors.toList()));
        assertEquals("connection refused", events.get(1).message());
        assertNotNull(events.get(0).time());

        for (int i = 0; i < 230; i++) {
            store.addEvent(source.id, "RUNNING", "entry " + i);
        }
        List<CollectionSourceEvent> all = store.listEvents(source.id, 1000);
        assertEquals("only the newest entries are kept", 200, all.size());
        assertEquals("entry 229", all.get(0).message());
    }

    @Test
    public void deletingASourceRemovesItsHistory() throws Exception {
        CollectionSourceRow source = store.insert(source("Shop", "http://a:9090", null));
        store.addEvent(source.id, "RUNNING", null);

        store.delete(source.id);

        assertNull(store.find(source.id));
        assertEquals(0, count("collection_source_events"));
    }

    @Test
    public void deletingAProjectRemovesItsSourcesAndTheirHistory() throws Exception {
        CollectionSourceRow shop = store.insert(source("Shop", "http://a:9090", null));
        CollectionSourceRow billing = store.insert(source("Billing", "http://c:9090", null));
        store.addEvent(shop.id, "RUNNING", null);
        store.addEvent(billing.id, "RUNNING", null);

        execute("DELETE FROM projects WHERE name = 'Shop'");

        assertNull(store.find(shop.id));
        assertNotNull(store.find(billing.id));
        assertEquals(1, count("collection_source_events"));
    }

    private static CollectionSourceRow source(String project, String endpoint, String secretRef) {
        CollectionSourceRow row = new CollectionSourceRow();
        row.projectName = project;
        row.sourceType = "prometheus";
        row.executionMode = "EMBEDDED";
        row.endpoint = endpoint;
        row.config = "project: Shop";
        row.secretRef = secretRef;
        return row;
    }

    private void execute(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private int count(String table) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
