package io.softwareecg.collector.source.logevents.events;


import io.softwareecg.collector.source.logevents.types.events.LogEvent;
import io.softwareecg.collector.source.logevents.types.events.LogType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 */
public class LogSalesTest {

    /**
     * Some sample logs
     */
    private final String callLog = "2014-02-05 10:39:36,698 INFO  [ol-1; w: 8] [tmr ] [     RemoteServicesBackendImpl.callMethod                         ] [                    ,                     ] [ 218]: [UserId: 1000075] MethodCall: GetTypausstattungRowsCall\n" +
            "Start: 2014-02-05 10:39:15.142; End: 2014-02-05 10:39:36.698; Duration: 0:00:21.556\n";


    @BeforeClass
    public static void beforeClass() {
        String path = LogSalesTest.class.getResource("/log-event-config.properties").getPath();
        System.setProperty("collectorPropertiesPath", path);

    }

    @Test
    public void testGetEventName() throws Exception {
        LogEvent event = new LogEvent(callLog, new LogType("salesServiceBackend"));
        assertThat(event.getName(), is("GetTypausstattungRowsCall"));
    }

    @Test
    public void testGetDate() throws Exception {
        LogEvent event = new LogEvent(callLog, new LogType("salesServiceBackend"));
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = df.parse("2014-02-05 10:39:15.142");
        assertThat(event.getDate().getTime(), is(expected.getTime()));
    }

    @Test
    public void testGetDuration() throws Exception {
        LogEvent event = new LogEvent(callLog, new LogType("salesServiceBackend"));
        assertThat(event.getValue(), is(21556D));
    }

    @Test
    public void testGetPropertyKeys() throws Exception {
        LogEvent event = new LogEvent(callLog, new LogType("salesServiceBackend"));
        //todo Set<String> expected = Sets.newHashSet("xxENDDATExx","xxLOGxx");
        //assertThat(event.getPropertyNames(), is(expected));
    }

}
