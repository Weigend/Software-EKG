package io.softwareecg.awb.sdk.core.events;

import org.apache.commons.lang3.StringUtils;

import java.util.EventObject;

/**
 * Global error event that can publish by each EKG component
 * to communicate the error to others and the UI itself.
 *
 * @since 03.06.2018
 */
public class AwbErrorEvent extends EventObject {

    private final Exception cause;

    private String customErrorMessage = null;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @param cause The exception that causes the error
     * @throws IllegalArgumentException if source is null.
     */
    public AwbErrorEvent(Object source, Exception cause) {
        super(source);
        this.cause = cause;
    }

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @param customErrorMessage error message that provides a better understandable error description for the AWB user
     * @param cause The exception that causes the error
     * @throws IllegalArgumentException if source is null.
     */
    public AwbErrorEvent(Object source, String customErrorMessage, Exception cause) {
        super(source);
        this.customErrorMessage = customErrorMessage;
        this.cause = cause;
    }

    /**
     * Returns the exception that causes the error in
     * the source component.
     *
     * @return exception that causes the error
     */
    public Exception getCause() {
        return cause;
    }

    /**
     * Returns the custom error message if
     * available. Otherwise null will returned.
     *
     * @return the error message or null
     */
    public String getCustomErrorMessage() {
        return customErrorMessage;
    }

    /**
     * Returns true if the event provides a custom
     * error message or false if not.
     *
     * @return true if an error message is available.
     */
    public boolean hasCustomErrorMessage() {
        return StringUtils.isNotBlank(customErrorMessage);
    }
}
