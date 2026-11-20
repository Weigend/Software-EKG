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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for delayed counters. The execution will be after all other counters are processed.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Delayed {
}
