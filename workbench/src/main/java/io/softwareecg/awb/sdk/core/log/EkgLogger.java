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
package io.softwareecg.awb.sdk.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating Log instances conveniently. By using this class it is no longer necessary
 * to provide a class when creating a Log instance.
 *
 */
public final class EkgLogger {

    /**
     * Private constructor since there should be no instances of this class.
     */
    private EkgLogger() {
    }

    /**
     * Creates a Logger instance. The log instance is initialized with the class calling
     * this method.
     * INFO: This warning is suppressed as the logger uses an throwable to get the calling class.
     *
     * @return A logger.
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public static Logger get() {
        Throwable t = new Throwable();
        StackTraceElement directCaller = t.getStackTrace()[1];
        return LoggerFactory.getLogger(directCaller.getClassName());
    }
}
