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
package io.softwareecg.collector.source.jmx;

import io.softwareecg.collector.config.CollectorJobConfiguration;
import io.softwareecg.collector.common.TestSupport;
import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import io.softwareecg.collector.source.CounterSeries;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for jmx counter. Uses the "self" Connection String which is a JMX connection to ourself.
 * Created by johannes.weigend on 02.04.14.
 */
public class JmxPerformanceMetricDefinitionSourceTest {

    private final JmxPerformanceCounterSource source = new JmxPerformanceCounterSource();

    private final CollectorJobConfiguration metricGroup = TestConfigFactory.createTestConfig(
            new HostInfo(
                    "localhost",
                    "127.0.0.1",
                    "HostGroup-1",
                    "user not used",
                    "pwd not used"
            ),
            new ProcessInfo(
                    "collector",
                    "collector",
                    "self",
                    null
            ),
            new MetricGroupInfo(
                    "countergroup",
                    source.getId(),
                    0L,
                    0L,
                    null),
            new MetricDefinition[]{
                    new MetricDefinition("MXBean(java.lang:type=Threading).ThreadCount", null, null, false),
                    new MetricDefinition("MXBean(java.lang:type=Threading).DaemonThreadCount", null, null, false),
            }
    );

    private final CollectorJobConfiguration wildcardGroup = TestConfigFactory.createTestConfig(
            new HostInfo(
                    "localhost",
                    "127.0.0.1",
                    "HostGroup-1",
                    "user not used",
                    "pwd not used"
            ),
            new ProcessInfo(
                    "collector",
                    "collector",
                    "self",
                    null
            ),
            new MetricGroupInfo(
                    "countergroup",
                    source.getId(),
                    0L,
                    0L,
                    null),
            new MetricDefinition[]{
                    new MetricDefinition(".*Memory.*", null, null, true /* wildcard */),
                    new MetricDefinition(".*GC.*", null, null, true /* wildcard */)
            }
    );

    @Test
    public void testConstruction() {
        TestSupport.testPrivateConstructor(JmxPerformanceCounterSource.class);
        TestSupport.testPrivateConstructor(JmxServiceUrlHelper.class);
    }

    @Test
    public void testQuerySingleCounter() {
        double threadCount = source.querySingleCounter(metricGroup, metricGroup.getMetricDefinitions()[0]);
        Assert.assertTrue(threadCount > 0);
    }

    @Test
    public void testQueryCounter() {
        CounterSeries series = source.queryCounter(metricGroup);
        Assert.assertTrue(series.getValues().size() > 0);
        Assert.assertTrue(series.getValues().get(0) > 0);
    }

    @Test
    public void testReplaceWildcardCounter() {
        Assert.assertEquals(wildcardGroup.getMetricDefinitions().length, 2);

        MetricDefinition[] replacedMetrics = source.replaceWildcardCounter(wildcardGroup).getExpandedDefinitions();
        Assert.assertTrue(replacedMetrics.length > 2);

        CollectorJobConfiguration copiedConfig = wildcardGroup.copyWithMetrics(replacedMetrics);
        CounterSeries series = source.queryCounter(copiedConfig);
        Assert.assertTrue(series.getValues().size() > 0);
    }

    @Test
    public void testGetId() {
        Assert.assertEquals(source.getId(), "jmx");
    }

    @Test
    public void testErrors() {

        source.queryCounter( TestConfigFactory.createTestConfig(
                new HostInfo(
                        "localhost",
                        "127.0.0.1",
                        "HostGroup-1",
                        "user not used",
                        "pwd not used"
                ),
                new ProcessInfo(
                        "collector",
                        "collector",
                        null,
                        null
                ),
                new MetricGroupInfo(
                        "countergroup",
                        source.getId(),
                        0L,
                        0L,
                        null),

                new MetricDefinition[]{
                        new MetricDefinition(".*Memory.*", null, null, true /* wildcard */)
                }
        ));
    }
}
