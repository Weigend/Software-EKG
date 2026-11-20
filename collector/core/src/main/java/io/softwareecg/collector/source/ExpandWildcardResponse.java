package io.softwareecg.collector.source;

import io.softwareecg.collector.dt.MetricDefinition;

/**
 * An response container that serves the resolved expanded concrete metrics
 * that matches to the wildcard definitions.
 *
 * In addition to that a the response also includes result type enum
 * that can use to get more details about the wildcard expand processing.
 *
 * @since 29.09.2018
 */
public class ExpandWildcardResponse {

    private final ExpandWildcardResultType resultType;

    private String message;

    private MetricDefinition[] expandedDefinitions = new MetricDefinition[0];

    /**
     * Standard constructor of this response class that initialized the
     * response with the concrete result type and the resolved metric definitions
     * of size 0-*.
     *
     * @param resultType the result type that serves more details about the processing of wildcard expansion
     * @param expandedDefinitions the resolved (expanded) metric definitions
     */
    public ExpandWildcardResponse(ExpandWildcardResultType resultType, MetricDefinition[] expandedDefinitions) {
        this.resultType = resultType;
        this.expandedDefinitions = expandedDefinitions;
    }

    /**
     * Shortcut Constructor used in success cases that will return an array
     * with 1-* metric definitions.
     *
     * @param expandedDefinitions the resolved (expanded) metric definitions
     */
    public ExpandWildcardResponse(MetricDefinition[] expandedDefinitions) {
        this.resultType = ExpandWildcardResultType.SUCCESSFUL_EXPANDED;
        this.expandedDefinitions = expandedDefinitions;
    }

    /**
     * Shortcut Constructor used in error cases that will return no MetricDefinition's
     * but details about the error using the concrete ExpandWildcardResultType
     *
     * @param resultType the ExpandWildcardResultType that serves details why no metric definitions could resolved
     */
    public ExpandWildcardResponse(ExpandWildcardResultType resultType) {
        this.resultType = resultType;
    }

    /**
     * An expansion that did not succeed, with the reason.
     *
     * @param resultType why there are no expanded definitions
     * @param message    what went wrong, written for the person who configured the source
     */
    public ExpandWildcardResponse(ExpandWildcardResultType resultType, String message) {
        this.resultType = resultType;
        this.message = message;
    }

    /**
     * @return what went wrong, or {@code null} if the source gave no reason
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the result type that serves more details
     * about the wildcard expansion.
     *
     * @return  the result type that serves more details about the processing of wildcard expansion
     */
    public ExpandWildcardResultType getResultType() {
        return resultType;
    }

    /**
     * Returns the expanded metric definitions that matches to the
     * wildcard definitions to expand.
     *
     * @return the resolved (expanded) metric definitions (maybe empty an empty array if no result available)
     */
    public MetricDefinition[] getExpandedDefinitions() {
        return expandedDefinitions;
    }
}
