package io.softwareecg.collector.source;

/**
 * An enumeration that defines the different states that can occur than
 * a wildcard counter sources tries to retrieve the concrete metrics
 * for the configured wildcard expressions.
 *
 * @since 29.09.2018
 */
public enum ExpandWildcardResultType {

    /**
     * The metric source is available and query the wildcard expressions against
     * it results in 1-* concrete (expanded) metrics that match to the expressions.
     */
    SUCCESSFUL_EXPANDED,

    /**
     * The metric source (server, application, ...) isn't available as result of
     * connections problems or failures on the remote side. The expansion of
     * wildcard metrics wasn't be possible at the moment but maybe later.
     */
    METRIC_SOURCE_NOT_AVAILABLE,

    /**
     * The wildcard metric definition has an invalid syntax and couldn't expanded
     * to one or more concrete metrics.
     * The expansion of wildcard metrics isn't possible until the wildcard expression will corrected.
     */
    METRIC_DEFINITION_INVALID,

    /**
     * The wildcard metric definition seems to be valid and the metric source response
     * as expected but the queried metrics couldn't find / doesn't exists.
     * The expansion of wildcard metrics wasn't be possible at the moment but maybe later.
     */
    METRIC_NOT_FOUND,

    /**
     * The wildcard expansion results in an unspecified / not more detailed error.
     * This error state is used by counter sources that are not able to classify the reason
     * that caused the error or no one of the error states above matches to the concrete one.
     */
    ERROR
}
