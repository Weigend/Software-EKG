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
package io.softwareecg.collector.source.ekgjolokia;

import io.softwareecg.collector.config.CollectorJobConfiguration;
import io.softwareecg.collector.common.TestSupport;
import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * To run this test in IntelliJ, please set the javaagent VM parameter in the startup properties.
 * -javaagent:S:/codebase/code/v51/collector/collector-delivery/supplementary/collector-directory/lib/jolokia-jvm-1.6.0-agent.jar=port=8778,host=localhost
 *
 * Created by johannes.weigend on 18.04.14.
 */
public class EkgJolokiaPerformanceMetricDefinitionAccessTest {

    private final EkgJolokiaPerformanceCounterAccess source = new EkgJolokiaPerformanceCounterAccess();

    private final CollectorJobConfiguration testConfig = TestConfigFactory.createTestConfig(
            new HostInfo(
                    "localhost",
                    "127.0.0.1",
                    "HostGroup-1",
                    null,
                    null
            ),
            new ProcessInfo(
                    "collector",
                    "collector",
                    "http://localhost:8080/ekgjolokia/jolokia",
                    null
            ),
            new MetricGroupInfo(
                    "countergroup",
                    source.getId(),
                    0L,
                    0L,
                    null),
            new MetricDefinition[]{
                    // Single jvm counter - no wildcards
                    new MetricDefinition("MXBean(java.lang:type=Threading).CurrentThreadCpuTimeSupported", null),
            });


    @Test
    public void testConstruction() {
        TestSupport.testPrivateConstructor(EkgJolokiaPerformanceCounterAccess.class);
    }

    @Test
    public void testQuerySingleCounter() throws Exception {
        double threadCount = source.querySingleCounter(testConfig, testConfig.getMetricDefinitions()[0]);
        Assert.assertTrue(threadCount > -2);
    }


//    @Test
//    public void testQuerySingleCounter() throws Exception {
//        double threadCount = source.querySingleCounter(testConfig, testConfig.getCounters()[0]);
//        Assert.assertTrue(threadCount > 0);
//    }
//
//    @Test
//    public void testQueryCounter() throws Exception {
//        CounterSeries series = source.queryCounter(testConfig);
//        Assert.assertTrue(series.getValues().size() > 0);
//        Assert.assertTrue(series.getValues().get(0) > 0);
//    }
//
//    @Test
//    public void testReplaceWildcardCounter() throws Exception {
//        Assert.assertEquals(wildcardGroup.getCounters().length, 1);
//        MetricGroup replacedGroup = source.replaceWildcardCounter(wildcardGroup);
//        Assert.assertTrue(replacedGroup.getCounters().length > 1);
//        CounterSeries series = source.queryCounter(replacedGroup);
//        Assert.assertTrue(series.getValues().size() > 0);
//    }

    @Test
    public void testGetId() throws Exception {
        Assert.assertEquals(source.getId(), "ekgjolokia");
    }

}
