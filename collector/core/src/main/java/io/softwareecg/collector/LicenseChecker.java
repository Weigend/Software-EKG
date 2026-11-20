//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: pospiech $
//       change date:   $Date: 2009-12-16 01:47:26 +0100 (Mi, 16 Dez 2009) $
//       revision:      $Revision: 190 $
//       description:   License Checker.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Simple implementation.
 */
public final class LicenseChecker {


    /**
     *  Simple implementation.
     */
    private static final Date VALID_UNTIL;

    static {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2015, Calendar.DECEMBER, 31);

        VALID_UNTIL = cal.getTime();
    }

    /**
     * Utility class.
     */
    private LicenseChecker() {

    }

    /**
     * Simple implementation checks for an end date.
     * @return true if the actual system date (now) is higher than the VALID_UNTIL date.
     */
    public static boolean isInvalid() {
        return new Date().getTime() > VALID_UNTIL.getTime();
    }
}
