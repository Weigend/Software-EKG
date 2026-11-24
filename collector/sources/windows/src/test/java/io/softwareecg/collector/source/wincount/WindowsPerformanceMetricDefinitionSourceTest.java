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

import io.softwareecg.collector.config.CollectorJobConfiguration;
import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import io.softwareecg.collector.source.CounterSeries;
import io.softwareecg.collector.source.CounterSource;
import io.softwareecg.collector.source.CounterWildcardSupport;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the wpc counter source.
 * Created by johannes.weigend on 19.03.14.
 */
public class WindowsPerformanceMetricDefinitionSourceTest {

    private final CollectorJobConfiguration jobConfig = TestConfigFactory.createTestConfig(
            new HostInfo(
                    "localhost",
                    "127.0.0.1",
                    "HostGroup-1",
                    "user not used",
                    "pwd not used"
            ),
            new ProcessInfo(
                    "java",
                    "java",
                    "not used",
                    null
            ),
            new MetricGroupInfo(
                    "countergroup",
                    "win",
                    0L,
                    0L,
                    "23:00"),
            new MetricDefinition[]{
                    // we use german counter and englisch counter. It is normal that one of them fails.
                    new MetricDefinition("\\Prozess(%1)\\*", null, null, true),
                    // the english counter
                    new MetricDefinition("\\Process(%1)\\Private Bytes", null),
            }
    );

    @Test
    public void testGetCounter() throws Exception {

        WindowsCounters.assumeAvailable();

        CounterSource source = new WindowsPerformanceCounterSource();
        CounterWildcardSupport support = (CounterWildcardSupport) source;

        MetricDefinition[] replacedMetrics = support.replaceWildcardCounter(jobConfig).getExpandedDefinitions();
        CounterSeries series = source.queryCounter(jobConfig.copyWithMetrics(replacedMetrics));
        assertNotNull(series);
        assertTrue(series.getValues().size() > 0);
    }



    @Test
    public void testAuthenticate() throws Exception {
        WindowsCounters.assumeAvailable();

        WindowsPerformanceCounterAccess source = new WindowsPerformanceCounterAccess();
        try {
            Assert.assertFalse(source.authenticate("localhost", "", ""));
            Assert.fail();
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testResolveWildcards() {

        WindowsCounters.assumeAvailable();

        CounterSource source = new WindowsPerformanceCounterSource();
        CounterWildcardSupport support = (CounterWildcardSupport) source;

        MetricDefinition[] replacedMetrics = support.replaceWildcardCounter(jobConfig).getExpandedDefinitions();
        CounterSeries series = source.queryCounter(jobConfig.copyWithMetrics(replacedMetrics));
        assertNotNull(series);
        assertNotSame(series, jobConfig);
    }


    /**
     * Counters written in English are expanded with the names of the machine, and a filter
     * names the counters found. Runs on every system: the Windows is a fake.
     */
    @Test
    public void testEnglishCountersAndFilteredLabels() throws Exception {
        String machine = java.net.InetAddress.getLocalHost().getHostName();
        FakeCounterAccess windows = new FakeCounterAccess();
        windows.expansions.put("\\\\" + machine + "\\Netzwerkschnittstelle(*)\\Empfangene Bytes/s", java.util.List.of(
                "\\\\" + machine + "\\Netzwerkschnittstelle(Intel[R] 82574L)\\Empfangene Bytes/s",
                "\\\\" + machine + "\\Netzwerkschnittstelle(Loopback)\\Empfangene Bytes/s"));
        WindowsPerformanceCounterSource source = new WindowsPerformanceCounterSource(windows);

        MetricDefinition[] expanded = source.replaceWildcardCounter(jobConfig.copyWithMetrics(new MetricDefinition[]{
                new MetricDefinition("\\Processor(_Total)\\% Processor Time", "cpu_usage_percent"),
                new MetricDefinition("\\Network Interface(*)\\Bytes Received/sec", "network_receive ${FILTER:1}",
                        ".*\\((.*)\\)\\\\[^\\\\]*", true),
        })).getExpandedDefinitions();

        java.util.Map<String, String> counterByLabel = new java.util.TreeMap<>();
        for (MetricDefinition definition : expanded) {
            counterByLabel.put(definition.getLabel(), definition.getDefinition());
        }
        assertEquals(java.util.Map.of(
                "cpu_usage_percent", "\\\\" + machine + "\\Prozessor(_Total)\\Prozessorzeit (%)",
                "network_receive Intel[R] 82574L", "\\\\" + machine + "\\Netzwerkschnittstelle(Intel[R] 82574L)\\Empfangene Bytes/s",
                "network_receive Loopback", "\\\\" + machine + "\\Netzwerkschnittstelle(Loopback)\\Empfangene Bytes/s"),
                counterByLabel);
    }

    @Test
    public void testGetId() throws Exception {
        WindowsPerformanceCounterSource source = new WindowsPerformanceCounterSource();
        Assert.assertEquals(source.getId(), "win");
    }

}
