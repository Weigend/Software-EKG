package io.softwareecg.collector.source.logevents.events;

import io.softwareecg.collector.source.logevents.types.events.LogEvent;
import io.softwareecg.collector.source.logevents.types.events.LogType;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class LogTypeTest {

    private static final String eventType = "logPerformanceEvent";

    /**
     * The test object
     */
    private LogType logType;

    public static void beforeTestClass() {
        System.setProperty("collectorPropertiesPath", "/log-event-config.properties");
    }

    @Before
    public void beforeEachTest() {
        beforeTestClass();
        logType = new LogType(eventType);
    }

    @Test
    public void testMatches() throws Exception {
        String testLog = "16:49:42.921 [main] INFO  d.q.e.c.s.solr.producer.EventLog - PERF [I] SampleClass [25-02-2014 16:49:42,868;25-02-2014 16:49:42,918;112]";
        assertThat(logType.startMatches(testLog), is(true));
    }

    @Test
    public void testNotMatching() throws Exception {
        String wrongTestLog = "2014-02-05 10:39:36,698 INFO  [ol-1; w: 8] [tmr ] [     RemoteServicesBackendImpl.callMethod                         ] [                    ,                     ] [ 218]: [UserId: 1000075] MethodCall: GetTypausstattungRowsCall\n" +
                "Start: 2014-02-05 10:39:15.142;x End: 2014-02-05 10:39:36.698; Duration: 0:00:21.556";
        assertThat(logType.startMatches(wrongTestLog), is(false));
    }

    @Test
    public void testParseEventFrom() throws Exception {
        String testLog = "16:49:42.921 [main] INFO  d.q.e.c.s.solr.producer.EventLog - PERF [I] SampleClass [25-02-2014 16:49:42,868;25-02-2014 16:49:42,918;112]";
        LogEvent logEvent = logType.parseType(testLog);
        assertThat(logEvent, notNullValue());
    }

    @Test
    public void testGetValueFormat() throws Exception {
        assertThat(logType.getValueFormat(), is("0.0"));
    }

    @Test
    public void testGetDateFormat() throws Exception {
        // todo assertThat(logType.getDateFormat(), is(DateTimeFormat.orPattern("dd-MM-yyyy HH:mm:ss,SSS")));
    }

    @Test
    public void testGetLogPattern() throws Exception {
        assertThat(logType.getStartPattern().toString(), is("(?<xxPAYLOADxx>.*\\sPERF\\s(?<xxEVENTTYPExx>\\[I\\])\\s(?<NAME>[A-Za-z0-9.]+)\\s\\[(?<TIME>\\d{2}-\\d{2}-\\d{4}\\s\\d{2}:\\d{2}:\\d{2},\\d+);(?<xxENDDATExx>\\d{2}-\\d{2}-\\d{4}\\s\\d{2}:\\d{2}:\\d{2},\\d+);(?<VALUE>\\d+)\\])"));
    }

    @Test
    public void testGetLogTypeName() throws Exception {
        assertThat(logType.getName(), is("logPerformanceEvent"));

    }

    @Test
    public void testGetGroup() throws Exception {
        assertThat(logType.getGroup(), is("Sales Performance Event"));
    }


}
