//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.explorer.ui.items;


import io.avaje.inject.External;
import io.softwareecg.awb.common.ui.explorer.api.AbstractItem;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.api.RepositoryService;
import io.softwareecg.awb.da.api.types.Embedded;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The task to read all available types.
 *
 * @since 08.04.2014
 */
@Singleton
public class RepositoryItemsTask {

    private final EkgRepository embeddedRepo;

    @Inject
    public RepositoryItemsTask(@External @Embedded EkgRepository embeddedRepo) {
        this.embeddedRepo = embeddedRepo;
    }

    /**
     * Load the types for the explorerView tree.
     *
     * @return A list with all types items.
     */
    public List<AbstractItem> getRepositoryItems() {

        RepositoryService service = embeddedRepo.getBoundedService(RepositoryService.class);
        List<EkgRepository> repositories = service.listEkgRepositories();
 
        return repositories
                .stream()
                .map(repository -> new RepositoryItem(repository.getRepositoryName(), true, repository))
                .collect(Collectors.toList());
    }
}
