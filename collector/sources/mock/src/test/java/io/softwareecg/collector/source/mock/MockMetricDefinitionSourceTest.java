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
package io.softwareecg.collector.source.mock;

import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import io.softwareecg.collector.source.CounterSource;
import io.softwareecg.collector.source.CounterWildcardSupport;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by johannes.weigend on 18.03.14.
 */
public class MockMetricDefinitionSourceTest {

    private final CollectorJobConfiguration config = TestConfigFactory.createTestConfig(
            new HostInfo("h1", "h1", "hg", null, null),
            new ProcessInfo("p", "p", null, null),
            new MetricGroupInfo("cg", "mock", 0L, 0L, null ),
            new MetricDefinition[]{new MetricDefinition("c1", null)});

    @Test
    public void testQuerySingleCounter() throws Exception {
        CounterSource source = new MockCounterSource();
        source.queryCounter(config);
    }

    @Test
    public void testReplaceWildcardCounter() throws Exception {
        CounterSource source = new MockCounterSource();
        CounterWildcardSupport support = (CounterWildcardSupport) source;
        MetricDefinition[] replacement = support.replaceWildcardCounter(config).getExpandedDefinitions();
        Assert.assertNotSame(support, replacement);
    }

    @Test
    public void testGetId() throws Exception {
        CounterSource source = new MockCounterSource();
        Assert.assertEquals(source.getId(), "mock");

    }
}
