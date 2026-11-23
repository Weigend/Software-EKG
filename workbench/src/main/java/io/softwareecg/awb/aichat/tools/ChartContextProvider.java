package io.softwareecg.awb.aichat.tools;

/**
 * Provides the current chart UI context (selected filters, time range, etc.)
 * as a JSON string. Implemented in the UI layer where chart state is accessible.
 */
@FunctionalInterface
public interface ChartContextProvider {

    /**
     * @return JSON string with the current chart context, or null if no chart is active
     */
    String getCurrentContextJson();
}
