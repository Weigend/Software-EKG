package io.softwareecg.awb.queryengine;

import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.importer.ImportModule;
import io.softwareecg.awb.importer.spi.RemoteSeriesDataFetcher;
import io.softwareecg.awb.importer.spi.SplitSourceProjectSupport;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple map based registry used to define a single point of contact to
 * register and retrieve RemoteSeriesDataFetcher instances provides by different importers.
 * The registry is application-wide and should be accessed via WFX lookup.
 *
 * @since 01.08.2018
 */
@Singleton
public class SeriesDataFetcherRegistry {

    private static final Logger LOG = EkgLogger.get();

    private final Map<String, RemoteSeriesDataFetcher> registeredDataFetcher = new HashMap<>();

    private boolean initializationComplete = false;

    public void init() {

        List<ImportModule> modules = Lookup.lookupAll(ImportModule.class);

        if (modules == null) {
            LOG.error("Unable to retrieve import modules");
            return;
        }

        for (ImportModule module : modules) {
            if (module instanceof SplitSourceProjectSupport) {
                registerSeriesDataFetcher(module.getImporterID(),  ((SplitSourceProjectSupport) module).getSeriesDataFetcher());
            }
        }

        initializationComplete = true;
    }

    /**
     * Register a new instance of {@link RemoteSeriesDataFetcher} to the global registry.
     * If already a fetcher exists for the specified importerId, the existing one will overwritten.
     *
     * @param importerId the id of the importer
     * @param fetcher an instance of RemoteSeriesDataFetcher
     */
    public void registerSeriesDataFetcher(String importerId, RemoteSeriesDataFetcher fetcher) {
        registeredDataFetcher.put(importerId, fetcher);
    }

    /**
     * Return a {@link RemoteSeriesDataFetcher} that was registered for the given key before.
     * If no fetcher exists null will returned.
     *
     * @param importerId the id of the importer the RemoteSeriesDataFetcher was registered for
     * @return the RemoteSeriesDataFetcher instance or null.
     */
    public synchronized RemoteSeriesDataFetcher retrieveSeriesDataFetcher(String importerId) {

        if (!initializationComplete) {
            init();
        }

        return registeredDataFetcher.get(importerId);
    }

}
