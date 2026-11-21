package io.softwareecg.collector.source.log;

import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.CollectorJobConfiguration;
import io.softwareecg.collector.dt.HostInfo;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.dt.MetricGroupInfo;
import io.softwareecg.collector.dt.ProcessInfo;
import io.softwareecg.collector.source.CounterSeries;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.ExpandWildcardResultType;
import io.softwareecg.collector.source.log.mock.ssh.SshServerMock;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LogCounterSourceTest {

    private static final long T0 = Instant.parse("2026-11-21T10:00:00Z").toEpochMilli();

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC);

    private static final int SSH_PORT = 12024;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path file;

    private long now = T0;

    private final List<CounterSeries> delivered = new ArrayList<>();

    private SshServerMock sshServer;

    @Before
    public void createFile() throws IOException {
        file = folder.newFile("checkout-testHost.log").toPath();
        LogCounterSource.forget();
    }

    @After
    public void stop() throws Exception {
        LogCounterSource.forget();
        if (sshServer != null) {
            sshServer.stop();
        }
    }

    @Test
    public void theSourceIsNamedLog() {
        assertEquals("log", new LogCounterSource().getId());
    }

    @Test
    public void aFileOfThisMachineIsFollowed() throws IOException {
        CollectorJobConfiguration job = localJob(file.toString(), "lookback=1m", "login succeeded", "took (?<max>\\d+) ms");
        write(line(T0 - 30_000, "user login succeeded"), line(T0 - 29_900, "request took 250 ms"));
        LogCounterSource source = new LogCounterSource(() -> now);

        ExpandWildcardResponse response = source.replaceWildcardCounter(job);
        assertEquals(ExpandWildcardResultType.SUCCESSFUL_EXPANDED, response.getResultType());
        assertArrayEquals("a metric is one series", job.getMetricDefinitions(), response.getExpandedDefinitions());

        assertTrue(source.queryCounterSince(job, null, delivered::add));
        assertEquals("from the first line of the file to now", 30, delivered.size());
        assertEquals(new Date(T0 - 30_000), delivered.get(0).getDate());
        assertEquals(Arrays.asList(1.0, 250.0), delivered.get(0).getValues());
        assertEquals(Arrays.asList(0.0, null), delivered.get(1).getValues());

        write(line(T0 + 400, "user login succeeded"), line(T0 + 500, "user login succeeded"));
        now = T0 + 1_000;
        assertTrue(source.queryCounterSince(job, delivered.get(29).getDate(), delivered::add));
        assertEquals(31, delivered.size());
        assertEquals(Arrays.asList(2.0, null), delivered.get(30).getValues());
    }

    @Test
    public void whereTheFileWasReadSurvivesANewSourceObject() throws IOException {
        CollectorJobConfiguration job = localJob(file.toString(), null, "login succeeded");
        assertTrue(new LogCounterSource(() -> now).queryCounterSince(job, null, delivered::add));

        // a job takes a new source object after a failed reading; the lines in between are not lost
        write(line(T0 + 300, "user login succeeded"));
        now = T0 + 1_000;
        LogCounterSource next = new LogCounterSource(() -> now);
        assertEquals(ExpandWildcardResultType.SUCCESSFUL_EXPANDED, next.replaceWildcardCounter(job).getResultType());
        assertTrue(next.queryCounterSince(job, null, delivered::add));

        assertEquals(1, delivered.size());
        assertEquals(List.of(1.0), delivered.get(0).getValues());
    }

    @Test
    public void anotherConfigurationIsAnotherFile() throws IOException {
        write(line(T0 - 5_000, "user login succeeded"));
        assertTrue(new LogCounterSource(() -> now).queryCounterSince(
                localJob(file.toString(), null, "login succeeded"), null, delivered::add));
        assertEquals("only what is written from now on", List.of(), delivered);

        assertTrue(new LogCounterSource(() -> now).queryCounterSince(
                localJob(file.toString(), "lookback=1m", "login succeeded"), null, delivered::add));
        assertEquals("with a lookback the file is read from its beginning", 5, delivered.size());
    }

    @Test
    public void theNameOfTheHostCanBePartOfTheNameOfTheFile() throws IOException {
        String name = file.toString().replace("testHost", "${HOSTNAME}");
        write(line(T0 - 2_000, "user login succeeded"));

        assertTrue(new LogCounterSource(() -> now).queryCounterSince(
                localJob(name, "lookback=1m", "login succeeded"), null, delivered::add));

        assertEquals(2, delivered.size());
    }

    @Test
    public void aFileThatCannotBeReadIsSaidSoUntilItCan() throws IOException {
        Files.delete(file);
        CollectorJobConfiguration job = localJob(file.toString(), null, "login succeeded");
        LogCounterSource source = new LogCounterSource(() -> now);

        ExpandWildcardResponse response = source.replaceWildcardCounter(job);
        assertEquals(ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE, response.getResultType());
        assertTrue(response.getMessage(), response.getMessage().contains(file.toString()));
        assertTrue(response.getMessage(), response.getMessage().contains("does not exist or may not be read"));
        assertFalse(source.queryCounterSince(job, null, delivered::add));

        Files.createFile(file);
        assertEquals(ExpandWildcardResultType.SUCCESSFUL_EXPANDED, source.replaceWildcardCounter(job).getResultType());
        assertTrue(source.queryCounterSince(job, null, delivered::add));
    }

    @Test
    public void theCurrentValuesAreThoseOfTheNewestInterval() throws IOException {
        CollectorJobConfiguration job = localJob(file.toString(), null, "login succeeded");
        LogCounterSource source = new LogCounterSource(() -> now);
        assertEquals("no interval is over yet", List.of(), source.queryCounter(job).getValues());

        write(line(T0 + 1_200, "user login succeeded"));
        now = T0 + 2_000;
        CounterSeries newest = source.queryCounter(job);

        assertEquals(new Date(T0 + 1_000), newest.getDate());
        assertEquals(List.of(1.0), newest.getValues());
    }

    @Test
    public void whatIsWrongInTheConfigurationIsSaid() {
        assertRejected(localJob(file.toString(), null, "login (succeeded"), IllegalArgumentException.class,
                "not a regular expression");
        assertRejected(localJob(file.toString(), "lookbak=1h", "login"), IllegalArgumentException.class,
                "no argument 'lookbak'");
        assertRejected(localJob(file.toString(), "timestamp=HH:mm", "login"), IllegalArgumentException.class,
                "does not describe a date with its time");
        assertRejected(localJob(null, null, "login"), IllegalStateException.class, "as its 'source'");
        assertRejected(job("db-01", "10.0.0.1", null, "pwd", file.toString(), null, "login"),
                IllegalStateException.class, "needs a user");
        assertRejected(job("db-01", "10.0.0.1", "ekg", "", file.toString(), null, "login"),
                IllegalStateException.class, "needs a password");
    }

    @Test
    public void aFileOfAnotherMachineIsFollowedOverSsh() throws Exception {
        Assume.assumeTrue("needs a shell", new File("/bin/sh").canExecute());
        String address = InetAddress.getLocalHost().getHostAddress();
        Assume.assumeFalse("this machine is only known as the local one", HostNameUtil.isLocalhost(address));

        AtomicInteger commands = new AtomicInteger();
        sshServer = new SshServerMock(SSH_PORT,
                command -> {
                    commands.incrementAndGet();
                    try {
                        return SshLogFileReaderTest.run(command);
                    } catch (IOException | InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                },
                (user, password) -> "ekg".equals(user) && "secret".equals(password));
        sshServer.start();

        CollectorJobConfiguration job = job("testHost", address, "ekg", "secret", file.toString(),
                "lookback=1m; port=" + SSH_PORT, "login succeeded", "took (?<max>\\d+) ms");
        write(line(T0 - 10_000, "user login succeeded"), line(T0 - 9_900, "request took 250 ms"));
        LogCounterSource source = new LogCounterSource(() -> now);

        ExpandWildcardResponse response = source.replaceWildcardCounter(job);
        assertEquals(response.getMessage(), ExpandWildcardResultType.SUCCESSFUL_EXPANDED, response.getResultType());
        assertTrue(source.queryCounterSince(job, null, delivered::add));
        assertTrue("the file was read through the server", commands.get() >= 2);
        assertEquals(10, delivered.size());
        assertEquals(Arrays.asList(1.0, 250.0), delivered.get(0).getValues());

        write(line(T0 + 100, "user login succeeded"), line(T0 + 200, "request took 90 ms"));
        Files.writeString(file, STAMP.format(Instant.ofEpochMilli(T0 + 300)) + " user login succ",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        now = T0 + 1_000;
        assertTrue(source.queryCounterSince(job, null, delivered::add));
        assertEquals("the line that is not complete yet does not count", Arrays.asList(1.0, 90.0),
                delivered.get(10).getValues());

        Files.writeString(file, "eeded\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        now = T0 + 2_000;
        assertTrue(source.queryCounterSince(job, null, delivered::add));
        assertEquals(Arrays.asList(1.0, null), delivered.get(11).getValues());

        // a file that is gone, and a server that is
        Files.delete(file);
        assertEquals(ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE, source.replaceWildcardCounter(job).getResultType());
        sshServer.stop();
        sshServer = null;
        ExpandWildcardResponse unreachable = source.replaceWildcardCounter(job);
        assertEquals(ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE, unreachable.getResultType());
        assertTrue(unreachable.getMessage(), unreachable.getMessage().contains("cannot be read over SSH"));
        assertFalse(source.queryCounterSince(job, null, delivered::add));
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private void assertRejected(CollectorJobConfiguration job, Class<? extends RuntimeException> kind, String expected) {
        try {
            new LogCounterSource(() -> now).replaceWildcardCounter(job);
            fail("must be rejected");
        } catch (RuntimeException e) {
            assertEquals(kind, e.getClass());
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
        }
    }

    private CollectorJobConfiguration localJob(String source, String arguments, String... expressions) {
        return job("testHost", "127.0.0.1", null, null, source, arguments, expressions);
    }

    private static CollectorJobConfiguration job(String hostName, String address, String user, String password,
                                                 String source, String arguments, String... expressions) {
        MetricDefinition[] metrics = new MetricDefinition[expressions.length];
        for (int i = 0; i < metrics.length; i++) {
            metrics[i] = new MetricDefinition(expressions[i], null);
        }
        return TestConfigFactory.createTestConfig(
                new HostInfo(hostName, address, "hostGroup", user, password),
                new ProcessInfo("checkout", "Checkout", source, arguments),
                new MetricGroupInfo("checkout-log", "log", null, 1000L, null),
                metrics);
    }

    private static String line(long time, String message) {
        return STAMP.format(Instant.ofEpochMilli(time)) + " " + message;
    }

    private void write(String... lines) throws IOException {
        Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }
}
