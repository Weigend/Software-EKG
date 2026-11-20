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
package io.softwareecg.collector.source.wincount;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterSeries;
import io.softwareecg.collector.source.CounterWildcardSupport;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.ExpandWildcardResultType;
import io.softwareecg.collector.source.common.AbstractCounterSource;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Windows performance counter source.
 * Created by johannes.weigend on 31.03.14.
 */
public class WindowsPerformanceCounterSource extends AbstractCounterSource implements CounterWildcardSupport {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Last query returned this id. We use one query for access.
     */
    private long lastQueryHandle = 0;

    /**
     * if false we disable the bulk query and use single access.
     */
    private static final boolean USE_BULK_QUERY = CollectorProperties.isBulkQuery();

    /**
     * The native methods are encapsulated here.
     */
    private final WindowsPerformanceCounterAccess nativeIf = new WindowsPerformanceCounterAccess();

    /**
     * Bulk query.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return the list of measurements (-1 for every case of a measurement error).
     */
    public CounterSeries queryCounter(CounterDescriptor descriptor) {

        // disable bulk query to avoid timeout issues if the target system is under heavy load
        if (!USE_BULK_QUERY) {
            return super.queryCounter(descriptor);
        }

        // bulk query
        String[] counterNames = new String[descriptor.getMetricDefinitions().length];
        MetricDefinition[] metricDefinitions = descriptor.getMetricDefinitions();
        for (int i = 0; i < metricDefinitions.length; i++) {
            MetricDefinition metricDefinition = metricDefinitions[i];
            counterNames[i] = metricDefinition.getDefinition();
        }
        double[] result = new double[counterNames.length];

        // the JNI call
        lastQueryHandle = nativeIf.queryCounter(lastQueryHandle, counterNames, result);
        List<Double> res = new ArrayList<>();
        for (double v : result) {
            res.add(v);
        }
        return new CounterSeries(res);
    }

    @Override
    public String getId() {
        return "win";
    }

    /**
     * Only for test purpose. We override queryCounter.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @param metricDefinition the current metricDefinition (its one metricDefinition of the group)
     * @return a single value.
     */
    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {
        return nativeIf.getCounter(metricDefinition.getDefinition());
    }

    /**
     * Expand all wildcard counter definitions in a counter group.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return the expandes group.
     */
    @Override
    public synchronized ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {

        MetricDefinition[] metricDefinitions = descriptor.getMetricDefinitions();
        Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();
        for (MetricDefinition c : metricDefinitions) {
            String wincounter = "\\\\" + getHostnameForWin7(descriptor.getNodeAddress()) + c.getDefinition();
            if (!c.useWildcard()) {
                expandedMetricDefinition.add(new MetricDefinition(wincounter, c.getLabel()));
                continue;
            }

            String[] newNames;
            try {
                newNames = nativeIf.resolveWildcards(wincounter);
                if (newNames == null) {
                    throw new IllegalStateException("Resolve wildcard returned null.");
                }
            } catch (RuntimeException e) {
                LOGGER.error("Can not expand windows wildcard path: " + wincounter, e);
                newNames = new String[]{wincounter};
            }

            for (String newName : newNames) {
                if (newName.equals(wincounter)) {
                    postFilterAwareAddMetric(expandedMetricDefinition, newName, c);
                } else {
                    try {
                        // new counter with same label and definition
                        String[] split = newName.split("\\\\");
                        String label = newName.substring(split[2].length() + 2);
                        expandedMetricDefinition.add(new MetricDefinition(newName, label, null, c.useWildcard()));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        LOGGER.error("can not split counter " + newName, e);
                    }
                }
            }
        }

        if (expandedMetricDefinition.isEmpty()) {
            return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_NOT_FOUND);
        }

        // The new group with a expanded counter list
        return new ExpandWildcardResponse(expandedMetricDefinition.toArray(new MetricDefinition[0]));
    }

    /**
     * Helper for Win7 support. Perfcounter on windows7 can only accessed if the prefix is the hostname in the domain.
     *
     * @param hostIp the ip.
     * @return the localhost name if address is 127.0.0.1 or localhost.
     */
    private String getHostnameForWin7(String hostIp) {
        if ("127.0.0.1".equals(hostIp) || "localhost".equals(hostIp)) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOGGER.error("Can not get name of localhost.", e);
            }
        }
        return hostIp;
    }
}
