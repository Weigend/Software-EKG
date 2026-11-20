//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.source.log;

import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import io.softwareecg.collector.source.log.mock.ssh.SshServerMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Tests for the LogCounterSource.
 */
public class LogMetricDefinitionSourceTest {

    private static final String HOST_GROUP = "hostGroup";
    private static final String SSH_USERNAME = "user";
    private static final String SSH_PASSWORD = "pwd";
    private HostInfo hostInfo;


    /**
     * local ssh port for testing
     */
    private static final int SSH_PORT = 12023;

    private SshServerMock sshServerMock;

    private LogCounterSource logCounterSource;

    private final CompletableFuture<Boolean> authenticated = new CompletableFuture<>();

    @Before
    public void setUp() throws Exception {
        logCounterSource = new LogCounterSource();
        hostInfo = new HostInfo("testHost", InetAddress.getLocalHost().getHostAddress(), HOST_GROUP, SSH_USERNAME, SSH_PASSWORD);
    }

    @After
    public void tearDown() {
        logCounterSource.stop();
    }

    @After
    public void shutdown() throws InterruptedException, IOException {
        if (sshServerMock != null) {
            sshServerMock.stop();
        }
    }

    private void setUpSsh() {
        System.setProperty("collectorPropertiesPath", getClass().getResource("/collector.properties").getPath());
        SshTailLogger.setSshPort(SSH_PORT);
        sshServerMock = new SshServerMock(
                SSH_PORT,
                command -> "TestTestTestTest\nTestTestTestTest\nTestTestTestTest",
                (user, password) -> {
                    authenticated.complete(Boolean.TRUE);
                    return true;
                });
    }

    @Test
    public void testCounterSourceBasics() {
        assertEquals(logCounterSource.getId(), "log");
    }

    @Test
    public void testCounterSourceSsh() throws Exception {
        setUpSsh();
        sshServerMock.start();

        CollectorJobConfiguration config = TestConfigFactory.createTestConfig(
                hostInfo,
                new ProcessInfo("process", "label", "source", null),
                new MetricGroupInfo( "metricGroup", "type", 1000L, 1000L, null),
                new MetricDefinition[]{new MetricDefinition("\\w+", "label")}
        );

        assertTrue(logCounterSource.queryCounter(config).getValues().size() > 0);

        //wait max. 3 seconds for the log counter source to connect to the sshServer
        authenticated.get(3, TimeUnit.SECONDS);
        assertEquals(sshServerMock.getActiveSessionCount(), 1);

        assertTrue(logCounterSource.queryCounter(config).getValues().size() > 0);
        sshServerMock.stop();
    }

    @Test
    public void testShutdownHook() throws Exception {
        try {
            setUpSsh();
            sshServerMock.start();

            CollectorJobConfiguration config = TestConfigFactory.createTestConfig(
                    hostInfo,
                    new ProcessInfo("process", "label", "source", null),
                    new MetricGroupInfo( "metricGroup", "type", 1000L, 1000L, null),
                    new MetricDefinition[]{new MetricDefinition("\\w+", "label")}
            );

            logCounterSource.queryCounter(config);
        } finally {
            logCounterSource.shutdown();
        }
    }


    //@Test
    public void testCounterSourceLocalhost() throws Exception {

        CollectorJobConfiguration config = TestConfigFactory.createTestConfig(
                hostInfo,
                new ProcessInfo("process", "label", "source", null),
                new MetricGroupInfo( "metricGroup", "type", 1000L, 1000L, null),
                new MetricDefinition[]{}
        );

        logCounterSource.queryCounter(config);
    }

    @Test
    public void testInvalidCounterSources() {
        try {
            CollectorJobConfiguration config = getCounterGroup("testHost", "10.0.0.0.1",  "user", null, "source");
            logCounterSource.queryCounter(config);
            fail();
        } catch (IllegalStateException ise) {
            assertTrue(ise.getMessage().contains("password"));
        }

        try {
            CollectorJobConfiguration config = getCounterGroup("testHost", "10.0.0.0.1",  "user", "", "source");
            logCounterSource.queryCounter(config);
            fail();
        } catch (IllegalStateException ise) {
            assertTrue(ise.getMessage().contains("password"));
        }

        try {
            CollectorJobConfiguration config= getCounterGroup("testHost", "10.0.0.0.1",  null, "pwd", "source");
            logCounterSource.queryCounter(config);
            fail();
        } catch (IllegalStateException ise) {
            assertTrue(ise.getMessage().contains("user"));
        }

        try {
            CollectorJobConfiguration config = getCounterGroup("testHost", "10.0.0.0.1",  "", "pwd", "source");
            logCounterSource.queryCounter(config);
            fail();
        } catch (IllegalStateException ise) {
            assertTrue(ise.getMessage().contains("user"));
        }

        try {
            CollectorJobConfiguration config = getCounterGroup("testHost", null,  "user", "pwd", "source");
            logCounterSource.queryCounter(config);
            fail();
        } catch (IllegalStateException ise) {
            assertTrue(ise.getMessage().contains("address"));
        }

        try {
            CollectorJobConfiguration config = getCounterGroup(null, "hostIp",  "user", "pwd", "source");
            logCounterSource.queryCounter(config);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("host name"));
        }

        try {
            CollectorJobConfiguration config = getCounterGroup("hostname", "hostIp", "user", "pwd", null);
            logCounterSource.queryCounter(config);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("source"));
        }
    }

    private CollectorJobConfiguration getCounterGroup(String hostname, String hostIp, String user, String pwd, String source) {
        return TestConfigFactory.createTestConfig(
                        new HostInfo(hostname, hostIp, "hostGroup", user, pwd),
                        new ProcessInfo("process", "label", source, null),
                        new MetricGroupInfo("counterGroup", "type", 1000L, 1000L, null),
                        new MetricDefinition[]{}
                );
    }

}
