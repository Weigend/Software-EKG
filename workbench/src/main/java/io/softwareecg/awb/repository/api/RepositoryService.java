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
package io.softwareecg.awb.repository.api;


import java.util.List;

/**
 * The repositories the workbench knows.
 *
 * @since 29.05.2015
 */
public interface RepositoryService {

    /**
     * @return the configured repositories and the embedded one, each of them once
     */
    List<Repository> listRepositories();

    /**
     * Removes a repository from the workbench for good: its connection configuration and the
     * metadata of all projects stored for it (including their aliases, bookmarks and import
     * sources). Without the repository these projects could never be shown again.
     * <p>
     * Works on the local metadata only. The (remote) repository is not contacted and its time
     * series stay untouched, so this also works while the repository is unreachable.
     *
     * @param repository the repository to remove; must not be the embedded one
     */
    void removeRepositoryWithProjects(Repository repository);
}
