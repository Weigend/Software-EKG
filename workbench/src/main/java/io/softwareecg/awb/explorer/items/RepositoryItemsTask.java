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
package io.softwareecg.awb.explorer.items;


import io.softwareecg.awb.explorer.tree.AbstractItem;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositoryService;
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

    private final RepositoryService repositoryService;

    @Inject
    public RepositoryItemsTask(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Load the types for the explorerView tree.
     *
     * @return A list with all types items.
     */
    public List<AbstractItem> getRepositoryItems() {

        List<Repository> repositories = repositoryService.listRepositories();
        return repositories
                .stream()
                .map(repository -> new RepositoryItem(repository.getRepositoryName(), true, repository))
                .collect(Collectors.toList());
    }
}
