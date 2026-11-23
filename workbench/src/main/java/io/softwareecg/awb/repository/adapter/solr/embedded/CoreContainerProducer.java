//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.repository.adapter.solr.embedded;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.Lazy;
import jakarta.inject.Inject;
import org.apache.solr.core.CoreContainer;

import java.nio.file.Paths;

/**
 * Produces the core container for solr.
 */
@Factory
public class CoreContainerProducer {

    private final SolrConfiguration config;

    @Inject
    public CoreContainerProducer(SolrConfiguration config) {
        this.config = config;
    }

    /**
     * Produces the core container and inject the correct configuration.
     *
     * @return The fully instantiated core container.
     */
    @Bean(destroyMethod = "shutdown")
    @Lazy
    public CoreContainer createCoreContainer() {
        return CoreContainer.createAndLoad(
                Paths.get(config.getSolrHomeDir().toURI()),
                Paths.get(config.getSolrConfig().toURI())
        );
    }

}
