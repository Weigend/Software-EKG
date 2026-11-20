package io.softwareecg.collector.control;

import io.softwareecg.collector.control.monitor.CollectorCoreMonitor;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 */
public abstract class BaseIntegrationTest {


    public final static String STOP_KEY = "key";
    public final static int STOP_PORT = 6523;

    protected String[] args = new String[]{};

    public BaseIntegrationTest() {
        String propertiesPath = BaseIntegrationTest.class.getResource("/collector.properties").getPath();
        String logBackPath = getClass().getResource("/logback.xml").getPath();

        System.setProperty("collectorPropertiesPath", propertiesPath);
        System.setProperty("logback.configurationFile", logBackPath);
    }

    protected void startCollector() throws IOException {
        CollectorCoreMonitor monitor = new CollectorCoreMonitor(STOP_KEY, 6523, true, args);
        monitor.start();
    }

    protected void stopCollector() throws Exception {
        Socket socket = new Socket(InetAddress.getByName(CollectorCoreMonitor.MONITOR_HOST), STOP_PORT);
        IOUtils.write(STOP_KEY, socket.getOutputStream(), StandardCharsets.UTF_8);
        socket.close();
    }
}
