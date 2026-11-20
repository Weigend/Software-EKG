package io.softwareecg.collector.source.logevents.interval;

import io.softwareecg.collector.source.logevents.types.interval.IntervalType;
import io.softwareecg.collector.source.logevents.types.interval.LogIntervalEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class IntervalTypeTest {

    /**
     * Our test object
     */
    private IntervalType type;

    @Before
    public void beforeTestClass() {
        String path =IntervalTypeTest.class.getResource("/log-event-config.properties").getPath();
        System.setProperty("collectorPropertiesPath", path);
        type = new IntervalType("psdhImportInterval");
    }


    @Test
    public void testParseEventFrom() throws Exception {
        String logLine = "2013-09-25 16:10:20,059 DEBUG Starting import of Motorformen...";
        LogIntervalEvent interval = type.parseType(logLine);
        assertThat(interval, notNullValue());
    }

    @Test
    public void testMatches() throws Exception {
        String logLine = "2013-09-25 16:10:20,059 DEBUG Starting import of Motorformen...";
        assertThat(type.startMatches(logLine), is(true));
    }

    @Test
    public void testGetIntervalStartPattern() throws Exception {
        assertThat(type.getStartPattern().toString(), is(Pattern.compile("(?<xxStartLogxx>(?s)(?<TIME>\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3})\\s.*Starting\\simport\\sof\\s(?<NAME>[A-Za-z]*)....*)").toString()));
    }

    @Test
    public void testGetIntervalEndPattern() throws Exception {
        assertThat(type.getEndPattern().toString(), is(Pattern.compile("(?<xxEndLogxx>(?s)(?<TIME>\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3})\\s.*Starting\\s(import\\sof\\s(?<xxFOLLOWERxx>[A-Za-z]*)....*|.*))").toString()));
    }

    @Test
    public void testGetGroup() throws Exception {
        assertThat(type.getGroup(), is("PSDH-IMPORT"));
    }
}
