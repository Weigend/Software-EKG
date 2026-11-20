/**
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 19.03.14 09:55
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.interval;


import io.softwareecg.collector.source.logevents.types.interval.IntervalType;
import io.softwareecg.collector.source.logevents.types.interval.LogIntervalEvent;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class LogIntervalEventTest {


    private final String logLine = "2013-09-25 16:10:20,059 DEBUG Starting import of Motorformen...";


    private LogIntervalEvent logIntervalEvent;


    @BeforeClass
    public static void beforeTestClass() {
        String path = LogIntervalEventTest.class.getResource("/log-event-config.properties").getPath();
        System.setProperty("collectorPropertiesPath", path);
    }

    @Before
    public void beforeEachTest() {
        logIntervalEvent = new LogIntervalEvent(logLine, new IntervalType("psdhImportInterval"));
    }

    @Test
    public void testCreateLogInterval() throws Exception {
        assertThat(logIntervalEvent, notNullValue());
    }

    @Test
    public void testGetLogIntervalName() throws Exception {
        assertThat(logIntervalEvent.getName(), is("Motorformen"));
    }

    @Test
    public void testGetLogIntervalStartDetected() throws Exception {
        assertThat(logIntervalEvent.startDetected(), is(true));
    }

    @Test
    public void testGetLogIntervalStartDate() throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = df.parse("2013-09-25 16:10:20,059");
        assertThat(logIntervalEvent.getDate(), is(expected));
    }

    @Test
    public void testGetLogIntervalEndDate() throws Exception {
        String endLine = "2013-09-25 16:10:20,114 DEBUG Starting import of Kraftstoffarten...";
        logIntervalEvent.parseEndLine(endLine);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = simpleDateFormat.parse("2013-09-25 16:10:20,114");
        assertThat(logIntervalEvent.getEndDate(), is(expected));
    }

    @Test
    public void testGetLogIntervalValue() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        String endLine = "2013-09-25 16:11:20,114 DEBUG Starting import of Kraftstoffarten...";
        long end = sdf.parse("2013-09-25 16:11:20,114").getTime();
        long start = sdf.parse("2013-09-25 16:10:20,059").getTime();

        double duration = end - start;

        logIntervalEvent.parseEndLine(endLine);

        assertThat(logIntervalEvent.getValue(), is(duration));
    }

    @Test
    public void testGetLogIntervalGroupName() throws Exception {
        assertThat(logIntervalEvent.getGroup(), is("PSDH-IMPORT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewLogIntervalEventForWrongLog() throws Exception {
        String wrongLogLine = "I will no match.";
        logIntervalEvent = new LogIntervalEvent(wrongLogLine, new IntervalType("psdhImportInterval"));

    }

}
