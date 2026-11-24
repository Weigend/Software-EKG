package io.softwareecg.awb.repository.adapter.solr.embedded;

import io.softwareecg.wfx.platform.api.Module;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

/**
 * Says where the index of the embedded Solr is: it belongs to the user, not to the build or the
 * installation ({@link UserSolrHome}).
 * <p>
 * This has to be known before anything asks for Solr - importers do when they are loaded - so the
 * module comes before all others. Modules are loaded by the application only: a test that wires
 * beans does not touch the data of the user.
 */
@Singleton
@Priority(EmbeddedSolrModule.BEFORE_ALL_OTHERS)
@SuppressWarnings("unused") // found by the injection scan
public class EmbeddedSolrModule implements Module {

    static final int BEFORE_ALL_OTHERS = 1;

    @Override
    public String getName() {
        return "Embedded Solr";
    }

    @Override
    public void preload() {
        UserSolrHome.useUnlessConfigured();
    }

    @Override
    public void start() {
        // nothing to start: Solr starts when it is first asked for
    }

    @Override
    public void stop() {
        // the container is closed with its bean
    }
}
