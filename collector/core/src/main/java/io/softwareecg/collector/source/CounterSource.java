//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-03-04 17:28:30 +0100 (Do, 04 Mrz 2010) $
//       revision:      $Revision: 421 $
//       description:   Access performance counter.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________

package io.softwareecg.collector.source;


import io.softwareecg.collector.config.CounterDescriptor;

/**
 * Access performance counter.
 *
 */
public interface CounterSource {

    /**
     * Query counter values for a given group of counters.
     *
     * @param descriptor holds host/counter/process/source.
     * @return the list of measurements (-1 for every case of a measurement error).
     */
    CounterSeries queryCounter(CounterDescriptor descriptor);

    /**
     * @return the unique id which is used in the xml config file.
     */
    String getId();
}
