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
package io.softwareecg.collector.source.calc;

import io.softwareecg.collector.config.CollectorJobConfiguration;
import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import io.softwareecg.collector.memory.CollectorMemory;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests the calculated performance counters. The values are calculated from real counters in the Collector Memory.
 * Created by johannes.weigend on 18.03.14.
 */
public class CalculatedPerformanceMetricDefinitionTest {

    private static final String NODE_ADDRESS = "//hostAddress";

    private final CollectorJobConfiguration collectorConfig = TestConfigFactory.createTestConfig(
            new HostInfo("h1", NODE_ADDRESS, "hg", null, null),
            new ProcessInfo("p", "processLabel", null, null),
            new MetricGroupInfo( "cg", "mock", 6000L, 6000L, null),
            new MetricDefinition[]{
                    new MetricDefinition("c1", null),
                    new MetricDefinition("avg('c1') + avg('c1')", null),
                    new MetricDefinition("min('c1') + min('c1')", null),
                    new MetricDefinition("max('c1') + max('c1')", null),
                    new MetricDefinition("value('c1') + value('c1')", null),
                    new MetricDefinition("max(\"c*\")", null, null, true)
            });

    @Test
    public void testQuerySingleCounter() {
        MetricDefinition[] metricDefArray = collectorConfig.getMetricDefinitions();
        CollectorMemory.getMemory(NODE_ADDRESS).recordCounter(new MetricDefinition[]{metricDefArray[0]}, List.of(2d));
        CollectorMemory.getMemory(NODE_ADDRESS).recordCounter(new MetricDefinition[]{metricDefArray[0]}, List.of(1d));
        CollectorMemory.getMemory(NODE_ADDRESS).recordCounter(new MetricDefinition[]{metricDefArray[0]}, List.of(2d));
        CollectorMemory.getMemory(NODE_ADDRESS).recordCounter(new MetricDefinition[]{metricDefArray[0]}, List.of(3d));
        CollectorMemory.getMemory(NODE_ADDRESS).recordCounter(new MetricDefinition[]{metricDefArray[0]}, List.of(2d));

        CalculatedPerformanceCounter source = new CalculatedPerformanceCounter();
        double value = source.querySingleCounter(collectorConfig, metricDefArray[1]);
        Assert.assertEquals(value, 2d + 2d, 0.0);
        value = source.querySingleCounter(collectorConfig, metricDefArray[2]);
        Assert.assertEquals(value, 1d + 1d, 0.0);
        value = source.querySingleCounter(collectorConfig, metricDefArray[3]);
        Assert.assertEquals(value, 3d + 3d, 0.0);
        value = source.querySingleCounter(collectorConfig, metricDefArray[4]);
        Assert.assertEquals(value, 2d + 2d, 0.0);
    }

    @Test
    public void testReplaceWildcardCounter() {
        CalculatedPerformanceCounter source = new CalculatedPerformanceCounter();
        MetricDefinition[] replacement = source.replaceWildcardCounter(collectorConfig).getExpandedDefinitions();
        Assert.assertNotSame(replacement, collectorConfig.getMetricDefinitions());
    }

    @Test
    public void testGetId() {
        CalculatedPerformanceCounter source = new CalculatedPerformanceCounter();
        Assert.assertEquals(source.getId(), "calc");
    }

}
