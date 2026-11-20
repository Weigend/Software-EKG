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
package io.softwareecg.collector.memory;

import io.softwareecg.collector.dt.MetricDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Memory which knows all current counters.
 */
public class CollectorMemory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorMemory.class);

    /**
     * The Constant host2memory.
     */
    private static final Map<String, CollectorMemory> host2memory = Collections.synchronizedMap(new HashMap<>());

    // calculated values.
    /**
     * The counter2value.
     */
    private final Map<String, Double> counter2value = Collections.synchronizedMap(new TreeMap<>());

    /**
     * The counter2sum.
     */
    private final Map<String, Double> counter2sum = Collections.synchronizedMap(new TreeMap<>());

    /**
     * The counter2count.
     */
    private final Map<String, Long> counter2count = Collections.synchronizedMap(new TreeMap<>());

    /**
     * The counter2deltavalue.
     */
    private final Map<String, Double> counter2deltavalue = Collections.synchronizedMap(new TreeMap<>());

    /**
     * The counter2min.
     */
    private final Map<String, Double> counter2min = Collections.synchronizedMap(new TreeMap<>());

    /**
     * The counter2max.
     */
    private final Map<String, Double> counter2max = Collections.synchronizedMap(new TreeMap<>());

    /**
     * Gets the memory.
     *
     * @param host the host
     * @return the memory
     */
    public static synchronized CollectorMemory getMemory(String host) {
        CollectorMemory memory = host2memory.get(host);
        if (memory == null) {
            memory = new CollectorMemory();
            host2memory.put(host, memory);
        }
        return memory;
    }

    /**
     * Record metricDefinition.
     *
     * @param metricDefinition the metricDefinition
     * @param values  the values
     */
    public synchronized void recordCounter(MetricDefinition[] metricDefinition, List<Double> values) {
        if (metricDefinition.length != values.size()) {
            LOGGER.error("CollectorMemory(for aggregations): The amount of metric definitions isn't equal " +
                    "to fetched value. Skip this set of counter.");
            return;
        }

        for (int i = 0; i < metricDefinition.length; i++) {
            MetricDefinition c = metricDefinition[i];
            String counterString = c.getLabel();

            // save old value for delta calculation
            counter2deltavalue.put(counterString, counter2value.get(counterString));

            counter2value.put(counterString, values.get(i));

            if (counter2sum.containsKey(counterString)) {
                counter2sum.put(counterString, counter2sum.get(counterString) + value(counterString));
            } else {
                counter2sum.put(counterString, values.get(i));
            }
            if (counter2count.containsKey(counterString)) {
                counter2count.put(counterString, counter2count.get(counterString) + 1);
            } else {
                counter2count.put(counterString, 1L);
            }
            if (counter2min.containsKey(counterString)) {
                counter2min.put(counterString, Math.min(counter2min.get(counterString), value(counterString)));
            } else {
                counter2min.put(counterString, values.get(i));
            }
            if (counter2max.containsKey(counterString)) {
                counter2max.put(counterString, Math.max(counter2max.get(counterString), value(counterString)));
            } else {
                counter2max.put(counterString, value(counterString));
            }
        }
    }

    /**
     * Avg.
     *
     * @param counter the counter
     * @return the double
     */
    public double avg(String counter) {
        if (!counter2count.containsKey(counter)) {
            return 0;
        }
        return sum(counter) / counter2count.get(counter);
    }

    /**
     * Sum.
     *
     * @param counter the counter
     * @return the double
     */
    public double sum(String counter) {
        Double result = counter2sum.get(counter);
        return (result != null) ? result : 0;
    }

    /**
     * Value.
     *
     * @param counter the counter
     * @return the double
     */
    public double value(String counter) {
        Double result = counter2value.get(counter);
        return (result != null) ? result : 0;
    }

    /**
     * Min.
     *
     * @param counter the counter
     * @return the double
     */
    public double min(String counter) {
        Double result = counter2min.get(counter);
        return (result != null) ? result : 0;
    }

    /**
     * Max.
     *
     * @param counter the counter
     * @return the double
     */
    public double max(String counter) {
        Double result = counter2max.get(counter);
        return (result != null) ? result : 0;
    }

    /**
     * Median.
     *
     * @param counter the counter
     * @return the double
     */
    public double median(String counter) {
        return value(counter);
    }

    /**
     * Delta.
     *
     * @param counter the counter
     * @return the double
     */
    public double delta(String counter) {
        Double c1 = counter2value.get(counter);
        Double c2 = counter2deltavalue.get(counter);

        return (c1 != null && c2 != null) ? c1 - c2 : 0;
    }

    /**
     * Gets the all counter.
     *
     * @param hostIp the host ip
     * @return the all counter
     */
    public String[] getAllCounter(String hostIp) {
        return counter2count.keySet().toArray(new String[counter2count.size()]);
    }


    /**
     * Clear method for unit testing.
     */
    protected void clear() {
        counter2count.clear();
        counter2deltavalue.clear();
        counter2max.clear();
        counter2sum.clear();
        counter2min.clear();
        counter2value.clear();
    }
}
