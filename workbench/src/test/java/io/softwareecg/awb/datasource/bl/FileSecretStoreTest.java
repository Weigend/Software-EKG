package io.softwareecg.awb.datasource.bl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class FileSecretStoreTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void storesReadsReplacesAndDeletes() throws Exception {
        FileSecretStore store = new FileSecretStore(folder.getRoot().toPath().resolve("secrets"));

        assertNull(store.read("source-1"));
        store.store("source-1", "päss wörd\n");
        assertEquals("päss wörd\n", store.read("source-1"));

        store.store("source-1", "replaced");
        assertEquals("replaced", store.read("source-1"));

        store.delete("source-1");
        assertNull(store.read("source-1"));
        store.delete("source-1");   // deleting what is not there is fine
    }

    @Test
    public void secretsAreReadableByTheOwnerOnly() throws Exception {
        Path directory = folder.getRoot().toPath().resolve("secrets");
        assumeTrue(Files.getFileStore(folder.getRoot().toPath()).supportsFileAttributeView("posix"));
        FileSecretStore store = new FileSecretStore(directory);

        store.store("source-1", "secret");

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(directory)));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(directory.resolve("source-1"))));
        try (var files = Files.list(directory)) {
            assertEquals("no temporary file is left behind", 1, files.count());
        }
    }

    @Test
    public void referencesCannotLeaveTheStore() {
        FileSecretStore store = new FileSecretStore(folder.getRoot().toPath().resolve("secrets"));

        for (String reference : new String[]{"../outside", "a/b", "", ".hidden", null}) {
            try {
                store.store(reference, "x");
                fail("accepted reference: " + reference);
            } catch (IllegalArgumentException expected) {
                // fine
            }
        }
        assertFalse(Files.exists(folder.getRoot().toPath().resolve("outside")));
    }
}
