//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-03-04 16:00:42 +0100 (Do, 04 Mrz 2010) $
//       revision:      $Revision: 419 $
//       description:   Wildcards
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source;


import io.softwareecg.collector.config.CounterDescriptor;

/**
 * API of counter sources that support the expansion of wildcard
 * metric definitions to a set of matching concrete metric definitions.
 * This interface is used to mark which sources have wildcard support and
 * to hide/standardized the various concrete implementations for that.
 *
 * @since 18.03.2014
 */
public interface CounterWildcardSupport {

    /**
     * Expand the wildcard metrics definitions to 0-* concrete metric definitions
     * that matches to the wildcard expression and describe metrics that are available
     * at the counter source.
     *
     * The wildcard expressions must use the '*' character to define which part of the metric name can match
     * various character sequences. Other syntax like regex may work if the concrete source supports it
     * but isn't grand by the API
     *
     * Metric definitions that doesn't include any wildcard expression will returned unchanged in the result.
     *
     * In case of errors an response with an empty array of metric definitions will returned.
     * Use the {@link ExpandWildcardResultType} to get more details about processing and it's result
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return an ExpandWildcardResponse that serves the expanded wildcard definitions and a processing state enum
     */
    ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor);

    /**
     * Method to register a listener that will be invoked every time the wildcard expansion results
     * in different concrete metrics as before.
     *
     * @param sourceListener the listener.
     */
    void addCounterListener(CounterSourceListener sourceListener);
}
