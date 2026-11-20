//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend
//       creation date: 21.12.2010
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-07-21 13:03:04 +0200 (Mi, 21 Jul 2010) $
//       revision:      $Revision: 1082 $
//       description:   Unix-Top Analyzer
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source.unix.analyzer;

import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterSourceListener;
import io.softwareecg.collector.source.unix.UnixTestHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * The Class TopAnalyzerTest.
 */
public class TopAnalyzerTest {

    /**
     * Test expand counter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExpandCounter() throws Exception {

        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE5);
        TopAnalyzer analyzer = new TopAnalyzer(topOutput);
        Collection<? extends MetricDefinition> counters = analyzer.expandCounter(new MetricDefinition("\\Process(java#1)\\*", "\\Process(java#1)\\*"));
        assertThat(counters.size(), is(equalTo(11)));
        String definition = counters.iterator().next().getDefinition();
        assertThat(definition.startsWith("\\Process(java#1)\\"), is(true));
        assertThat(counters.contains(new MetricDefinition("\\Process(java#1)\\PID", "\\Process(java#1)\\PID")), is(true));
        assertThat(counters.contains(new MetricDefinition("\\Process(java#1)\\SHR", "\\Process(java#1)\\SHR")), is(true));

        counters = analyzer.expandCounter(new MetricDefinition("\\Process(*/jav*)\\*", "\\Process(jav*)\\*"));
        assertThat(counters.size(), is(equalTo(44)));

        MetricDefinition metricDefinition1 = counters.iterator().next();
        assertThat(metricDefinition1.getDefinition().equals(metricDefinition1.getLabel()), is(true));

        for (MetricDefinition metricDefinition : counters) {
            assertThat(metricDefinition.getDefinition().startsWith("\\Process(/usr/lib/jvm/jdk1.7.0_02/bin/java -Xmx50"), is(true));
            assertThat(metricDefinition.getDefinition().matches(".*\\\\(RES|USER|PR|NI|VIRT|SHR|S|TIME|CPU|MEM|PID)$"), is(true));
        }

        counters = analyzer.expandCounter(new MetricDefinition("\\Process(*/jav*)\\P*", "\\Process(jav*)\\P*"));
        assertThat(counters.size(), is(equalTo(8)));

        for (MetricDefinition metricDefinition : counters) {
            assertThat(metricDefinition.getDefinition().startsWith("\\Process(/usr/lib/jvm/jdk1.7.0_02/bin/java -Xmx50"), is(true));
            assertThat(metricDefinition.getDefinition().matches(".*\\\\(PR|PID)$"), is(true));
        }

        counters = analyzer.expandCounter(new MetricDefinition("\\Process(jav*)\\*", "\\Process(jav*)\\*"));

        analyzer.setTerminalOutput(UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE6));
        counters = analyzer.expandCounter(new MetricDefinition("\\Process(*e4c2 *)\\*", "\\Process(*e4c2 *)\\*"));
        counters.size();
    }

    /**
     * Test expand counter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExpandCounterSwap() throws Exception {

        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE5);
        TopAnalyzer analyzer = new TopAnalyzer(topOutput);
        Collection<? extends MetricDefinition> countersSwap = analyzer.expandCounter(new MetricDefinition("\\Swap\\*", "\\Swap\\*"));
        assertThat(countersSwap.size(), is(equalTo(4)));

        Collection<? extends MetricDefinition> countersMemory = analyzer.expandCounter(new MetricDefinition("\\Memory\\*", "\\Memory\\*"));
        assertThat(countersMemory.size(), is(equalTo(4)));

        Collection<? extends MetricDefinition> countersTasks = analyzer.expandCounter(new MetricDefinition("\\Tasks\\*", "\\Tasks\\*"));
        assertThat(countersTasks.size(), is(equalTo(5)));

        Collection<? extends MetricDefinition> countersCpu = analyzer.expandCounter(new MetricDefinition("\\Cpu\\*", "\\Cpu\\*"));
        assertThat(countersCpu.size(), is(equalTo(8)));

        Collection<? extends MetricDefinition> countersUsers = analyzer.expandCounter(new MetricDefinition("\\User\\*", "\\User\\*"));
        assertThat(countersUsers.size(), is(equalTo(1)));

        Collection<? extends MetricDefinition> countersLoad = analyzer.expandCounter(new MetricDefinition("\\Load\\*", "\\Load\\*"));
        assertThat(countersLoad.size(), is(equalTo(3)));

        Collection<? extends MetricDefinition> countersUptime = analyzer.expandCounter(new MetricDefinition("\\Uptime\\*", "\\Uptime\\*"));
        assertThat(countersUptime.size(), is(equalTo(1)));


        try {
            analyzer.expandCounter(new MetricDefinition("\\Unknown\\*", "\\Unkowon\\*"));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(equalTo("No enum constant io.softwareecg.collector.source.unix.counters.top.TopCounterGroupEnum.Unknown")));
        }

    }

    /**
     * Test expand counter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExpandCounterPlaceholder() throws Exception {

        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE5);
        TopAnalyzer analyzer = new TopAnalyzer(topOutput);
        Collection<? extends MetricDefinition> countersSwap = analyzer.expandCounter(new MetricDefinition("\\Sw*\\*", "\\Sw*\\*"));
        assertThat(countersSwap.size(), is(equalTo(4)));

        Collection<? extends MetricDefinition> countersMemory = analyzer.expandCounter(new MetricDefinition("\\Me*\\*", "\\Me*\\*"));
        assertThat(countersMemory.size(), is(equalTo(4)));

        Collection<? extends MetricDefinition> countersTasks = analyzer.expandCounter(new MetricDefinition("\\Ta*\\*", "\\Ta*\\*"));
        assertThat(countersTasks.size(), is(equalTo(5)));

        Collection<? extends MetricDefinition> countersCpu = analyzer.expandCounter(new MetricDefinition("\\Cp*\\*", "\\Cp*\\*"));
        assertThat(countersCpu.size(), is(equalTo(8)));

        Collection<? extends MetricDefinition> countersUsers = analyzer.expandCounter(new MetricDefinition("\\Us*\\*", "\\Us*\\*"));
        assertThat(countersUsers.size(), is(equalTo(1)));

        Collection<? extends MetricDefinition> countersLoad = analyzer.expandCounter(new MetricDefinition("\\Lo*\\*", "\\Lo*\\*"));
        assertThat(countersLoad.size(), is(equalTo(3)));

        Collection<? extends MetricDefinition> countersUptime = analyzer.expandCounter(new MetricDefinition("\\Up*\\*", "\\Up*\\*"));
        assertThat(countersUptime.size(), is(equalTo(1)));

        Collection<? extends MetricDefinition> countersAll = analyzer.expandCounter(new MetricDefinition("\\*\\*", "\\*\\*"));
        assertThat(countersAll.size(), is(equalTo(26)));

    }

    /**
     * Test notify at processlist change.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNotifyAtProcesslistChange() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE4);
        ListenerMockup listener = new ListenerMockup();

        ArrayList<CounterSourceListener> counterSourceListeners = new ArrayList<>();
        counterSourceListeners.add(listener);

        TopAnalyzer analyzer = new TopAnalyzer(topOutput, counterSourceListeners);
        Collection<? extends MetricDefinition> counters = analyzer.expandCounter(new MetricDefinition("\\Process(*)\\PID", "\\Process(*)\\PID"));
        assertThat(counters.size(), is(equalTo(145)));

        assertThat(listener.isTriggered, is(false));
        analyzer.setTerminalOutput(UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE5));

        assertThat(listener.isTriggered, is(true));
    }


    /**
     * The Class ListenerMockup.
     */
    class ListenerMockup implements CounterSourceListener {

        /**
         * The is triggered.
         */
        public boolean isTriggered = false;

        /* (non-Javadoc)
         * @see io.softwareecg.collector.source.CounterSourceListener#counterChanged()
         */
        @Override
        public void counterChanged() {
            isTriggered = true;
        }
    }

    /**
     * Test full path top.
     *
     * @throws Exception the exception
     */
    @Test
    public void testFullPathTop() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE5);

        TopAnalyzer analyzer = new TopAnalyzer(topOutput);
        assertThat(analyzer.getCounter("\\Process(java)\\PID"), is(equalTo(6729.0)));
    }

    /**
     * Test analzer.
     *
     * @throws Exception the exception
     */
    /**
     * The format top prints today: the unit once in front of the memory lines, numbers with decimals.
     */
    @Test
    public void currentTopFormatIsReadAndTheDerivedCountersAreThere() throws Exception {
        TopAnalyzer analyzer = new TopAnalyzer(UnixTestHelper.loadResult("analyzer/top-output-linux-procps-ng.txt"));
        double mib = 1024.0 * 1024.0;

        assertThat(analyzer.getCounter("\\Memory\\total"), is(equalTo(96546.5 * mib)));
        assertThat(analyzer.getCounter("\\Memory\\used"), is(equalTo(14577.2 * mib)));
        assertThat(analyzer.getCounter("\\Memory\\free"), is(equalTo(58774.7 * mib)));
        assertThat(analyzer.getCounter("\\Memory\\buffers"), is(equalTo(26575.1 * mib)));
        assertThat(analyzer.getCounter("\\Memory\\available"), is(equalTo(81969.4 * mib)));
        assertThat(analyzer.getCounter("\\Swap\\total"), is(equalTo(50321.3 * mib)));
        assertThat(analyzer.getCounter("\\Swap\\used"), is(equalTo(0.0)));
        assertThat(analyzer.getCounter("\\Swap\\free"), is(equalTo(50321.3 * mib)));

        assertThat(analyzer.getCounter("\\CPU\\us"), is(equalTo(1.6)));
        assertThat(analyzer.getCounter("\\CPU\\id"), is(equalTo(97.3)));
        assertThat(analyzer.getCounter("\\CPU\\wa"), is(equalTo(0.2)));
        assertThat(analyzer.getCounter("\\Load\\one"), is(equalTo(1.24)));

        // derived: what is not idle, and the share of the memory that is not available to programs
        assertEquals(2.7, analyzer.getCounter("\\CPU\\used"), 1e-9);
        assertEquals(100.0 * (96546.5 - 81969.4) / 96546.5, analyzer.getCounter("\\Memory\\usedPercent"), 1e-9);
        assertEquals(0.0, analyzer.getCounter("\\Swap\\usedPercent"), 1e-9);

        assertThat(analyzer.getCounter("\\Process(postgres)\\CPU"), is(equalTo(7.1)));
        assertThat(analyzer.getCounter("\\Process(java)\\CPU"), is(equalTo(21.4)));
        assertThat(analyzer.getCounter("\\Process(java#1)\\CPU"), is(equalTo(3.5)));
    }

    @Test
    public void aDecimalCommaIsReadAsWell() throws Exception {
        TopAnalyzer analyzer = new TopAnalyzer(UnixTestHelper.loadResult("analyzer/top-output-linux-procps-ng-comma.txt"));
        double gib = 1024.0 * 1024.0 * 1024.0;

        assertThat(analyzer.getCounter("\\CPU\\us"), is(equalTo(1.6)));
        assertThat(analyzer.getCounter("\\CPU\\id"), is(equalTo(97.3)));
        assertEquals(2.7, analyzer.getCounter("\\CPU\\used"), 1e-9);
        assertThat(analyzer.getCounter("\\Memory\\total"), is(equalTo(94.3 * gib)));
        assertThat(analyzer.getCounter("\\Memory\\used"), is(equalTo(14.2 * gib)));
        assertThat(analyzer.getCounter("\\Load\\one"), is(equalTo(1.24)));
        assertThat(analyzer.getCounter("\\Load\\fifteen"), is(equalTo(1.68)));
    }

    /**
     * The older format has no "available": the share in use is then what top calls used.
     */
    @Test
    public void derivedCountersWorkWithTheOlderFormatToo() throws Exception {
        TopAnalyzer analyzer = new TopAnalyzer(UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE1));

        assertEquals(16.6, analyzer.getCounter("\\CPU\\used"), 1e-9);
        assertEquals(100.0 * 475336 / 1406812, analyzer.getCounter("\\Memory\\usedPercent"), 1e-9);
        assertEquals(0.0, analyzer.getCounter("\\Swap\\usedPercent"), 1e-9);
        assertThat(analyzer.getCounter("\\Memory\\available"), is(equalTo(-1.0)));
    }

    @Test
    public void testAnalzer() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE1);

        TopAnalyzer analyzer = new TopAnalyzer(topOutput);

        // Process counter
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\PID"), is(equalTo(1.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\USER"), is(equalTo(3506402.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\PR"), is(equalTo(20.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\NI"), is(equalTo(0.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\VIRT"), is(equalTo(3088.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\RES"), is(equalTo(1880.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\SHR"), is(equalTo(556.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\S"), is(equalTo(83.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\TIME"), is(equalTo(2.62)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\CPU"), is(equalTo(0.0)));
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\MEM"), is(equalTo(0.1)));

        // Multiple instances support with #x
        assertThat(analyzer.getCounter("\\Process([watchdog/0]#0)\\PID"), is(equalTo(5.0)));
        assertThat(analyzer.getCounter("\\Process([watchdog/0]#1)\\PID"), is(equalTo(6.0)));

        // Memory
        assertThat(analyzer.getCounter("\\Memory\\total"), is(equalTo(1.440575488E9)));
        assertThat(analyzer.getCounter("\\Memory\\used"), is(equalTo(4.86744064E8)));
        assertThat(analyzer.getCounter("\\Memory\\free"), is(equalTo(9.53831424E8)));
        assertThat(analyzer.getCounter("\\Memory\\total"), is(equalTo(1.440575488E9)));
        assertThat(analyzer.getCounter("\\Memory\\free"), is(equalTo(9.53831424E8)));
        assertThat(analyzer.getCounter("\\Memory\\buffers"), is(equalTo(1.46874368E8)));

        assertThat(analyzer.getCounter("\\Swap\\total"), is(equalTo(4.244201472E9)));
        assertThat(analyzer.getCounter("\\Swap\\used"), is(equalTo(0.0)));
        assertThat(analyzer.getCounter("\\Swap\\free"), is(equalTo(4.244201472E9)));
        assertThat(analyzer.getCounter("\\Swap\\total"), is(equalTo(4.244201472E9)));
        assertThat(analyzer.getCounter("\\Swap\\free"), is(equalTo(4.244201472E9)));
        assertThat(analyzer.getCounter("\\Swap\\cached"), is(equalTo(1.76144384E8)));

        // Tasks and CPU
        assertThat(analyzer.getCounter("\\Tasks\\total"), is(equalTo(142.0)));
        assertThat(analyzer.getCounter("\\Tasks\\running"), is(equalTo(1.0)));
        assertThat(analyzer.getCounter("\\Tasks\\sleeping"), is(equalTo(141.0)));
        assertThat(analyzer.getCounter("\\Tasks\\stopped"), is(equalTo(0.0)));
        assertThat(analyzer.getCounter("\\Tasks\\zombie"), is(equalTo(0.0)));

        assertThat(analyzer.getCounter("\\CPU\\us"), is(equalTo(4.4)));
        assertThat(analyzer.getCounter("\\CPU\\sy"), is(equalTo(8.0)));
        assertThat(analyzer.getCounter("\\CPU\\ni"), is(equalTo(0.3)));
        assertThat(analyzer.getCounter("\\CPU\\id"), is(equalTo(83.4)));
        assertThat(analyzer.getCounter("\\CPU\\hi"), is(equalTo(0.1)));
        assertThat(analyzer.getCounter("\\CPU\\si"), is(equalTo(0.0)));
        assertThat(analyzer.getCounter("\\CPU\\st"), is(equalTo(0.0)));

        // Misc
        assertThat(analyzer.getCounter("\\User\\user"), is(equalTo(2.0)));
        assertThat(analyzer.getCounter("\\Uptime\\up"), is(equalTo(10.0)));
        assertThat(analyzer.getCounter("\\Load\\one"), is(equalTo(0.23)));
        assertThat(analyzer.getCounter("\\Load\\five"), is(equalTo(0.25)));
        assertThat(analyzer.getCounter("\\Load\\fifteen"), is(equalTo(0.21)));
    }

    // todo ASSERT

    /**
     * Test analzer vtd.
     *
     * @throws Exception the exception
     */
    @Test
    public void testAnalzerVTD() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE2);

        TopAnalyzer analyzer = new TopAnalyzer(topOutput);

        // Memory
        assertThat(analyzer.getCounter("\\Memory\\total"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\used"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\free"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\total"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\free"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\buffers"), is(not(equalTo(-1.0))));

        // Tasks and CPU
        assertThat(analyzer.getCounter("\\Tasks\\total"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\running"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\sleeping"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\stopped"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\zombie"), is(not(equalTo(-1.0))));
    }

    /**
     * Test analzer ccr.
     *
     * @throws Exception the exception
     */
    @Test
    public void testAnalzerCCR() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE3);

        TopAnalyzer analyzer = new TopAnalyzer(topOutput);

        // Memory
        assertThat(analyzer.getCounter("\\Memory\\total"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\used"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\free"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\total"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\free"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Memory\\buffers"), is(not(equalTo(-1.0))));

        // Tasks and CPU
        assertThat(analyzer.getCounter("\\Tasks\\total"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\running"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\sleeping"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\stopped"), is(not(equalTo(-1.0))));
        assertThat(analyzer.getCounter("\\Tasks\\zombie"), is(not(equalTo(-1.0))));
    }

    /**
     * The top output of an application server with many users.
     *
     * @throws Exception the exception
     */
    @Test
    public void topAnalyzerApplicationServer() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult("analyzer/top-output-appi1.txt");
        TopAnalyzer analyzer = new TopAnalyzer(topOutput);
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\USER"), is(lessThan(0.)));
        assertThat(analyzer.getCounter("\\Process(java#2)\\USER"), is(greaterThan(0.)));

        topOutput = UnixTestHelper.loadResult("analyzer/top-output-appi2.txt");
        analyzer = new TopAnalyzer(topOutput);
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\USER"), is(lessThan(0.)));
        assertThat(analyzer.getCounter("\\Process(java#2)\\USER"), is(greaterThan(0.)));

        topOutput = UnixTestHelper.loadResult("analyzer/top-output-webi1.txt");
        analyzer = new TopAnalyzer(topOutput);
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\USER"), is(lessThan(0.)));
        assertThat(analyzer.getCounter("\\Process(httpd#2)\\USER"), is(not(equalTo(-1.0))));

        topOutput = UnixTestHelper.loadResult("analyzer/top-output-webi2.txt");
        analyzer = new TopAnalyzer(topOutput);
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\USER"), is(lessThan(0.)));
        assertThat(analyzer.getCounter("\\Process(httpd#2)\\USER"), is(not(equalTo(-1.0))));   // the number stands for the name of the user; its sign says nothing
    }

    @Test
    public void topAnalyzerWithErrors() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.TOP_SOURCE7);
        TopAnalyzer analyzer = new TopAnalyzer(topOutput);
        assertThat(analyzer.getCounter("\\Process(/sbin/init)\\USER"), is(lessThan(0.)));
        assertThat(analyzer.getCounter("\\Memory\\total"), is(not(equalTo(-1.0))));
        // not found
        assertThat(analyzer.getCounter("\\Process(java#2)\\USER"), is(lessThan(0.)));
    }
}
