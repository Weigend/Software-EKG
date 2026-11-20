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
package io.softwareecg.collector.source.unix.counters.iostat;

import io.softwareecg.collector.dt.MetricDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Created by martin.binder on 29.04.14.
 */
public final class IOStatCounters {

    private IOStatCounters() {
        //empty
    }

    /**
     * Find all matching device counters.
     * @param deviceExpression device name.
     * @param counterName counter name.
     * @param devIndexSet collection with all supported names.
     * @return found counters.
     */
    public static Collection<MetricDefinition> getDeviceWildcardCounter(String deviceExpression, String counterName, Collection<String> devIndexSet) {
        return findMatchingCounter(devIndexSet,
                IOStatCounterEnum.Device.getExpression(deviceExpression),
                counterName);
    }

    /**
     * Find all matching fs counters.
     * @param filesystemExpression fs name
     * @param counterName counter name.
     * @param fsIndexSet collection with all supported names.
     * @return found counters.
     */
    public static Collection<MetricDefinition> getFilesystemWildcardCounter(String filesystemExpression, String counterName, Collection<String> fsIndexSet) {
        return findMatchingCounter(fsIndexSet,
                IOStatCounterEnum.Filesystem.getExpression(filesystemExpression),
                counterName);
    }

    private static Collection<MetricDefinition> findMatchingCounter(Collection<String> supportedCounterNames,
                                                                    String counterDef, String lookupCounterName) {
        ArrayList<MetricDefinition> newMetricDefinition = new ArrayList<>();

        if (lookupCounterName.contains(".*")) {
            Pattern pattern = Pattern.compile(lookupCounterName);

            for (String counterName : supportedCounterNames) {
                if (pattern.matcher(counterName).matches()) {
                    newMetricDefinition.add(new MetricDefinition(counterDef + counterName, counterDef + counterName, null, Boolean.TRUE));
                }
            }

        } else {
            newMetricDefinition.add(new MetricDefinition(counterDef + lookupCounterName, counterDef + lookupCounterName));
        }

        return newMetricDefinition;
    }


}
