package io.softwareecg.collector.source.log;

import io.softwareecg.collector.source.CounterSeries;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LogFollowerTest {

    /** 2026-11-21 10:00:00 UTC, on the grid of whole seconds. */
    private static final long T0 = Instant.parse("2026-11-21T10:00:00Z").toEpochMilli();

    private static final long SECOND = 1_000;

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(ZoneOffset.UTC);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path file;

    private long now = T0;

    private final List<CounterSeries> delivered = new ArrayList<>();

    @Before
    public void createFile() throws IOException {
        file = folder.newFile("application.log").toPath();
    }

    // ---------------------------------------------------------------------------------------------
    // following what is written from now on
    // ---------------------------------------------------------------------------------------------

    @Test
    public void linesAreCountedInTheIntervalOfTheirTimestamp() throws IOException {
        LogFollower follower = follower(0, "login ok", "ERROR");
        write(line(T0 - 5_000, "INFO login ok"));              // was there before: not followed
        now = T0 + 500;
        assertTrue(poll(follower, null));
        assertEquals("the interval of the start is not over yet", List.of(), delivered);

        write(line(T0 + 600, "INFO login ok"),
              line(T0 + 1_200, "INFO login ok"),
              line(T0 + 1_300, "ERROR out of stock"),
              line(T0 + 1_900, "INFO login ok"));
        now = T0 + 2_000;
        assertTrue(poll(follower, null));

        assertEquals(2, delivered.size());
        assertSet(T0, 1.0, 0.0);
        assertSet(T0 + SECOND, 2.0, 1.0);
    }

    @Test
    public void anIntervalInWhichNothingHappenedSaysSo() throws IOException {
        LogFollower follower = follower(0, "login ok", "took (?<max>\\d+) ms");
        now = T0;
        poll(follower, null);

        now = T0 + 4_000;
        assertTrue(poll(follower, null));

        assertEquals(4, delivered.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(new Date(T0 + i * SECOND), delivered.get(i).getDate());
            assertEquals("a count of 0, and no value for an aggregate without a line",
                    Arrays.asList(0.0, null), delivered.get(i).getValues());
        }
    }

    @Test
    public void aggregatesOfTheNumbersInTheLines() throws IOException {
        LogFollower follower = follower(0, "took (?<sum>\\d+) ms", "took (?<avg>\\d+) ms", "took (?<min>\\d+) ms",
                "took (?<max>\\d+) ms", "took (?<last>\\d+) ms", "took");
        now = T0;
        poll(follower, null);

        write(line(T0 + 100, "request took 40 ms"),
              line(T0 + 200, "request took 10 ms"),
              line(T0 + 300, "request took 70 ms"),
              line(T0 + 400, "request took ages"));
        now = T0 + 2_000;
        poll(follower, null);

        assertSet(T0, 120.0, 40.0, 10.0, 70.0, 70.0, 4.0);
        assertEquals(Arrays.asList(0.0, null, null, null, null, 0.0), delivered.get(1).getValues());
    }

    @Test
    public void furtherLinesOfAnEventBelongWhereItsFirstLineDoes() throws IOException {
        LogFollower follower = follower(0, "IllegalStateException", "at com\\.example");
        now = T0;
        poll(follower, null);

        write(line(T0 + 900, "ERROR payment failed"),
              "java.lang.IllegalStateException: no stock",
              "\tat com.example.Checkout.pay(Checkout.java:42)",
              "\tat com.example.Checkout.run(Checkout.java:17)");
        now = T0 + 3_000;   // read long after the event
        poll(follower, null);

        assertSet(T0, 1.0, 2.0);
        assertSet(T0 + SECOND, 0.0, 0.0);
    }

    @Test
    public void linesWithoutAnyTimestampCountWhenTheyAreRead() throws IOException {
        LogFollower follower = follower(0, "login ok");
        now = T0 + 100;
        poll(follower, null);

        write("login ok", "login ok");
        now = T0 + 1_500;
        poll(follower, null);
        assertEquals(1, delivered.size());
        assertSet(T0, 0.0);

        now = T0 + 2_100;
        poll(follower, null);
        assertSet(T0 + SECOND, 2.0);
    }

    @Test
    public void aLineIsEvaluatedWhenItIsComplete() throws IOException {
        LogFollower follower = follower(0, "login ok");
        now = T0;
        poll(follower, null);

        append(STAMP.format(Instant.ofEpochMilli(T0 + 100)) + " INFO login");
        now = T0 + 1_000;
        poll(follower, null);
        assertSet(T0, 0.0);

        append(" ok\n");
        now = T0 + 2_000;
        poll(follower, null);
        assertSet("counted once, in the interval after the one that was delivered without it", T0 + SECOND, 1.0);
    }

    @Test
    public void linesWrittenOnWindowsEndWithACarriageReturn() throws IOException {
        LogFollower follower = follower(0, "login ok$");
        now = T0;
        poll(follower, null);

        append(STAMP.format(Instant.ofEpochMilli(T0 + 100)) + " INFO login ok\r\n\r\n");
        now = T0 + 1_000;
        poll(follower, null);

        assertSet(T0, 1.0);
    }

    @Test
    public void aLineOfAnIntervalThatWasDeliveredAlreadyCountsInTheNext() throws IOException {
        LogFollower follower = follower(0, "login ok");
        now = T0;
        poll(follower, null);
        now = T0 + 2_000;
        poll(follower, null);
        assertEquals(2, delivered.size());

        write(line(T0 + 900, "INFO login ok"));   // written late by the application
        now = T0 + 3_000;
        poll(follower, null);

        assertSet("not lost", T0 + 2 * SECOND, 1.0);
    }

    @Test
    public void timestampsFromTheFutureAreNotBelieved() throws IOException {
        LogFollower follower = follower(0, "login ok");
        now = T0;
        poll(follower, null);

        write(line(T0 + 3_600_500, "INFO login ok"));   // a clock or a time zone that is an hour off
        now = T0 + 1_000;
        poll(follower, null);
        now = T0 + 2_000;
        poll(follower, null);

        assertSet(T0, 0.0);
        assertSet("counted when it was read", T0 + SECOND, 1.0);
    }

    // ---------------------------------------------------------------------------------------------
    // the file
    // ---------------------------------------------------------------------------------------------

    @Test
    public void aFileThatIsNotThereIsSaidSoAndFollowedOnceItIs() throws IOException {
        Files.delete(file);
        LogFollower follower = follower(0, "login ok");

        assertTrue(follower.check(), follower.check().contains("does not exist or may not be read"));
        now = T0;
        assertFalse(poll(follower, null));
        now = T0 + 60_000;
        assertFalse(poll(follower, null));

        Files.createFile(file);
        assertNull(follower.check());
        now = T0 + 61_000;
        assertTrue(poll(follower, null));
        write(line(T0 + 61_500, "INFO login ok"));
        now = T0 + 62_000;
        assertTrue(poll(follower, null));

        assertEquals("nothing is known about the minute before, so nothing is said about it",
                List.of(new Date(T0 + 61_000)), times());
        assertSet(T0 + 61_000, 1.0);
    }

    @Test
    public void aFileThatWasReplacedIsReadFromItsBeginning() throws IOException {
        LogFollower follower = follower(0, "login ok");
        write(line(T0 - 10_000, "INFO login ok"), line(T0 - 9_000, "INFO login ok"));
        now = T0;
        poll(follower, null);
        write(line(T0 + 100, "INFO login ok"));
        now = T0 + 1_000;
        poll(follower, null);
        assertSet(T0, 1.0);

        // rotated: the file gets another name and a new one begins
        Files.move(file, file.resolveSibling("application.log.1"));
        Files.createFile(file);
        write(line(T0 + 1_100, "INFO login ok"), line(T0 + 1_200, "INFO login ok"), line(T0 + 1_300, "INFO login ok"));
        now = T0 + 2_000;
        assertTrue(poll(follower, null));

        assertSet(T0 + SECOND, 3.0);
    }

    @Test
    public void aFileThatWasEmptiedIsReadFromItsBeginning() throws IOException {
        LogFollower follower = follower(0, "login ok");
        write(line(T0 - 10_000, "INFO login ok and a long text so that the file is longer than what follows"));
        now = T0;
        poll(follower, null);

        Files.writeString(file, line(T0 + 100, "INFO login ok") + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING);
        now = T0 + 1_000;
        poll(follower, null);

        assertSet(T0, 1.0);
    }

    @Test
    public void aLongFileIsReadInParts() throws IOException {
        int lines = 30_000;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            // one line every 10 ms, the last one just before T0
            text.append(line(T0 - (lines - i) * 10L, "INFO login ok for the user number " + i)).append('\n');
        }
        append(text.toString());
        assertTrue("the file has to be longer than one part", Files.size(file) > LogFollower.MAX_PART_BYTES);

        LogFollower follower = follower(3_600_000, "login ok");
        now = T0;
        assertTrue(poll(follower, null));

        assertEquals(lines / 100, delivered.size());
        double counted = 0;
        for (CounterSeries values : delivered) {
            assertEquals(Double.valueOf(100), values.getValues().get(0));
            counted += values.getValues().get(0);
        }
        assertEquals(lines, counted, 0);
    }

    // ---------------------------------------------------------------------------------------------
    // what the file already has
    // ---------------------------------------------------------------------------------------------

    @Test
    public void withALookbackTheContentOfTheFileIsReadAtTheTimeItHappened() throws IOException {
        write(line(T0 - 3_600_000, "INFO login ok"),      // older than the lookback
              line(T0 - 30_500, "INFO login ok"),
              line(T0 - 30_400, "INFO login ok"),
              line(T0 - 10_000, "INFO login ok"));
        LogFollower follower = follower(60_000, "login ok");
        now = T0;
        assertTrue(poll(follower, null));

        assertEquals("the last minute, second by second", 60, delivered.size());
        assertEquals(new Date(T0 - 60_000), delivered.get(0).getDate());
        assertEquals(new Date(T0 - SECOND), delivered.get(59).getDate());
        assertSet(T0 - 31_000, 2.0);
        assertSet(T0 - 10_000, 1.0);
        assertEquals(3.0, delivered.stream().mapToDouble(v -> v.getValues().get(0)).sum(), 0);

        // and from then on what is written
        write(line(T0 + 200, "INFO login ok"));
        now = T0 + 1_000;
        poll(follower, null);
        assertSet(T0, 1.0);
    }

    @Test
    public void nothingIsSaidAboutTheTimeBeforeTheFileBegins() throws IOException {
        write(line(T0 - 20_300, "INFO started"),
              line(T0 - 10_000, "INFO login ok"));
        LogFollower follower = follower(3_600_000, "login ok");
        now = T0;
        poll(follower, null);

        assertEquals("from the first line of the file on, not from an hour ago", 21, delivered.size());
        assertEquals(new Date(T0 - 21_000), delivered.get(0).getDate());
        assertSet(T0 - 10_000, 1.0);
    }

    @Test
    public void whatTheStorageAlreadyHoldsIsNotDeliveredAgain() throws IOException {
        write(line(T0 - 50_000, "INFO login ok"),
              line(T0 - 20_000, "INFO login ok"),
              line(T0 - 19_500, "INFO login ok"),
              line(T0 - 5_000, "INFO login ok"));
        LogFollower follower = follower(60_000, "login ok");
        now = T0;
        poll(follower, new Date(T0 - 20_000));

        assertEquals(19, delivered.size());
        assertEquals(new Date(T0 - 19_000), delivered.get(0).getDate());
        assertEquals("the lines of the interval the storage ends with are in it already",
                1.0, delivered.stream().mapToDouble(v -> v.getValues().get(0)).sum(), 0);
    }

    @Test
    public void oldContentWithoutTimestampsCannotBePlaced() throws IOException {
        write("login ok", "login ok", "login ok");
        LogFollower follower = follower(60_000, "login ok");
        now = T0 + 100;
        assertTrue(poll(follower, null));
        assertEquals(List.of(), delivered);

        write("login ok");
        now = T0 + 1_500;
        poll(follower, null);
        now = T0 + 2_000;
        poll(follower, null);

        assertSet(T0, 0.0);
        assertSet("what comes now counts when it is read", T0 + SECOND, 1.0);
    }

    @Test
    public void linesSlightlyOutOfOrderAreNotLostWhileALongFileIsRead() throws IOException {
        int lines = 30_000;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            // every tenth line is written 30 ms late, as threads do
            long time = T0 - (lines - i) * 10L - (i % 10 == 0 ? 30 : 0);
            text.append(line(time, "INFO login ok for the user number " + i)).append('\n');
        }
        append(text.toString());

        LogFollower follower = follower(3_600_000, "login ok");
        now = T0;
        poll(follower, null);

        assertEquals(lines, delivered.stream().mapToDouble(v -> v.getValues().get(0)).sum(), 0);
    }

    // ---------------------------------------------------------------------------------------------
    // the storage
    // ---------------------------------------------------------------------------------------------

    @Test
    public void aStorageThatFailsGetsTheIntervalAgain() throws IOException {
        LogFollower follower = follower(0, "login ok");
        now = T0;
        poll(follower, null);
        write(line(T0 + 100, "INFO login ok"), line(T0 + 1_100, "INFO login ok"), line(T0 + 2_100, "INFO login ok"));

        now = T0 + 3_000;
        try {
            follower.poll(now, SECOND, null, values -> {
                if (values.getDate().getTime() == T0 + SECOND) {
                    throw new IllegalStateException("repository down");
                }
                delivered.add(values);
            });
            fail();
        } catch (IllegalStateException expected) {
            assertEquals(1, delivered.size());
        }

        now = T0 + 3_500;
        poll(follower, null);

        assertEquals(List.of(new Date(T0), new Date(T0 + SECOND), new Date(T0 + 2 * SECOND)), times());
        assertEquals(3.0, delivered.stream().mapToDouble(v -> v.getValues().get(0)).sum(), 0);
    }

    @Test
    public void aStorageThatLostValuesGetsThemFromTheFileAgain() throws IOException {
        LogFollower follower = follower(0, "login ok");
        write(line(T0 - 5_000, "INFO login ok"));
        now = T0;
        poll(follower, null);
        write(line(T0 + 100, "INFO login ok"), line(T0 + 1_100, "INFO login ok"), line(T0 + 2_100, "INFO login ok"));
        now = T0 + 3_000;
        poll(follower, null);
        assertEquals(3, delivered.size());
        delivered.clear();

        // the storage says it ends with the first interval: the two after it never arrived there
        now = T0 + 3_200;
        poll(follower, new Date(T0));

        assertEquals(List.of(new Date(T0 + SECOND), new Date(T0 + 2 * SECOND)), times());
        assertSet(T0 + SECOND, 1.0);
        assertSet(T0 + 2 * SECOND, 1.0);
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private LogFollower follower(long lookbackMillis, String... expressions) {
        List<LogMetric> metrics = new ArrayList<>();
        for (String expression : expressions) {
            metrics.add(LogMetric.of(expression));
        }
        return new LogFollower(new LocalLogFileReader(file, StandardCharsets.UTF_8), metrics,
                new LineTimestamps(null, ZoneOffset.UTC, () -> now), lookbackMillis);
    }

    private boolean poll(LogFollower follower, Date since) {
        return follower.poll(now, SECOND, since, delivered::add);
    }

    private static String line(long time, String message) {
        return STAMP.format(Instant.ofEpochMilli(time)) + " " + message;
    }

    private void write(String... lines) throws IOException {
        append(String.join("\n", lines) + "\n");
    }

    private void append(String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private List<Date> times() {
        return delivered.stream().map(CounterSeries::getDate).toList();
    }

    private void assertSet(long time, Double... values) {
        assertSet("", time, values);
    }

    private void assertSet(String message, long time, Double... values) {
        for (CounterSeries series : delivered) {
            if (series.getDate().getTime() == time) {
                assertEquals(message, Arrays.asList(values), series.getValues());
                return;
            }
        }
        fail(message + ": no values for " + new Date(time) + "; delivered: " + times());
    }
}
