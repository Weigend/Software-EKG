package io.softwareecg.collector.source.unix.analyzer;


import org.junit.Test;

import static io.softwareecg.collector.source.unix.analyzer.AnalyzerStringUtils.convertToDouble;
import static io.softwareecg.collector.source.unix.analyzer.AnalyzerStringUtils.parseUptimeToMinutes;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class AnalyzerStringUtilsTest {

    @Test
    public void testParseUptimeToMinutes() {

        String[] formats = new String[]{"23:47,",
                "123 days, 20:53,",
                "301 days,  1:35,",
                "1 day, 54 min,",
                "385 days, 14:00,",
                "",
                ",",
                "12:00",
                "2 hours",
                "12 unknown" /* 2 minutes */};

        assertThat(parseUptimeToMinutes(formats[0]), is(equalTo(1380d + 47d)));
        assertThat(parseUptimeToMinutes(formats[1]), is(equalTo(177120d + 1200d + 53d)));
        assertThat(parseUptimeToMinutes(formats[2]), is(equalTo(433440d + 60d + 35d)));
        assertThat(parseUptimeToMinutes(formats[3]), is(equalTo(1440d + 54d)));
        assertThat(parseUptimeToMinutes(formats[4]), is(equalTo(554400d + 840d)));
        assertThat(parseUptimeToMinutes(formats[5]), is(equalTo(0d)));
        assertThat(parseUptimeToMinutes(formats[6]), is(equalTo(0d)));
        assertThat(parseUptimeToMinutes(formats[7]), is(equalTo(720d)));
        assertThat(parseUptimeToMinutes(formats[8]), is(equalTo(120d)));
        assertThat(parseUptimeToMinutes(formats[9]), is(equalTo(12d)));

    }

    @Test
    public void testConvertToDouble() {
        assertThat(convertToDouble(null), is(equalTo(-1.0)));
        assertThat(convertToDouble(""), is(equalTo(-1.0)));

        assertThat(convertToDouble("1K"), is(equalTo(1024.0)));
        assertThat(convertToDouble("2k"), is(equalTo(2048.0)));

        assertThat(convertToDouble("1M"), is(equalTo(1024.0*1024.0)));
        assertThat(convertToDouble("2m"), is(equalTo(2048.0*1024.0)));
        assertThat(convertToDouble("10.5M"), is(equalTo(1024.0*1024.0*10.5)));

        assertThat(convertToDouble("15%"), is(equalTo(0.15)));
        assertThat(convertToDouble("30.2%"), is(equalTo(0.302)));
    }
}