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
//       description:   Calculated and statistic performance counter.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source.calc;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.memory.CollectorMemory;
import io.softwareecg.collector.source.CounterWildcardSupport;
import io.softwareecg.collector.source.Delayed;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.AbstractCounterSource;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * This counter source calculates counters from other sources by using predefined functions
 * like avg, min, max, median, delta or value.
 * The counter is evaluated
 */
@Delayed
public class CalculatedPerformanceCounter extends AbstractCounterSource implements CounterWildcardSupport {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Skript functions.
     */
    private static final String SKRIPT_FUNCTIONS =
            "function value(counter) { return memory.value(counter); } " +
                    "function avg(counter) { return memory.avg(counter); } " +
                    "function sum(counter) { return memory.sum(counter); } " +
                    "function delta(counter) { return memory.delta(counter); }" +
                    "function median(counter) { return memory.median(counter); } " +
                    "function min(counter) { return memory.min(counter); } " +
                    "function max(counter) { return memory.max(counter); } ";

    /**
     * Javascript Backslash
     */
    public static final String JAVASCRIPT_BACKSLASH_ESCAPE = "\\\\\\\\";

    /**
     * Backslash Regex.
     */
    public static final String BACKSLASH_REGEX = "\\\\";

    /**
     * GraalVM Polyglot Context for JavaScript execution.
     */
    private Context polyglotContext;

    /**
     * The script engine is costly - it takes memory and time to start, and it loads native
     * code. It is created when the first calculated metric is asked for, so that a collector
     * that calculates nothing never starts it.
     *
     * @throws IllegalStateException if there is no script engine in this installation
     */
    private synchronized Context context() {
        if (polyglotContext == null) {
            try {
                polyglotContext = Context.newBuilder("js")
                        .allowHostAccess(HostAccess.ALL)
                        .allowHostClassLookup(className -> true)
                        .build();
            } catch (LinkageError e) {
                throw new IllegalStateException("Calculated metrics need the JavaScript engine GraalJS, "
                        + "which is not part of this installation.", e);
            }
        }
        return polyglotContext;
    }



    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.AbstractCounterSource#querySingleCounter(io.softwareecg.collector.dt.CounterDescriptor, io.softwareecg.collector.dt.MetricDefinition)
     */
    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {
        String script = metricDefinition.getDefinition();

        // javascript specific escape of backslashes
        script = script.replaceAll(BACKSLASH_REGEX, JAVASCRIPT_BACKSLASH_ESCAPE);

        try {
            // Bind the memory object to JavaScript context
            Context context = context();
            context.getBindings("js").putMember("memory", CollectorMemory.getMemory(descriptor.getNodeAddress()));
            Value result = context.eval("js", SKRIPT_FUNCTIONS + script);
            return result.asDouble();
        } catch (Exception e) {
            LOGGER.error("Problem during metricDefinition script execution", e);
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.CounterWildcardSupport#replaceWildcardCounter(io.softwareecg.collector.dt.CounterDescriptor)
     */
    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {

        CollectorMemory memory = CollectorMemory.getMemory(descriptor.getProcessMetricsSource());
        String[] allCounters = memory.getAllCounter(descriptor.getProcessMetricsSource());

        List<MetricDefinition> expandedMetricDefinition = getReplacedCounters(descriptor.getMetricDefinitions(), allCounters);
        return new ExpandWildcardResponse(expandedMetricDefinition.toArray(new MetricDefinition[0]));
    }

    /**
     * Helper to replace metricDefinitions.
     *
     * @param metricDefinitions        the original metricDefinitions.
     * @param allCounterNames all names in memory.
     * @return a list of replaced metricDefinitions.
     */
    private List<MetricDefinition> getReplacedCounters(MetricDefinition[] metricDefinitions, String[] allCounterNames) {
        List<MetricDefinition> expandedMetricDefinition = new ArrayList<>();
        for (MetricDefinition originalMetricDefinition : metricDefinitions) {

            if (!originalMetricDefinition.useWildcard()) {
                expandedMetricDefinition.add(originalMetricDefinition);
                continue;
            }

            String definition = originalMetricDefinition.getDefinition();
            int start = 0, end = 0;
            for (int j = 0; j < definition.length(); j++) {
                if (definition.charAt(j) == '\"') {
                    if (start > end) {
                        end = j;
                        break;
                    } else {
                        start = j;
                    }
                }
            }
            String cnt = definition.substring(start + 1, end);
            cnt = cnt.replaceAll("\\*", ".*");
            cnt = cnt.replaceAll(BACKSLASH_REGEX, JAVASCRIPT_BACKSLASH_ESCAPE);
            for (String counter2 : allCounterNames) {
                if (counter2.equals(cnt) || counter2.matches(cnt)) {
                    String newName = definition.substring(0, start + 1) + counter2 + definition.substring(end);
                    postFilterAwareAddMetric(expandedMetricDefinition, newName, originalMetricDefinition);
                }
            }
        }
        return expandedMetricDefinition;
    }

    @Override
    public String getId() {
        return "calc";
    }

}
