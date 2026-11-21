package io.softwareecg.collector.repository.csv;

import io.softwareecg.collector.dt.TimeSeriesHeader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Csv4jRepositoryTest {

    private static final long DAY = 86_400_000L;

    /** Saturday, 2026-11-21 12:00 UTC. */
    private static final long NOON = Instant.parse("2026-11-21T12:00:00Z").toEpochMilli();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // ---------------------------------------------------------------------------------------------
    // files by time
    // ---------------------------------------------------------------------------------------------

    @Test
    public void aDayIsAFile() throws IOException {
        Csv4jRepository repository = repository(daily("30MB", "0"), "logins");
        repository.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});
        repository.addValueSetToMeasurement(new Date(NOON + DAY / 2 - 1), new Double[]{2.0});   // 23:59:59.999
        repository.addValueSetToMeasurement(new Date(NOON + DAY / 2), new Double[]{3.0});       // midnight
        repository.addValueSetToMeasurement(new Date(NOON + DAY), new Double[]{4.0});

        assertEquals(List.of("job_2026-11-21.csv", "job_2026-11-22.csv"), files());
        assertEquals(List.of(NOON + ";1", (NOON + DAY / 2 - 1) + ";2"), rows("job_2026-11-21.csv"));
        assertEquals(List.of((NOON + DAY / 2) + ";3", (NOON + DAY) + ";4"), rows("job_2026-11-22.csv"));
        for (String file : files()) {
            assertTrue("every file begins with the header", lines(file).get(0).endsWith(";logins;"));
        }
    }

    @Test
    public void aRowGoesIntoTheFileOfItsOwnTime() throws IOException {
        Csv4jRepository repository = repository(daily("30MB", "0"), "logins");
        repository.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});
        repository.addValueSetToMeasurement(new Date(NOON + DAY), new Double[]{2.0});
        // what a source with a history of its own delivers of the day before, later
        repository.addValueSetToMeasurement(new Date(NOON + 1_000), new Double[]{3.0});

        assertEquals(List.of("job_2026-11-21.csv", "job_2026-11-22.csv"), files());
        assertEquals(List.of(NOON + ";1", (NOON + 1_000) + ";3"), rows("job_2026-11-21.csv"));
        assertEquals("the file is continued, not begun again", 3, lines("job_2026-11-21.csv").size());
    }

    @Test
    public void aWeekOrAMonthIsAFile() throws IOException {
        Csv4jRepository weekly = repository(CsvFileRolling.of("%d{YYYY-'W'ww}", "30MB", "0", ZoneOffset.UTC), "logins");
        weekly.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});                // Saturday
        weekly.addValueSetToMeasurement(new Date(NOON + DAY), new Double[]{2.0});          // Sunday
        weekly.addValueSetToMeasurement(new Date(NOON + 2 * DAY), new Double[]{3.0});      // Monday
        weekly.addValueSetToMeasurement(new Date(Instant.parse("2027-01-01T12:00:00Z").toEpochMilli()),
                new Double[]{4.0});                                                        // still the last week of 2026
        assertEquals(List.of("job_2026-W47.csv", "job_2026-W48.csv", "job_2026-W53.csv"), files());
        assertEquals(2, rows("job_2026-W47.csv").size());
        weekly.cleanUp();

        File months = folder.newFolder("months");
        Csv4jRepository monthly = new Csv4jRepository("measurement", new File(months, "job").getPath(),
                CsvFileRolling.of("yyyy-MM", "30MB", "0", ZoneOffset.UTC));
        monthly.createMeasurement(new TimeSeriesHeader(), new String[]{"logins"});
        monthly.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});
        monthly.addValueSetToMeasurement(new Date(NOON + 10 * DAY), new Double[]{2.0});
        assertEquals(List.of("job_2026-11.csv", "job_2026-12.csv"), Arrays.stream(months.list()).sorted().toList());
    }

    @Test
    public void aDayBeginsAtMidnightOfTheConfiguredZone() throws IOException {
        Csv4jRepository repository = repository(
                CsvFileRolling.of("%d{yyyy-MM-dd}", "30MB", "0", ZoneId.of("Europe/Berlin")), "logins");
        repository.addValueSetToMeasurement(new Date(Instant.parse("2026-11-21T22:59:59Z").toEpochMilli()), new Double[]{1.0});
        repository.addValueSetToMeasurement(new Date(Instant.parse("2026-11-21T23:00:00Z").toEpochMilli()), new Double[]{2.0});

        assertEquals(List.of("job_2026-11-21.csv", "job_2026-11-22.csv"), files());
    }

    @Test
    public void withoutAPeriodThereIsOneFile() throws IOException {
        for (String off : new String[]{"none", "off", "", null}) {
            File directory = folder.newFolder();
            Csv4jRepository repository = new Csv4jRepository("measurement", new File(directory, "job").getPath(),
                    CsvFileRolling.of(off, "30MB", "0", ZoneOffset.UTC));
            repository.createMeasurement(new TimeSeriesHeader(), new String[]{"logins"});
            repository.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});
            repository.addValueSetToMeasurement(new Date(NOON + 30 * DAY), new Double[]{2.0});

            assertEquals(List.of("job.csv"), Arrays.asList(directory.list()));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // files by size
    // ---------------------------------------------------------------------------------------------

    @Test
    public void whatDoesNotFitIntoAFileGoesIntoTheNextOfThePeriod() throws IOException {
        Csv4jRepository repository = repository(daily("2KB", "0"), "logins");
        for (int row = 0; row < 500; row++) {
            repository.addValueSetToMeasurement(new Date(NOON + row), new Double[]{(double) row});
            repository.flush();
        }

        List<String> files = files();
        assertTrue("several files: " + files, files.size() > 3);
        assertEquals("job_2026-11-21.csv", files.get(0));
        assertTrue(files.contains("job_2026-11-21_2.csv"));

        List<String> all = new ArrayList<>();
        for (int part = 1; part <= files.size(); part++) {
            String name = part == 1 ? "job_2026-11-21.csv" : "job_2026-11-21_" + part + ".csv";
            File file = new File(folder.getRoot(), name);
            assertTrue(name + " is not larger than configured: " + file.length(), file.length() <= 2_048);
            assertTrue("every file begins with the header", lines(name).get(0).endsWith(";logins;"));
            all.addAll(rows(name));
        }
        assertEquals("nothing is deleted, and the parts are in the order of their numbers", 500, all.size());
        assertEquals(NOON + ";0", all.get(0));
        assertEquals((NOON + 499) + ";499", all.get(499));
    }

    @Test
    public void onlySoManyFilesAreKeptIfANumberIsSet() throws IOException {
        Csv4jRepository repository = repository(daily("30MB", "2"), "logins");
        for (int day = 0; day < 5; day++) {
            repository.addValueSetToMeasurement(new Date(NOON + day * DAY), new Double[]{(double) day});
        }

        assertEquals("the file that is written and the two before it",
                List.of("job_2026-11-23.csv", "job_2026-11-24.csv", "job_2026-11-25.csv"), files());
    }

    // ---------------------------------------------------------------------------------------------
    // the life of a storage
    // ---------------------------------------------------------------------------------------------

    @Test
    public void rowsStayInOneFileHoweverOftenTheJobFlushes() throws IOException {
        Csv4jRepository repository = repository(daily("30MB", "0"), "logins", "errors");

        // a job that collects from a source with a history of its own flushes after every cycle
        for (int cycle = 0; cycle < 10; cycle++) {
            repository.addValueSetToMeasurement(new Date(NOON + 1_000L * cycle), new Double[]{(double) cycle, 0.0});
            repository.flush();
        }

        assertEquals(List.of("job_2026-11-21.csv"), files());
        List<String> rows = rows("job_2026-11-21.csv");
        assertEquals(10, rows.size());
        assertEquals(NOON + ";0;0", rows.get(0));
        assertEquals((NOON + 9_000) + ";9;0", rows.get(9));
    }

    @Test
    public void aStorageThatIsGivenUpLeavesItsFileAsItIs() throws IOException {
        Csv4jRepository repository = repository(daily("30MB", "0"), "logins");
        repository.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});

        repository.flush();
        repository.cleanUp();

        assertEquals("not renamed: the import of the workbench looks for files that end with .csv",
                List.of("job_2026-11-21.csv"), files());
        assertEquals(List.of(NOON + ";1"), rows("job_2026-11-21.csv"));
    }

    @Test
    public void aStorageWithTheSameSeriesContinuesTheFile() throws IOException {
        Csv4jRepository first = repository(daily("30MB", "0"), "logins");
        first.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});
        first.cleanUp();

        Csv4jRepository second = repository(daily("30MB", "0"), "logins");
        second.addValueSetToMeasurement(new Date(NOON + 1_000), new Double[]{2.0});

        assertEquals(List.of("job_2026-11-21.csv"), files());
        assertEquals("one header, then the rows of both", 3, lines("job_2026-11-21.csv").size());
    }

    @Test
    public void aStorageWithOtherSeriesBeginsAFileOfItsOwn() throws IOException {
        Csv4jRepository first = repository(daily("30MB", "0"), "logins");
        first.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0});
        first.flush();
        first.cleanUp();

        // the series of the job changed: the rows have another column now
        Csv4jRepository second = repository(daily("30MB", "0"), "logins", "errors");
        second.addValueSetToMeasurement(new Date(NOON + 1_000), new Double[]{2.0, 7.0});

        assertEquals(List.of("job_2026-11-21.csv", "job_2026-11-21_2.csv"), files());
        assertTrue(lines("job_2026-11-21.csv").get(0).endsWith(";logins;"));
        assertEquals(List.of(NOON + ";1"), rows("job_2026-11-21.csv"));
        assertTrue(lines("job_2026-11-21_2.csv").get(0).endsWith(";logins;errors;"));
        assertEquals(List.of((NOON + 1_000) + ";2;7"), rows("job_2026-11-21_2.csv"));
    }

    @Test
    public void aValueThatIsMissingLeavesItsColumnEmpty() throws IOException {
        Csv4jRepository repository = repository(daily("30MB", "0"), "a", "b", "c");

        repository.addValueSetToMeasurement(new Date(NOON), new Double[]{1.0, null, 3.0});
        repository.addValueSetToMeasurement(new Date(NOON + 1), new Double[]{null, 2.0, null});
        repository.addValueSetToMeasurement(new Date(NOON + 2), new Double[]{null, null, null});

        List<String> rows = rows("job_2026-11-21.csv");
        assertEquals("the value of c stays under c", NOON + ";1;;3", rows.get(0));
        assertEquals((NOON + 1) + ";;2;", rows.get(1));
        assertEquals((NOON + 2) + ";;;", rows.get(2));
    }

    @Test
    public void aStorageWithoutRowsWritesNoFile() {
        repository(daily("30MB", "0"), "logins").cleanUp();

        assertEquals(List.of(), files());
    }

    // ---------------------------------------------------------------------------------------------
    // settings
    // ---------------------------------------------------------------------------------------------

    @Test
    public void withoutSettingsADayIsAFileOfThirtyMegabytesAtMostAndNothingIsDeleted() {
        CsvFileRolling rolling = CsvFileRolling.configured();

        assertEquals("yyyy-MM-dd", rolling.periodPattern());
        assertEquals(30L << 20, rolling.maxFileBytes());
        assertEquals(0, rolling.maxClosedFiles());
        assertEquals(ZoneId.systemDefault(), rolling.zone());
    }

    @Test
    public void sizesHaveAUnit() {
        assertEquals(500, CsvFileRolling.of("none", "500", "0", ZoneOffset.UTC).maxFileBytes());
        assertEquals(500L << 10, CsvFileRolling.of("none", "500KB", "0", ZoneOffset.UTC).maxFileBytes());
        assertEquals(2L << 30, CsvFileRolling.of("none", " 2 gb ", "0", ZoneOffset.UTC).maxFileBytes());
        assertNull(CsvFileRolling.of("none", "1MB", "0", ZoneOffset.UTC).periodPattern());
        assertEquals("a number below 0 keeps all as well", 0, CsvFileRolling.of("none", "1MB", "-1", ZoneOffset.UTC).maxClosedFiles());
    }

    @Test
    public void settingsThatAreNotUsableAreSaidSo() {
        assertRejected("%d{yyyy-MM-dd'T'HH:mm}", "30MB", "0", "cannot be part of the name of a file");
        assertRejected("%d{yyyy/MM/dd}", "30MB", "0", "cannot be part of the name of a file");
        assertRejected("%d{daily}", "30MB", "0", "not a pattern for dates");
        assertRejected("%d{yyyy-MM-dd}", "30 megabytes", "0", "write a size like 30MB");
        assertRejected("%d{yyyy-MM-dd}", "0", "0", "write a size like 30MB");
        assertRejected("%d{yyyy-MM-dd}", "30MB", "three", "the number of files to keep");
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private static void assertRejected(String pattern, String size, String keep, String expected) {
        try {
            CsvFileRolling.of(pattern, size, keep, ZoneOffset.UTC);
            fail("must be rejected: " + pattern + ", " + size + ", " + keep);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
        }
    }

    private static CsvFileRolling daily(String maxFileSize, String keep) {
        return CsvFileRolling.of("%d{yyyy-MM-dd}", maxFileSize, keep, ZoneOffset.UTC);
    }

    private Csv4jRepository repository(CsvFileRolling rolling, String... labels) {
        Csv4jRepository repository = new Csv4jRepository("measurement", new File(folder.getRoot(), "job").getPath(), rolling);
        repository.createMeasurement(new TimeSeriesHeader(), labels);
        return repository;
    }

    private List<String> files() {
        String[] names = folder.getRoot().list((directory, name) -> name.endsWith(".csv"));
        return names == null ? List.of() : Arrays.stream(names).sorted().toList();
    }

    private List<String> lines(String file) throws IOException {
        return Files.readAllLines(new File(folder.getRoot(), file).toPath());
    }

    /** The lines of a file without its header. */
    private List<String> rows(String file) throws IOException {
        List<String> lines = lines(file);
        return lines.subList(1, lines.size());
    }
}
