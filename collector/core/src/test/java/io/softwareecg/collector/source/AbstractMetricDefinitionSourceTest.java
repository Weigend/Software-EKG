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
package io.softwareecg.collector.source;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by johannes.weigend on 18.03.14.
 */
public class AbstractMetricDefinitionSourceTest {

    private static class MyCounterSource extends AbstractCounterSource {

        @Override
        public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {
            return 4711;
        }

        @Override
        public String getId() {
            return "myCounterSource";
        }


    }

    private boolean listenerWasCalled = false;

    private final CollectorJobConfiguration collectorConfig = TestConfigFactory.createTestConfig(
            new HostInfo("h1", "h1", "hg", null, null),
            new ProcessInfo("p", "p", null, null),
            new MetricGroupInfo("mock", "cg", 6000L, 6000L, null),
            new MetricDefinition[]{new MetricDefinition("c1", null)});

    @Test
    public void testQuerySingleCounter() {
        MyCounterSource source = new MyCounterSource();
        double value = source.querySingleCounter(collectorConfig, new MetricDefinition("cx", null));
        Assert.assertEquals(value, 4711d, 0.0);
    }

    @Test
    public void testQueryCounter() {
        CounterSource source = new MyCounterSource();
        CounterSeries series = source.queryCounter(collectorConfig);
        Assert.assertEquals(series.getValues().size(), 1);
        Assert.assertEquals(series.getValues().get(0), Double.valueOf(4711d));

    }

    @Test
    public void testAddCounterListener() {
        listenerWasCalled = false;
        MyCounterSource source = new MyCounterSource();
        source.addCounterListener(() -> listenerWasCalled = true);
        source.fireCounterChanged();
        Assert.assertTrue(listenerWasCalled);

    }

    @Test
    public void testRemoveCounterListener() {
        listenerWasCalled = false;
        MyCounterSource source = new MyCounterSource();
        CounterSourceListener listener = () -> listenerWasCalled = true;
        source.addCounterListener(listener);
        source.removeCounterListener(listener);
        source.fireCounterChanged();
        Assert.assertFalse(listenerWasCalled);
    }

    @Test
    public void testFireCounterChanged() {
        // already tested
    }
}
