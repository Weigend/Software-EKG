/**
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: c.amann
 * creation date: 12.03.14 11:29
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.common;

/**
 * Miscellaneous helper functions
 */
public final class Validate {

    /**
     * Utility class
     */
    private Validate(){

    }

    /**
     * @param args the arguments
     * @return true if any argument is not null, otherwise false
     */
    public static boolean nullCheckArgs(Object... args) {
        //If we have one argument to check
        if (args == null) {
            return true;
        }

        //If we have more arguments to check
        for (Object arg : args) {
            if(arg==null) {
                return true;
            }
        }
        return false;
    }
}
