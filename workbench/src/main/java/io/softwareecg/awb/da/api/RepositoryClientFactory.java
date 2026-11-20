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
package io.softwareecg.awb.da.api;


/**
 * Interface that represents an resolver for
 * {@link RepositoryClient} instances.
 *
 * @since 03.06.2015
 */
public interface RepositoryClientFactory {

    RepositoryClient createRepositoryClient(String connectionUrl, String userName, String password);

    RepositoryClient createRepositoryClient(String clusterManagerUrl, String repositoryName, String userName, String password);
}
