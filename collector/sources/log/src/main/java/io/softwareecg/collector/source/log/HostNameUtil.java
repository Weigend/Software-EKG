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
package io.softwareecg.collector.source.log;

/**
 * Helper class for host names.
 */
public final class HostNameUtil {

    private HostNameUtil() {
        //empty
    }

    /**
     * Determines if the given host is a localhost identifier.
     * @param host The host name (IP or "localhost" ID)
     * @return TRUE if the host is not null and points to localhost.
     */
    public static boolean isLocalhost(String host) {
        return ("127.0.0.1".equals(host) || "localhost".equals(host));
    }


}
