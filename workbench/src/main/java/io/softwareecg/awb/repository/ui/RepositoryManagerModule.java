package io.softwareecg.awb.repository.ui;

import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.api.RepositoryService;
import io.softwareecg.awb.repository.events.EditRepositoryRequest;
import io.softwareecg.awb.repository.api.Embedded;
import io.softwareecg.awb.repository.ui.admin.EkgRepositoryAdmin;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.Module;
import io.softwareecg.wfx.platform.api.exceptions.PlatformException;
import io.avaje.inject.External;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.EventObject;

/**
 * todo enter comment here
 *
 * @since 09.07.2018
 */
@Singleton
public class RepositoryManagerModule implements Module {

    private final EkgRepository repository;

    @Inject
    public RepositoryManagerModule(@External @Embedded EkgRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getName() {
        return "Embedded Repository";
    }

    @Override
    public void preload() throws PlatformException {

        EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);
        eventBus.subscribe(EditRepositoryRequest.class, event
                -> new EkgRepositoryAdmin().editRepository((EditRepositoryRequest) event));

        // Pre-warm the embedded Solr stack and the H2 metadata database while
        // the WFX preloader is visible. Both are now lazy:
        //   - Solr: Provider<CoreContainer> + CoreContainerProducer @Bean @Lazy
        //           (see EmbeddedRepositoryClientProvider)
        //   - H2:   first getConnection() opens the file and runs schema.sql
        //           (see H2DataSource)
        // Touching them here shifts the cost into the visible preloader phase.
        repository.getRepositoryClient();
        RepositoryService repoService = repository.getBoundedService(RepositoryService.class);
        if (repoService != null) {
            repoService.listEkgRepositories();
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        repository.close();
    }
}
