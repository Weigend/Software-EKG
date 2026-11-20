//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Weigend AM
//       creation date: 2009 - 2013
//       changed by:    $Author$
//       change date:   $Date$
//       revision:      $Revision$
//       description:   Collector UI
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.ui.util;

/**
 */
public class PasswortVerwaltung {

    private static String password = "1234";

    /**
     * Checks whether the correct password was entered or not
     *
     * @param login password which will be checked
     * @return true if login is the password
     */
    public static boolean checkLogin(String login) {
        return password.equals(login);
    }

    /**
     * Sets new password if login is correct
     *
     * @param login
     * @param newPassword
     * @return true if changing password was successfull
     */
    public static boolean setPasswort(String login, String newPassword) {
        if (newPassword.length() != 4) {
            throw new RuntimeException("Incorrect password '" + newPassword + "' length: " + newPassword.length() + " instead of 4.");
        }
        if (login.equals(password)) {
            password = newPassword;
            return true;
        }
        return false;
    }
}
