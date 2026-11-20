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
package io.softwareecg.collector.dt;


import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * A configuration bean that represents the definition of one or more metrics
 * that match to the definition expression.
 *
 * @since 09.08.2018
 */
public class MetricDefinition {

    public static final String FILTER_LABEL_UNRESOLVABLE = "GROUP_VALUE_UNRESOLVABLE";

    /**
     * the metric metricDefinitionExpr, this means the name or the
     * matching expression of the metric.
     */
    private String metricDefinitionExpr;

    /**
     * The user configured label/name of the counter
     * that will overwrite the real world name of the metric
     */
    private String metricLabel;

    /**
     * An optional attribute that can used to specify an regular expression that can
     * used to filter the metric names and use a group value as label
     */
    private String regexPostFilter;

    /**
     * boolean flag that indicates if wildcard
     * expansion should used or not
     */
    private final boolean wildcard;


    //================================================================================================================
    //  Multiple constructors for copy and other purposes
    //================================================================================================================

    /**
     * Constructs a new instance of MetricDefinition
     *
     * @param metricDefinitionExpr an expression that will used to find 1-n matching metrics in the counter source
     * @param metricLabel  the label of the metric that will shown instead the real world metric name in the AWB
     * @param regexPostFilter regular expression that can used to filter the metric names and use a group value as label
     * @param wildcard boolean flag if wildcard expansion should used or not
     */
    public MetricDefinition(String metricDefinitionExpr, String metricLabel, String regexPostFilter, Boolean wildcard) {
        this.metricDefinitionExpr = metricDefinitionExpr;
        this.metricLabel = metricLabel;
        this.regexPostFilter = regexPostFilter;
        this.wildcard = wildcard != null ? wildcard : false;
    }

    /**
     * Non wildcard constructor.
     *
     * @param definition the metricDefinitionExpr.
     * @param label      the label.
     */
    public MetricDefinition(String definition, String label) {
        this(definition, label, null, false);
    }

    /**
     * Copy constructor
     *
     * @param metricDefinition the metricDefinition to be copied
     */
    @SuppressWarnings("CopyConstructorMissesField") // false positive
    public MetricDefinition(MetricDefinition metricDefinition) {
        this(metricDefinition.getDefinition(), metricDefinition.getLabel(),
                metricDefinition.getRegexPostFilterExp(), metricDefinition.useWildcard());
    }


    //================================================================================================================
    //  Accessor API of the MetricDefinition bean
    //================================================================================================================

    /**
     * Gets the label.
     *
     * @return the counter label. if no counter label is set in configuration, the counter metricDefinitionExpr is returned.
     */
    public String getLabel() {
        return StringUtils.isNoneBlank(metricLabel) ? metricLabel : metricDefinitionExpr;
    }

    public void setMetricLabel(String label) {
        metricLabel = label;
    }

    public String getRegexPostFilterExp() {
        return regexPostFilter;
    }

    public void setRegexPostFilter(String regexPostFilter) {
        this.regexPostFilter = regexPostFilter;
    }

    /**
     * Gets the metricDefinitionExpr.
     *
     * @return counter metricDefinitionExpr
     */
    public String getDefinition() {
        return metricDefinitionExpr;
    }

    /**
     * Sets the metricDefinitionExpr.
     * @param definition a metricDefinitionExpr.
     */
    public void setDefinition(String definition){
        this.metricDefinitionExpr = definition;
    }

    /**
     * Gets the wildcard.
     *
     * @return the wildcard
     */
    public boolean useWildcard() {
        return wildcard;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString(
     */
    @Override
    public String toString() {
        return " MetricDefinition{exp='" + metricDefinitionExpr + '}';
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetricDefinition metricDefinition = (MetricDefinition) o;
        return !(!Objects.equals(metricDefinitionExpr, metricDefinition.metricDefinitionExpr));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return metricDefinitionExpr != null ? metricDefinitionExpr.hashCode() : 0;
    }


}
