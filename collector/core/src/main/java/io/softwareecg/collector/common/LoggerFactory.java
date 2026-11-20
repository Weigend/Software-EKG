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
package io.softwareecg.collector.common;

import org.slf4j.Logger;


/**
 * Creates slf4j logging logger instances.
 *
 * @version $Id: LoggerFactory.java 6354 2011-04-29 14:09:40Z fuchshuber $
 */
public final class LoggerFactory {

    /**
     * Private constructor since there should be no instances of this class.
     */
    private LoggerFactory() {
    }

    /**
     * Creates a logger instance.
     * The logger instance is initialized with the class calling this method.
     *
     * @return a logger instance
     */
    public static Logger create() {
        StackTraceElement directCaller = Thread.currentThread().getStackTrace()[2];

        return create(directCaller.getClassName());
    }

    /**
     * Creates a logger instance.
     * The logger instance is initialized with the given class.
     *
     * @param clazz the class
     * @return a logger instance
     */
    public static Logger create(Class<?> clazz) {
        return org.slf4j.LoggerFactory.getLogger(clazz);
    }

    /**
     * Creates a logger instance.
     * The logger instance is initialized with the given class name.
     *
     * @param name the class name
     * @return a logger instance
     */
    public static Logger create(String name) {
        return org.slf4j.LoggerFactory.getLogger(name);
    }
}
