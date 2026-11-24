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
import io.softwareecg.collector.source.CounterSeries;
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
 * Tests for the  UnixIOCounterSource
 */
public class UnixIOMetricDefinitionSourceTest {

    @Test
    public void testExpandWildcards() throws IOException {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig().copyWithMetrics(getCounters());
        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.IO_SOURCE1));

        UnixIOCounterSource unixIOCounterSource = new UnixIOCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics = unixIOCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, notNullValue());

        //  rBlk_nor/s   wBlk_nor/s   rBlk_dir/s   wBlk_dir/s   rBlk_svr/s   wBlk_svr/s     ops/s    rops/s    wops/s
        assertThat(replacedWildcardMetrics.length, is(equalTo(9)));

        double value = unixIOCounterSource.querySingleCounter(config, replacedWildcardMetrics[0]);
        assertThat(value, is(greaterThanOrEqualTo(0.0)));
    }

    private MetricDefinition[] getCounters() {
        return new MetricDefinition[]{new MetricDefinition("\\Filesystem(nfsa013.lan:/vol/agr051/qtr01841/v00003-i/app1)\\*", null, null, Boolean.TRUE)};
    }


    @Test
    public void testExpandWildcardsForSystemWithoutFilesystem() throws IOException {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig().copyWithMetrics(
                new MetricDefinition[]{new MetricDefinition("\\Device(sda)\\*", null, null, Boolean.TRUE)});

        CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.IO_SOURCE5));

        UnixIOCounterSource unixIOCounterSource = new UnixIOCounterSource(connectorMock);
        MetricDefinition[] replacedWildcardMetrics = unixIOCounterSource.replaceWildcardCounter(config).getExpandedDefinitions();
        assertThat(replacedWildcardMetrics, notNullValue());

        // rrqm/s   wrqm/s   r/s   w/s   rsec/s   wsec/s avgrq-sz avgqu-sz   await  svctm  %datamanagement
        assertThat(replacedWildcardMetrics.length, is(equalTo(11)));

        double value = unixIOCounterSource.querySingleCounter(config, replacedWildcardMetrics[0]);
        assertThat(value, is(greaterThanOrEqualTo(0.0)));
    }

    @Test
    public void testQueryCounterWithMultipleReports() throws IOException {
        CollectorJobConfiguration config = UnixTestHelper.getCollectorJobConfig().copyWithMetrics(
                new MetricDefinition[]{new MetricDefinition("\\Device(cciss/c0d0)\\wrqm/s", null, null, Boolean.TRUE)});

                CommandExecutor connectorMock = mock(CommandExecutor.class);
        when(connectorMock.executeCommand(anyString(), any())).thenReturn(UnixTestHelper.loadResultAsString(UnixTestHelper.IO_SOURCE6));

        UnixIOCounterSource unixIOCounterSource = new UnixIOCounterSource(connectorMock);
        CounterSeries series = unixIOCounterSource.queryCounter(config);

        assertThat(series, notNullValue());
        assertThat(series.getValues().size(), is(equalTo(1)));
        assertThat(series.getValues().get(0), is(equalTo(1.49)));
    }

}
