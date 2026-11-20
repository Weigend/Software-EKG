package io.softwareecg.awb.datasource.bl;

import io.softwareecg.wfx.platform.api.Module;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ties data collection to the life of the application: all stored collection plans start
 * with the AWB and stop when it is closed.
 */
@Singleton
@SuppressWarnings("unused") // discovered by the platform
public class DataCollectionModule implements Module {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataCollectionModule.class);

    private final DataCollectionService service;

    @Inject
    public DataCollectionModule(DataCollectionService service) {
        this.service = service;
    }

    @Override
    public void preload() {
        // nothing to prepare before the application window is there
    }

    @Override
    public void start() {
        // off the startup thread: opening the metadata store and the first queries take a moment
        Thread starter = new Thread(() -> {
            try {
                service.startAll();
            } catch (RuntimeException e) {
                LOGGER.warn("Could not start the stored data collections: {}", e.getMessage(), e);
            }
        }, "start-data-collection");
        starter.setDaemon(true);
        starter.start();
    }

    @Override
    public void stop() {
        service.stopAll();
    }
}
