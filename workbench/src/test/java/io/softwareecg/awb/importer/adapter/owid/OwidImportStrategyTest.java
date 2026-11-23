package io.softwareecg.awb.importer.adapter.owid;

import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.importer.testsupport.LocalImportStrategyRunner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is a unit test for the OWID ImportStrategy.
 */
public class OwidImportStrategyTest {
    private static final String SAMPLE_FILE = "testdata/sample.csv";

    @Test
    public void testStrategy() {
        LocalImportStrategyRunner runner = LocalImportStrategyRunner.newBuilder()
                .strategyToTest(new OwidImportStrategy(List.of(
                        ClassLoader.getSystemResource(SAMPLE_FILE).getFile()
                )))
                .activateRemoteSolrExport() // exports the data proceed by the strategy to an standalone Solr instance
                .useProject("myTestProject") // assign the data in the remote Solr the this project
                .build();


        List<TimeSeries> seriesData = runner.runStrategy();
        assertThat(seriesData.size()).isEqualTo(7844);
    }
}
