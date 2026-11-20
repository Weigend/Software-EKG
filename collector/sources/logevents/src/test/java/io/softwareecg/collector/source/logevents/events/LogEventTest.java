/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 12.02.14 11:35
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.events;


import io.softwareecg.collector.source.logevents.types.events.LogEvent;
import io.softwareecg.collector.source.logevents.types.events.LogType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 */
public class LogEventTest {

    /*
     * The regex to describe the log entry
     * (?<xxPAYLOADxx>.*\\sPERF\\s(?<xxEVENTTYPExx>\\[I\\])\\s(?<NAME>[A-Za-z0-9.]+)\\s\\[(?<TIME>\\d{2}-\\d{2}-\\d{4}\\s\\d{2}:\\d{2}:\\d{2},\\d{3});(?<xxENDDATExx>\\d{2}-\\d{2}-\\d{4}\\s\\d{2}:\\d{2}:\\d{2},\\d{3});(?<VALUE>\\d{3})\\])
     */

    /**
     * Some sample logs
     */
    private static final String CALL_LOG = "16:49:42.921 [main] INFO  d.q.e.c.s.solr.producer.EventLog - PERF [I] SampleClass [25-02-2014 16:49:42,868;25-02-2014 16:49:42,918;112]";
    private static final String WRONG_CALL = "16:49:42.921 [main] INFO  d.q.e.c.s.solr.producer.EventLog - PERF [X] SampleClass [25-02-2014 16:49:42,868;25-02-2014 16:49:42,918;132]";
    private static final String INVALID_DATE_CALL = "16:49:42.921 [main] INFO  d.q.e.c.s.solr.producer.EventLog - PERF [I] SampleClass [25-32-2014 16:49:42,868;25-02-2014 16:49:42,918;162]";


    /**
     * The test object
     */
    private LogEvent event;

    @BeforeClass
    public static void beforeClass() {
        String path = LogEventTest.class.getResource("/log-event-config.properties").getPath();
        System.setProperty("collectorPropertiesPath", path);
    }

    @Before
    public void beforeEachTest() {
        event = new LogEvent(CALL_LOG, new LogType("logPerformanceEvent"));
    }

    @Test
    public void testGetEventName() {
        assertThat(event.getName(), is("SampleClass"));
    }


    @Test
    public void testGetDate() {
        Date expected = new Date(1393346982868L);
        assertThat(event.getDate(), is(expected));
    }

    //@Test
    // @todo: This test case doen't work anymore because the JAVA DateTimeFormatter API which is now in use, doesn't accept the missing zero.
    public void testGetDateWithRemovedZerosInMilliseconds() throws Exception {
        String callLog = "16:49:42.921 [main] INFO  d.q.e.c.s.solr.producer.EventLog - PERF [I] SampleClass [25-02-2014 16:49:42,86;25-02-2014 16:49:42,918;11]";
        LogEvent event = new LogEvent(callLog, new LogType("logPerformanceEvent"));
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss,SSS");
        df.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));

        Date expected = df.parse("25-02-2014 16:49:42,860");
        assertThat(event.getDate(), is(expected));
    }

    @Test
    public void testGetDuration() {
        assertThat(event.getValue(), is(112D));
    }

    @Test
    public void testGetGroup() {
        assertThat(event.getGroup(), is("Sales Performance Event"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongLog() throws Exception {
        new LogEvent(WRONG_CALL, new LogType("logPerformanceEvent"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidDate() throws Exception {
        new LogEvent(INVALID_DATE_CALL, new LogType("logPerformanceEvent"));
    }

    @Test
    public void testEndMatches() throws Exception {
        assertThat(event.endMatches(""), is(false));
    }

    @Test
    public void testHashCode() throws Exception{
        assertThat(event.hashCode(),not(0));
    }

    @Test
    public void testToString() throws Exception{
        assertThat(event.toString(),notNullValue());
    }







}
