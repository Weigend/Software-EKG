//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.repository;


import io.softwareecg.collector.config.CounterDescriptor;

/**
 * Factory that will provide a {@link StaticTimeSeriesRepository} repository implementation
 * that will selected and configured based on the collection configuration.
 *
 * @since 08.08.2018
 */
public interface
RepositoryFactory {

    /**
     * Returns a concrete StaticTimeSeriesRepository implementation based on the current
     * EKG Collector configuration.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return a concrete repository that implements the StaticTimeSeriesRepository api
     */
    StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor);
}
