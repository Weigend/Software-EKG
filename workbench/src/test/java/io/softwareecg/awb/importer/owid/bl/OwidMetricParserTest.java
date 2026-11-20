package io.softwareecg.awb.importer.owid.bl;

import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OwidMetricParserTest {
    private static final String CORRECT_FILE = "testdata/validateFile_correct.csv";

    @Test
    void parseToRawSeriesStream() throws URISyntaxException {
        File input = new File(ClassLoader.getSystemResource(CORRECT_FILE).toURI());

        Stream<RawSeriesData> actual = OwidMetricParser.parseToRawSeriesStream(input);

        List<RawSeriesData> collect = actual.collect(Collectors.toUnmodifiableList());
        assertThat(collect).hasSize(37);
    }
}
