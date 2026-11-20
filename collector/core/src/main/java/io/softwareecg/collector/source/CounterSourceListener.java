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


/**
 * Will be notified if a new dynamic counter is added.
 */
public interface CounterSourceListener {

    /**
     * Method will be called if changed in wildcard or generic counters are detected.
     * The collector will than create a new storage with the new set of series.
     */
    void counterChanged();
}
