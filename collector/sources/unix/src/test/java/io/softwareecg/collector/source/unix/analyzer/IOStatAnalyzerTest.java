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
import io.softwareecg.collector.source.unix.UnixTestHelper;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 06.03.13
 */
public class IOStatAnalyzerTest {

    /**
     * Test expand counter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExpandCounter() throws Exception {

        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.IO_SOURCE1);
        IOStatAnalyzer analyzer = new IOStatAnalyzer();
        analyzer.setTerminalOutput(topOutput);

        Collection<? extends MetricDefinition> counters = analyzer.expandCounter(new MetricDefinition("\\Filesystem(nfsa013.lan:/vol/agr051/qtr01841/v00003-i/app1)\\*", null));
        assertThat(counters.size(), is(equalTo(9)));
        String definition = counters.iterator().next().getDefinition();
        assertThat(definition.startsWith("\\Filesystem(nfsa013.lan:/vol/agr051/qtr01841/v00003-i/app1)\\"), is(true));

        counters = analyzer.expandCounter(new MetricDefinition("\\*(*nfsa172*)\\*", null));
        assertThat(counters.contains(new MetricDefinition("\\Filesystem(nfsa172:/vol/agr372/qtr08064/v001-i/app1)\\wBlk_dir/s", null)), is(true));

        assertThat(counters.contains(new MetricDefinition("\\Filesystem(nfsa172:/vol/agr372/qtr08064/v001-i/shared)\\wops/s", null)), is(true));

        topOutput = UnixTestHelper.loadResult(UnixTestHelper.IO_SOURCE4);
        analyzer = new IOStatAnalyzer();
        analyzer.setTerminalOutput(topOutput);

        counters = analyzer.expandCounter(new MetricDefinition("\\Filesystem(*qtr25405*)\\*", null));
        assertThat(counters.size(), is(equalTo(9)));

        String counterPartName = "\\Filesystem(nfsa869:/vol/agr885/qtr25405)\\";

        assertThat(counters.contains(new MetricDefinition(counterPartName + "rBlk_nor/s", counterPartName + "rBlk_nor/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "wBlk_nor/s", counterPartName + "wBlk_nor/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "rBlk_dir/s", counterPartName + "rBlk_dir/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "wBlk_dir/s", counterPartName + "wBlk_dir/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "rBlk_svr/s", counterPartName + "rBlk_svr/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "wBlk_svr/s", counterPartName + "wBlk_svr/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "ops/s", counterPartName + "ops/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "rops/s", counterPartName + "rops/s")), is(true));
        assertThat(counters.contains(new MetricDefinition(counterPartName + "wops/s", counterPartName + "wops/s")), is(true));
    }

    @Test
    public void testMultipleNFS() throws Exception {
        String[] topOutput = UnixTestHelper.loadResult(UnixTestHelper.IO_SOURCE4);
        IOStatAnalyzer analyzer = new IOStatAnalyzer();
        analyzer.setTerminalOutput(topOutput);

        assertThat(analyzer.getCounter("\\Filesystem(nfsa516.xyzgroup.net:/vol/agr885/qtr25820)\\ops/s"), is(equalTo(143.28)));
    }

}
