package io.softwareecg.awb.secrets.adapter.file;

import io.softwareecg.awb.secrets.port.SecretStore;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Keeps secrets as files that only the owner can read, in {@code ~/.ekg/secrets} - the way
 * SSH keys or cloud credentials are kept. The secrets are thereby out of the metadata database
 * and out of every stored or exported configuration.
 * <p>
 * The files are not encrypted: who can read the user's files can read the secrets. A keyring of
 * the operating system can replace this store behind the {@link SecretStore} interface.
 */
@Singleton
public class FileSecretStore implements SecretStore {

    private static final Set<PosixFilePermission> OWNER_ONLY_DIRECTORY = PosixFilePermissions.fromString("rwx------");

    private static final Set<PosixFilePermission> OWNER_ONLY_FILE = PosixFilePermissions.fromString("rw-------");

    private final Path directory;

    public FileSecretStore() {
        this(Paths.get(System.getProperty("user.home"), ".ekg", "secrets"));
    }

    FileSecretStore(Path directory) {
        this.directory = directory;
    }

    @Override
    public void store(String reference, String secret) {
        Path file = fileOf(reference);
        try {
            Files.createDirectories(directory);
            restrict(directory, OWNER_ONLY_DIRECTORY);

            // written next to the target and moved into place: never a half-written secret
            Path temporary = Files.createTempFile(directory, "secret-", ".tmp");
            try {
                restrict(temporary, OWNER_ONLY_FILE);
                Files.writeString(temporary, secret, StandardCharsets.UTF_8);
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the secret '" + reference + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String read(String reference) {
        Path file = fileOf(reference);
        try {
            return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the secret '" + reference + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String reference) {
        try {
            Files.deleteIfExists(fileOf(reference));
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete the secret '" + reference + "': " + e.getMessage(), e);
        }
    }

    private Path fileOf(String reference) {
        if (reference == null || !reference.matches("[A-Za-z0-9._-]{1,200}") || reference.startsWith(".")) {
            throw new IllegalArgumentException("Invalid secret reference: " + reference);
        }
        return directory.resolve(reference);
    }

    private static void restrict(Path path, Set<PosixFilePermission> permissions) throws IOException {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException e) {
            // no POSIX permissions (Windows): the files in the user's profile are private to the user there
        }
    }
}
