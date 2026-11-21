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
//       description:   Baseclass to simplify concrete source implementations.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Base class for concrete counter sources helps to implements event delegation.
 * To implement bulk queries you have to override queryCounter().
 * To implement wildcard support for counter names you have to override replaceWildcardCounter().
 */
public abstract class AbstractCounterSource implements CounterSource {

    private static final Pattern LABEL_FILTERREF_SEARCH_PATTERN = Pattern.compile("(\\$\\{FILTER:([^}]+)})");

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The list of listeners.
     */
    protected List<CounterSourceListener> listeners = new ArrayList<>();

    /**
     * Default -> We request single counters.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @param metricDefinition the current metricDefinition (its one metricDefinition of the group)
     * @return a result.
     */
    public abstract double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition);

    /**
     * Default -> We request single values.
     *
     * @param descriptor holds host/counter/process/source.
     * @return the list of measurements (-1 for every case of a measurement error).
     */
    public CounterSeries queryCounter(CounterDescriptor descriptor) {
        List<Double> result = new ArrayList<>();
        for (MetricDefinition metricDefinition : descriptor.getMetricDefinitions()) {
            double value = -1.0;
            try {
                value = querySingleCounter(descriptor, metricDefinition);
            } catch (Exception e) {
                LOGGER.debug(e.toString(), e);  // normal
            }

            result.add(value);
        }
        return new CounterSeries(result);
    }

    /**
     * Checks the real world metric name (param 'metric') using the regular expression provided by the given
     * {@link MetricDefinition} instance. If it pass a new MetricDefinition will created that is based on the data
     * of the given MetricDefinition, the real world metric name that replaces the metric definition expression
     * and optional also the label the regex group value if expression language used in the label name
     * ((${FILTER:1-n} var expression)
     * In case of match and creation of new MetricDefinition instance it will added to the target list of
     * MetricDefinition.
     *
     * @param filteredMetricDefinitions the target MetricDefinition a matching metric will added to
     * @param metric the real world metric name that was resolved before using the MetricDefinition of the third param
     * @param orgMetricDefinition the original MetricDefinition that will be the base for the new created one
     */
    public static void postFilterAwareAddMetric(Collection<MetricDefinition> filteredMetricDefinitions, String metric,
                                         MetricDefinition orgMetricDefinition) {

        Matcher labelMatcher = LABEL_FILTERREF_SEARCH_PATTERN.matcher(orgMetricDefinition.getLabel());

        boolean useFilterLabelReplacement = labelMatcher.find();

        // check if label replacement is active?
        if (!useFilterLabelReplacement) {
            filteredMetricDefinitions.add(new MetricDefinition(metric, metric, null, false));
            return;
        }

        // label replacement is active but is also a filter expression given?
        if (orgMetricDefinition.getRegexPostFilterExp() == null) {
            filteredMetricDefinitions.add(new MetricDefinition(metric, MetricDefinition.FILTER_LABEL_UNRESOLVABLE,
                    null, false));
            return;
        }

        // match metric name with post filter expression
        Matcher postFilterMatcher = Pattern.compile(orgMetricDefinition.getRegexPostFilterExp()).matcher(metric);
        if (!postFilterMatcher.matches()) {
            // metric doesn't match the post filter
            return;
        }

        String metricLabel = orgMetricDefinition.getLabel();

        do {
            String filterSubLabel = labelMatcher.group();

            // metric name matches to post filter, let's see if the can resolve the group
            String groupIndexString = labelMatcher.group(2);
            if (!groupIndexString.matches("\\d+")) {
                throw new IllegalArgumentException("the variable '" + orgMetricDefinition.getLabel()
                        + "' doesn't specify a group number that is expected after ':'");
            }

            int groupIndex = Integer.valueOf(groupIndexString);

            // check if group exists we should take the value from
            if (groupIndex > postFilterMatcher.groupCount()) {

                // dammed! use default label instead.
                filteredMetricDefinitions.add(new MetricDefinition(metric, MetricDefinition.FILTER_LABEL_UNRESOLVABLE,
                        null, false));
                return;
            }

            metricLabel = StringUtils.replace(metricLabel, filterSubLabel, postFilterMatcher.group(groupIndex));

        } while (labelMatcher.find());

        // work is done label can replaced with group value from filter expression
        filteredMetricDefinitions.add(new MetricDefinition(metric, metricLabel));
    }

    /**
     * add a MetricDefinition listener for dynamic (generic) counters which dynamically expands to N counter.
     *
     * @param sourceListener the listener.
     */
    public void addCounterListener(CounterSourceListener sourceListener) {
        listeners.add(sourceListener);
    }

    /**
     * add a MetricDefinition listener for dynamic (generic) counters which dynamically expands to N counter.
     *
     * @param sourceListener the listener.
     */
    public void removeCounterListener(CounterSourceListener sourceListener) {
        listeners.remove(sourceListener);
    }

    /**
     * Fire counterChanged() to all listeners.
     */
    public void fireCounterChanged() {
        for (CounterSourceListener next : listeners) {
            next.counterChanged();
        }
    }
}