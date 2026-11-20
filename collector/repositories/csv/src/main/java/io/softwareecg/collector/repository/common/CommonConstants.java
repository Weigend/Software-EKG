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
package io.softwareecg.collector.repository.common;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Common constants.
 */
public final class CommonConstants {

    /**
     * The date format (uses 24h (HH) format).
     */
    private static final DateFormat DATE_LOGFORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale.US);

    /**
     * Our formatter.
     */
    private static final DecimalFormat EN_DECIMAL_LOGFORMAT = (DecimalFormat) DecimalFormat.getInstance(Locale.ENGLISH);


    /**
     * Utility class.
     */
    private CommonConstants() {

    }

    /**
     * Gets the date format for storage directories
     *
     * @return the date format.
     */
    public static DateFormat getDateFormat() {
        return (DateFormat) DATE_LOGFORMAT.clone();
    }

    /**
     * Gets the decimal format for csv logfiles.
     *
     * @return a decimal format.
     */
    public static DecimalFormat getDecimalFormat() {
        return (DecimalFormat) EN_DECIMAL_LOGFORMAT.clone();
    }

}
