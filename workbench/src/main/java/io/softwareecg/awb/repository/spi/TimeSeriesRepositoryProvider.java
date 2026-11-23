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
package io.softwareecg.awb.repository.spi;

import io.softwareecg.awb.repository.port.TimeSeriesRepository;

import java.util.List;


/**
 * Interface that represents an resolver for
 * {@link TimeSeriesRepository} instances.
 *
 * @since 03.06.2015
 */
public interface TimeSeriesRepositoryProvider {

    /** Repository types contributed by this provider, including UI metadata and validation. */
    default List<RepositoryTypeDescriptor> repositoryTypes() {
        return List.of();
    }

    TimeSeriesRepository createRepositoryClient(String connectionUrl, String userName, String password);

    TimeSeriesRepository createRepositoryClient(String clusterManagerUrl, String repositoryName, String userName, String password);
}
