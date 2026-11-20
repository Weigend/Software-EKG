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
package io.softwareecg.collector.source.unix.counters.top;

import io.softwareecg.collector.dt.MetricDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Helper class for getting the unix TOP dependent counters.
 */
public final class TopCounters {

    private TopCounters() {
        //empty
    }

    /**
     * Get the uptime counter for unix TOP.
     * @return MetricDefinition with uptime pattern.
     */
    public static MetricDefinition getUptimeCounter() {
        return new MetricDefinition("\\Uptime\\up", "\\Uptime\\up");
    }

    /**
     * Get the user counter for unix TOP.
     * @return MetricDefinition with users pattern.
     */
    public static MetricDefinition getUserCounter() {
        return new MetricDefinition("\\User\\user", "\\User\\user");
    }

    /**
     * Create and returns a collection of counters for the given MetricDefinition name.
     * The counter name will be expanded if wildcards used
     *
     * @param counterName the counter name (wildcard and/or a name from LoadCounterEnum)
     * @return a collection with counters
     */
    public static Collection<MetricDefinition> getLoadWildcardCounter(String counterName) {
        ArrayList<MetricDefinition> newMetricDefinition = new ArrayList<>();
        String counterDef = TopCounterGroupEnum.Load.getExpression();

        if (counterName.contains(".*")) {

            Pattern pattern = Pattern.compile(counterName);

            for (LoadCounterEnum lc : LoadCounterEnum.values()) {
                if (pattern.matcher(lc.name()).matches()) {
                    newMetricDefinition.add(new MetricDefinition(counterDef + lc.name(), counterDef + lc.name()));
                }
            }

        } else {
            newMetricDefinition.add(new MetricDefinition(counterDef + counterName, counterDef + counterName));
        }

        return newMetricDefinition;
    }

    /**
     * Create and returns a collection of counters for the given MetricDefinition name.
     * The counter name will be expanded if wildcards used
     *
     * @param counterName the counter name (wildcard and/or a name from TaskCounterEnum)
     * @return a collection with counters
     */
    public static Collection<MetricDefinition> getTaskWildcardCounter(String counterName) {
        ArrayList<MetricDefinition> newMetricDefinition = new ArrayList<>();
        String counterDef = TopCounterGroupEnum.Tasks.getExpression();

        if (counterName.contains(".*")) {

            Pattern pattern = Pattern.compile(counterName);

            for (TaskCounterEnum tc : TaskCounterEnum.values()) {
                if (pattern.matcher(tc.name()).matches()) {
                    newMetricDefinition.add(new MetricDefinition(counterDef + tc.name(), counterDef + tc.name()));
                }
            }

        } else {
            newMetricDefinition.add(new MetricDefinition(counterDef + counterName, counterDef + counterName));
        }

        return newMetricDefinition;
    }


    /**
     * Create and returns a collection of counters for the given MetricDefinition name.
     * The counter name will be expanded if wildcards used
     *
     * @param counterName the counter name (wildcard and/or a name from SwapCounterEnum)
     * @return a collection with counters
     */
    public static Collection<MetricDefinition> getSwapWildcardCounter(String counterName) {
        ArrayList<MetricDefinition> newMetricDefinition = new ArrayList<>();
        String counterDef = TopCounterGroupEnum.Swap.getExpression();

        if (counterName.contains(".*")) {

            Pattern pattern = Pattern.compile(counterName);

            for (SwapCounterEnum sc : SwapCounterEnum.values()) {
                if (pattern.matcher(sc.name()).matches()) {
                    newMetricDefinition.add(new MetricDefinition(counterDef + sc.name(), counterDef + sc.name()));
                }
            }

        } else {
            newMetricDefinition.add(new MetricDefinition(counterDef + counterName, counterDef + counterName));
        }

        return newMetricDefinition;
    }


    /**
     * Create and returns a collection of counters for the given MetricDefinition name.
     * The counter name will be expanded if wildcards used
     *
     * @param counterName the counter name (wildcard and/or a name from MemoryCounterEnum)
     * @return a collection with counters
     */
    public static Collection<MetricDefinition> getMemoryWildcardCounter(String counterName) {
        ArrayList<MetricDefinition> newMetricDefinition = new ArrayList<>();
        String counterDef = TopCounterGroupEnum.Memory.getExpression();

        if (counterName.contains(".*")) {

            Pattern pattern = Pattern.compile(counterName);

            for (MemoryCounterEnum mc : MemoryCounterEnum.values()) {
                if (pattern.matcher(mc.name()).matches()) {
                    newMetricDefinition.add(new MetricDefinition(counterDef + mc.name(), counterDef + mc.name()));
                }
            }

        } else {
            newMetricDefinition.add(new MetricDefinition(counterDef + counterName, counterDef + counterName));
        }

        return newMetricDefinition;
    }


    /**
     * Create and returns a collection of counters for the given MetricDefinition name.
     * The counter name will be expanded if wildcards used
     *
     * @param counterName the counter name (wildcard and/or a name from CPUCounters)
     * @return a collection with counters
     */
    public static Collection<MetricDefinition> getCpuWildcardCounter(String counterName) {
        ArrayList<MetricDefinition> newMetricDefinition = new ArrayList<>();
        String counterDef = TopCounterGroupEnum.Cpu.getExpression();

        if (counterName.contains(".*")) {

            Pattern pattern = Pattern.compile(counterName);

            for (CPUCounterEnum cc : CPUCounterEnum.values()) {
                if (pattern.matcher(cc.name()).matches()) {
                    newMetricDefinition.add(new MetricDefinition(counterDef + cc.name(), counterDef + cc.name()));
                }
            }

        } else {
            newMetricDefinition.add(new MetricDefinition(counterDef + counterName, counterDef + counterName));
        }

        return newMetricDefinition;
    }


}
