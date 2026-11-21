package io.softwareecg.collector.repository.archive;

import io.softwareecg.collector.dt.TimeSeriesHeader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArchiveRepositoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * What the task of every midnight does to a file: it moves it into the archive folder of the
     * day and begins a new one.
     */
    @Test
    public void aRotatedFileGoesToTheArchiveFolderAndANewOneBegins() throws IOException {
        File current = folder.newFolder("ekg", "current");
        ArchiveRepository repository = new ArchiveRepository(new File(current, "host_process_group").getPath());
        repository.createMeasurement(new TimeSeriesHeader(), new String[]{"logins", "errors"});
        repository.addValueSetToMeasurement(new Date(1_000), new Double[]{1.0, null});
        repository.addValueSetToMeasurement(new Date(2_000), new Double[]{2.0, 5.0});
        repository.flush();   // as a job does after every cycle: nothing is rotated by it

        File archive = new File(folder.getRoot(), "ekg/archiv");
        assertTrue(archive.isDirectory());
        assertEquals("nothing is archived before the rotation", 0, filesBelow(archive));

        repository.rotate();

        File[] days = archive.listFiles(File::isDirectory);
        assertNotNull(days);
        assertEquals("one folder, named by the archive directory pattern", 1, days.length);
        File[] archived = days[0].listFiles();
        assertNotNull(archived);
        assertEquals(1, archived.length);
        assertTrue("the file carries the time of its rotation: " + archived[0].getName(),
                archived[0].getName().matches("host_process_group_\\d{4}\\.\\d{2}\\.\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.csv\\.gz"));

        List<String> rows = read(archived[0]);
        assertEquals(3, rows.size());
        assertEquals("Date;logins;errors;", rows.get(0));
        assertTrue(rows.get(1), rows.get(1).endsWith(";1;"));
        assertTrue(rows.get(2), rows.get(2).endsWith(";2;5"));

        // what follows goes into a new file under the old name, with the header again
        repository.addValueSetToMeasurement(new Date(3_000), new Double[]{3.0, 0.0});
        repository.rotate();
        File[] both = days[0].listFiles();
        assertNotNull(both);
        assertTrue(both.length >= 1);
        assertTrue(new File(current, "host_process_group.csv.gz").exists());
    }

    private static int filesBelow(File directory) {
        int count = 0;
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                count += child.isDirectory() ? filesBelow(child) : 1;
            }
        }
        return count;
    }

    private static List<String> read(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        }
    }
}
