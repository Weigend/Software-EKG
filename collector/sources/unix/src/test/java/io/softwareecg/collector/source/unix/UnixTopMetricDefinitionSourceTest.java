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
package io.softwareecg.collector.source.unix;

import io.softwareecg.collector.config.CollectorJobConfiguration;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the  UnixTopCounterSource
 */
public class UnixTopMetricDefinitionSourceTest {

    @Test
    public void testExpandWildcards() throws IOException {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig().copyWithMetrics(getCounters());
        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.TOP_SOURCE5));

        UnixTopCounterSource unixTopCounterSource = new UnixTopCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics = unixTopCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, is(not(equalTo(null))));

        //PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME
        assertThat(replacedWildcardMetrics.length, is(equalTo(23)));

        double value = unixTopCounterSource.querySingleCounter(config, replacedWildcardMetrics[7]);
        assertThat(value, is(greaterThanOrEqualTo(0.0)));
        value = unixTopCounterSource.querySingleCounter(config, replacedWildcardMetrics[3]);
        assertThat(value, is(greaterThanOrEqualTo(0.0)));
    }

    private MetricDefinition[] getCounters() {
        return new MetricDefinition[]{new MetricDefinition("\\Process(java#1)\\*", "\\Process(java#1)\\*", null, Boolean.TRUE),
                new MetricDefinition("\\Memory\\*", "\\Memory\\*", null, Boolean.TRUE),
                new MetricDefinition("\\CPU\\*", "\\CPU\\*", null, Boolean.TRUE)};
    }

    @Test
    public void testUptimeCounter() throws IOException {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig()
                .copyWithMetrics(new MetricDefinition[]{new MetricDefinition("\\Uptime\\up", "global")});

        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.TOP_SOURCE8));

        UnixTopCounterSource unixTopCounterSource = new UnixTopCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics  = unixTopCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, is(not(equalTo(null))));

        //PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME
        assertThat(replacedWildcardMetrics.length, is(equalTo(1)));

        double value = unixTopCounterSource.querySingleCounter(config, replacedWildcardMetrics[0]);
        assertThat(value, is(greaterThanOrEqualTo(0.0)));
    }

    @Test
    public void testUserCounterForOneUser() throws Exception {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig()
                .copyWithMetrics(new MetricDefinition[]{new MetricDefinition("\\User\\user", "global", null, Boolean.TRUE)});

        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.TOP_SOURCE9));

        UnixTopCounterSource unixTopCounterSource = new UnixTopCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics = unixTopCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, is(not(equalTo(null))));

        //PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME
        assertThat(replacedWildcardMetrics.length, is(equalTo(1)));

        double value = unixTopCounterSource.querySingleCounter(config, replacedWildcardMetrics[0]);
        assertThat(value, is(equalTo(1.)));
    }

    @Test
    public void testLeadingSshErrorMsg() throws Exception {

        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig()
                .copyWithMetrics(new MetricDefinition[]{
                        new MetricDefinition("\\User\\users", "global", null, Boolean.TRUE),
                        new MetricDefinition("\\Load\\*", "global", null, Boolean.TRUE),
                        new MetricDefinition("\\Uptime\\*", "global", null, Boolean.TRUE),
                });

        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.TOP_SOURCE10));


        UnixTopCounterSource unixTopCounterSource = new UnixTopCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics = unixTopCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, is(not(equalTo(null))));

        //PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME
        assertThat(replacedWildcardMetrics.length, is(equalTo(5)));

        double usersCount = unixTopCounterSource.querySingleCounter(config, replacedWildcardMetrics[0]);
        assertThat(usersCount, is(equalTo(2d)));

        double loadFive = unixTopCounterSource.querySingleCounter(config, replacedWildcardMetrics[2]);
        assertThat(loadFive, is(equalTo(0.03)));
    }

    @Test
    public void testUserCounterForUsers() throws Exception {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig()
                .copyWithMetrics(new MetricDefinition[]{new MetricDefinition("\\User\\user", "global", null, Boolean.TRUE)});

        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.TOP_SOURCE1));

        UnixTopCounterSource unixTopCounterSource = new UnixTopCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics = unixTopCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, is(not(equalTo(null))));

        //PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME
        assertThat(replacedWildcardMetrics.length, is(equalTo(1)));

        double value = unixTopCounterSource.querySingleCounter(config, replacedWildcardMetrics[0]);
        assertThat(value, is(equalTo(2.)));
    }
}
