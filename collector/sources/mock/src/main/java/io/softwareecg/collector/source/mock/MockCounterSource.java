//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend
//       creation date: 2011
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-03-04 17:28:30 +0100 (Do, 04 Mrz 2010) $
//       revision:      $Revision: 421 $
//       description:   Johannes Weigend
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source.mock;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterWildcardSupport;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.common.AbstractCounterSource;

import java.util.Map;


/**
 * Mock counter.
 */
public class MockCounterSource extends AbstractCounterSource implements CounterWildcardSupport {

    private final Map<String, Double> smoothingFactors = Map.of(
            "mock-metric-a", 1d,
            "mock-metric-b", 5d,
            "mock-metric-c", 30d
    );

    private long lastQuery = System.currentTimeMillis() ;

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.common.AbstractCounterSource#querySingleCounter(CounterDescriptor, MetricDefinition)
     */
    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {

        double factor = smoothingFactors.get(metricDefinition.getLabel());
        double booster = 1;

        long offset = Double.valueOf(Math.random() * 880_000d).longValue();

        if ((lastQuery + offset) < System.currentTimeMillis()) {
            booster = Math.random() * 6;
            lastQuery = System.currentTimeMillis();
        }

        double res = 0;

        for (int i = 0; i < (factor + booster); i++) {
            res += Math.random();
        }

        res /= (factor + booster);


        if ("mock-metric-c".equals(metricDefinition.getLabel())) {
            return res * 1000;
        }

        return res * 1000 * booster;
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.CounterWildcardSupport#replaceWildcardCounter(CounterDescriptor)
     */
    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {
        MetricDefinition[] metricDefinition = new MetricDefinition[]{
                new MetricDefinition("ca", "mock-metric-a"),
                new MetricDefinition("cb", "mock-metric-b"),
                new MetricDefinition("cc", "mock-metric-c", null, true)
        };

        return new ExpandWildcardResponse(metricDefinition);
    }

    @Override
    public String getId() {
        return "mock";
    }
}
