package io.softwareecg.awb.repository.connection;

import io.softwareecg.awb.metadata.model.RepositoryConfigRow;
import io.softwareecg.awb.metadata.port.RepositoryConfigStore;
import io.softwareecg.awb.repository.adapter.clickhouse.ClickHouseCloudRepository;
import io.softwareecg.awb.repository.adapter.solr.SolrClassicRepository;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A technology may need a name for the index of a repository where the user entered none.
 */
public class RepositoryConfigurationTest {

    private final RepositoryConfigStore store = mock(RepositoryConfigStore.class);

    private final RepositoryDataAccessServiceImpl configuration =
            new RepositoryDataAccessServiceImpl(mock(EkgRepository.class), store);

    @Before
    @SuppressWarnings("unchecked")
    public void startLookup() {
        EventBus<java.util.EventObject> eventBus = new SimpleEventBus();
        List<EkgRepository.Factory> factories = List.of(
                new SolrClassicRepository.Factory(null), new ClickHouseCloudRepository.Factory(null));
        Lookup.init(new LookupStrategy() {
            @Override
            public <T> T lookup(Class<T> clazz) {
                return clazz == EventBus.class ? clazz.cast(eventBus) : null;
            }

            @Override
            public <T> T lookup(TypeRef<T> type) {
                return null;
            }

            @Override
            public <T> List<T> lookupAll(Class<T> clazz) {
                return clazz == EkgRepository.Factory.class ? (List<T>) factories : List.of();
            }

            @Override
            public <T> List<T> lookupAll(TypeRef<T> type) {
                return List.of();
            }
        });
    }

    @After
    public void shutdownLookup() {
        Lookup.shutdown();
    }

    @Test
    public void clickHouseGetsATableNameFromTheNameOfTheRepository() {
        assertEquals("my_plant", storedIndexName(EkgRepositoryDbType.CLICKHOUSE_CLOUD, "", "My Plant"));
    }

    @Test
    public void anEnteredNameIsStored() {
        assertEquals("series", storedIndexName(EkgRepositoryDbType.CLICKHOUSE_CLOUD, " series ", "My Plant"));
    }

    @Test
    public void theOtherTechnologiesStoreWhatWasEntered() {
        assertEquals("", storedIndexName(EkgRepositoryDbType.SOLR_STANDALONE, "", "My Plant"));
    }

    private String storedIndexName(EkgRepositoryDbType type, String indexName, String name) {
        configuration.addEkgRepository(name, "http://127.0.0.1:1", indexName, type, ResourceAuthType.NONE, null, null);
        ArgumentCaptor<RepositoryConfigRow> row = ArgumentCaptor.forClass(RepositoryConfigRow.class);
        verify(store).insert(row.capture());
        return row.getValue().dbIndexName;
    }
}
