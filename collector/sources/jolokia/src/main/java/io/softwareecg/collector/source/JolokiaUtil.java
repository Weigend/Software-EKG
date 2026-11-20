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
package io.softwareecg.collector.source;

import org.jolokia.client.request.J4pQueryParameter;
import org.jolokia.client.request.J4pReadResponse;

import java.util.HashMap;
import java.util.Map;

/**
 */
public final class JolokiaUtil {

    /**
     * The default options.
     */
    private static Map<J4pQueryParameter, String> defaultOptions = null;

    /**
     * Should not be instantiated.
     */
    private JolokiaUtil() {
    }

    /**
     * Handle the response of an request which should result in a scalar.
     *
     * @param response The jolokia response object.
     * @return The value in the response or -1 in case of the answer is not a scalar.
     */
    public static double handleResponseValue(J4pReadResponse response) {

        Object value = response.getValue();
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return -1;
        }
    }

    /**
     * Get a list with the default options for this connector.
     *
     * @return A list with the default options.
     */
    public static synchronized Map<J4pQueryParameter, String> getDefaultOptions() {

        if (defaultOptions == null) {
            defaultOptions = new HashMap<>();
            defaultOptions.put(J4pQueryParameter.IGNORE_ERRORS, "true");
        }
        return defaultOptions;
    }
}
