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

import io.softwareecg.collector.dt.CollectorJobConfiguration;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.dt.ProcessInfo;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the  UnixTopCounterSource
 */
public class UnixDiskFreeMetricDefinitionSourceTest {

    @Test
    public void testExpandWildcards() throws IOException {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig().copyWithMetrics(getCounters());

        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.DF_SOURCE1));

        UnixDiskFreeCounterSource unixDiskFreeCounterSource = new UnixDiskFreeCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics = unixDiskFreeCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, notNullValue());

        //Filesystem     1K-blocks   Used Available Use% Mounted on
        assertThat(replacedWildcardMetrics.length, is(equalTo(4)));

        double value = unixDiskFreeCounterSource.querySingleCounter(config, getCounters()[0]);
        assertThat(value, is(equalTo(25600d)));

        value = unixDiskFreeCounterSource.querySingleCounter(config, getCounters()[1]);
        assertThat(value, is(equalTo(5580d)));

        value = unixDiskFreeCounterSource.querySingleCounter(config, getCounters()[2]);
        assertThat(value, is(equalTo(20021d)));

        value = unixDiskFreeCounterSource.querySingleCounter(config, getCounters()[3]);
        assertThat(value, is(equalTo(22d)));
    }

    private MetricDefinition[] getCounters() {
        return new MetricDefinition[]{
                new MetricDefinition("1M-blocks", ""),
                new MetricDefinition("Used", ""),
                new MetricDefinition("Available", ""),
                new MetricDefinition("Use%", "")};
    }

    public static ProcessInfo getProcessInfo() {
        return new ProcessInfo("/lfs/wwwmnt/crmcockpits-i-shared1", "processLabel", "%1:22 PAR:12", null);
    }

    @Test
    public void testUnixDFId() {
        CommandExecutor connectorMock = mock(CommandExecutor.class);
        UnixDiskFreeCounterSource unixDiskFreeCounterSource = new UnixDiskFreeCounterSource(connectorMock);

        assertThat(unixDiskFreeCounterSource.getId(), is(equalTo("unixdf")));

    }
}
