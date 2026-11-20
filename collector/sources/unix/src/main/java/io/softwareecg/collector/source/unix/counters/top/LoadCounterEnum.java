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
package io.softwareecg.collector.source.unix.counters.top;

/**
 * The Enum LoadCounterEnum.
 * <p>
 * Example: load average: 0.09, 0.05, 0.01
 * <p>
 * - First (0.09) is the one<p>
 * - Second (0.05) is the five<p>
 * - Third (0.01) is the fifteen<p>
 * minute(s) average load.
 */
public enum LoadCounterEnum {

    /**
     * The one minute average load
     */
    one,
    /**
     * The five minutes average load
     */
    five,
    /**
     * The fifteen minutes average load
     */
    fifteen
}

