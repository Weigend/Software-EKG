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
package io.softwareecg.collector.config;


/**
 * Invalid configuration.
 */
public class InvalidConfigurationException extends IllegalStateException {

    /**
     * User readable message.
     */
    public static final String INVALID_COLLECTOR_CONFIGURATION = "Invalid collector configuration: ";

    /**
     * Instantiates a new invalid configuration exception.
     *
     * @param message   the message
     * @param exception the preserved exception
     */
    public InvalidConfigurationException(String message, Exception exception) {
        super(INVALID_COLLECTOR_CONFIGURATION + message, exception);
    }

    /**
     * Instantiates a new invalid configuration exception.
     *
     * @param message the message
     */
    public InvalidConfigurationException(String message) {
        super(INVALID_COLLECTOR_CONFIGURATION + message);
    }

    /**
     * @return what is wrong, without the words that say that the configuration is invalid
     */
    public String getDetail() {
        String message = getMessage();
        return message != null && message.startsWith(INVALID_COLLECTOR_CONFIGURATION)
                ? message.substring(INVALID_COLLECTOR_CONFIGURATION.length()) : message;
    }
}
