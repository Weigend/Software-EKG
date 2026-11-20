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
//       description:   Windows performance counter JNI wrapper.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________

package io.softwareecg.collector.source.wincount;

import io.softwareecg.collector.common.LoggerFactory;
import org.slf4j.Logger;


/**
 * Windows performance counter JNI wrapper.
 *
 */
@SuppressWarnings("PMD")
public class WindowsPerformanceCounterAccess {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Name of the windows 32-bit performance counter access dll.
     */
    private static final String WPACCESS_32_DLL = "wpcaccess-5.1.0-x32";

    /**
     * Name of the windows 64-bit performance counter access dll.
     */
    private static final String WPACCESS_64_DLL = "wpcaccess-5.1.0-x64";

    /**
     * Native Library.
     */
    private static final String NATIVE_LIBRARY = "Native library ";

    /**
     * Loaded.
     */
    private static final String LOADED = " loaded.";


    /**
     * Loading.
     */
    protected WindowsPerformanceCounterAccess() {
        try {
            // check if we have a 64 bit JVM here, so we load the 64 bit dll
            String jvmBitSize = System.getProperty("sun.arch.data.model");
            if (jvmBitSize == null) {
                throw new IllegalStateException("Can not read thread size.");
            }
            if ("64".equals(jvmBitSize)) {
                System.loadLibrary(WPACCESS_64_DLL);
                LOGGER.info(NATIVE_LIBRARY + WPACCESS_64_DLL + " loaded.");

            } else {
                System.loadLibrary(WPACCESS_32_DLL);
                LOGGER.info(NATIVE_LIBRARY + WPACCESS_32_DLL + LOADED);
            }
            init();
        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn(NATIVE_LIBRARY + WPACCESS_32_DLL + "/" + WPACCESS_64_DLL + " can not found.");
        }
    }


    /**
     * Inits the dll.
     */
    private static native void init();

    /**
     * Request a single counter for a given host.
     *
     * @param counter the counter.
     * @return the counter.
     */
    public native double getCounter(String counter);

    /**
     * Autenticate and impersonate this thread with the given user/pass.
     *
     * @param domain   the authentication domain.
     * @param userName the user.
     * @param password the pwd.
     * @return true if the user can be successful authenticated.
     */
    public native boolean authenticate(String domain, String userName, String password);


    /**
     * Resolves wildcard counters by using the performance helper dll.
     *
     * @param counterName the name.
     * @return a String[] which contains all counters for a wildcard.
     */
    public native String[] resolveWildcards(String counterName);


    /**
     * Request counters.
     *
     * @param lastQueryHandle the handle of the last call or 0.
     * @param counterNames    the counters (without wildcards)
     * @param result          the array of doubles as [out] parameter.
     * @return the handle for the next call for query reuse.
     */
    public native long queryCounter(long lastQueryHandle, String[] counterNames, double[] result);


    /**
     * Cleanup. This is currently not used.
     *
     * @param lastQueryHandle the last query handle.
     */
    public native void deleteQuery(long lastQueryHandle);
}
