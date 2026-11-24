package io.softwareecg.awb.importer.adapter.owid;

import io.softwareecg.awb.importer.model.RawSeriesData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OwidMetricParserTest {
    private static final String CORRECT_FILE = "testdata/validateFile_correct.csv";
    private static final String COMPACT_FILE = "testdata/owid_compact.csv";

    @Test
    void parseToRawSeriesStream() throws URISyntaxException {
        File input = new File(ClassLoader.getSystemResource(CORRECT_FILE).toURI());

        Stream<RawSeriesData> actual = OwidMetricParser.parseToRawSeriesStream(input);

        List<RawSeriesData> collect = actual.collect(Collectors.toUnmodifiableList());
        assertThat(collect).hasSize(37);
    }

    /**
     * The file of the OWID catalog (compact.csv) orders its columns differently: the country
     * first, then the date, and the continent among the last ones.
     */
    @Test
    void readsTheCompactFileOfTheCatalog() throws URISyntaxException {
        File input = new File(ClassLoader.getSystemResource(COMPACT_FILE).toURI());

        List<RawSeriesData> series = OwidMetricParser.parseToRawSeriesStream(input).toList();

        RawSeriesData totalCases = series.stream()
                .filter(s -> "total_cases".equals(s.getMetricName()))
                .findFirst().orElseThrow();
        assertThat(totalCases.getValues()).hasSize(12);
        assertThat(totalCases.getValues().get(0).getValue()).isEqualTo(55733d);
        assertThat(totalCases.getSeriesLabels().values()).contains("Afghanistan", "Asia");
        // what says which row it is must not become a metric
        assertThat(series).extracting(RawSeriesData::getMetricName)
                .doesNotContain("country", "date", "code", "continent");
    }
}
