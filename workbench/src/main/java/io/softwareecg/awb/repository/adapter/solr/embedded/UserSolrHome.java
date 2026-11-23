package io.softwareecg.awb.repository.adapter.solr.embedded;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the index of the embedded Solr where the other data of the user is: in
 * {@code ~/.ekg/solr}, next to the metadata store.
 * <p>
 * The index holds the collected and imported time series. It must not live in the build tree
 * or in the installation directory: there a {@code mvn clean} or a new version of the
 * application would delete it, while the projects in the metadata store stay behind.
 * <p>
 * The Solr directory in the user's home consists of two things. The <b>configuration</b>
 * (solr.xml, schema, solrconfig) belongs to the application; it is copied from the template
 * that ships with it on every start, so a new version brings its configuration along. The
 * <b>data</b> belongs to the user and is never touched.
 */
public final class UserSolrHome {

    /**
     * Overrides the directory, like the environment variable {@value #HOME_ENVIRONMENT_VARIABLE}.
     * It is the property the embedded Solr is located by ({@link DefaultSolrConfig}).
     */
    public static final String HOME_PROPERTY = DefaultSolrConfig.SOLR_BASE_DIR_PROPERTY;

    public static final String HOME_ENVIRONMENT_VARIABLE = "EKG_SOLR_HOME";

    /**
     * Where the configuration template is, if not next to the application.
     */
    public static final String TEMPLATE_PROPERTY = "ekg.solr.template";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSolrHome.class);

    private static final String DATA_DIRECTORY = "data";

    private UserSolrHome() {
    }

    /**
     * Makes the application use the Solr directory in the user's home, unless a directory is
     * configured explicitly with the system property {@value DefaultSolrConfig#SOLR_BASE_DIR_PROPERTY}.
     * To be called by the application before anything asks for Solr - not by tests, which
     * must never write into the data of the user.
     */
    public static void useUnlessConfigured() {
        String configured = System.getProperty(HOME_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return;
        }
        Path template = findTemplate();
        if (template == null) {
            LOGGER.warn("No Solr configuration template found next to the application; "
                    + "the embedded Solr falls back to its default search paths.");
            return;
        }
        String fromEnvironment = System.getenv(HOME_ENVIRONMENT_VARIABLE);
        Path home = fromEnvironment != null && !fromEnvironment.isBlank()
                ? Paths.get(fromEnvironment)
                : Paths.get(System.getProperty("user.home"), ".ekg", "solr");
        try {
            prepare(template, home);
            System.setProperty(HOME_PROPERTY, home.toAbsolutePath().toString());
            LOGGER.info("Embedded Solr uses {} (configuration from {})", home.toAbsolutePath(), template);
        } catch (UncheckedIOException e) {
            LOGGER.warn("Could not prepare the Solr directory {}: {}", home, e.getMessage());
        }
    }

    /**
     * Brings the configuration in a Solr directory up to date and leaves its data alone.
     * An index that an earlier version left beside the template is moved over once, if the
     * directory has none yet.
     *
     * @param template the directory with the configuration that ships with the application
     * @param home     the Solr directory to prepare; created if missing
     */
    public static void prepare(Path template, Path home) {
        try {
            Files.createDirectories(home);
            if (template.toAbsolutePath().normalize().equals(home.toAbsolutePath().normalize())) {
                return;
            }
            moveLeftBehindIndex(template, home);
            Files.walkFileTree(template, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                    if (DATA_DIRECTORY.equals(String.valueOf(directory.getFileName()))) {
                        return FileVisitResult.SKIP_SUBTREE;    // the data belongs to the user
                    }
                    Files.createDirectories(home.resolve(template.relativize(directory).toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.copy(file, home.resolve(template.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Every core directory of the template (a directory with a {@code core.properties}) that
     * holds data, while the same core in the home has none: the data is moved.
     */
    private static void moveLeftBehindIndex(Path template, Path home) throws IOException {
        try (var cores = Files.list(template)) {
            for (Path core : cores.filter(Files::isDirectory).toList()) {
                Path leftBehind = core.resolve(DATA_DIRECTORY);
                Path target = home.resolve(core.getFileName().toString()).resolve(DATA_DIRECTORY);
                if (Files.isDirectory(leftBehind) && !Files.exists(target)) {
                    Files.createDirectories(target.getParent());
                    Files.move(leftBehind, target);
                    LOGGER.info("Moved the existing Solr index from {} to {}", leftBehind, target);
                }
            }
        }
    }

    /**
     * The template is given explicitly, or it stands beside the application: beside the
     * compiled classes in a build ({@code target/solr}), or beside the {@code lib} directory
     * of an installed distribution.
     */
    private static Path findTemplate() {
        List<Path> candidates = new ArrayList<>();
        String configured = System.getProperty(TEMPLATE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            candidates.add(Paths.get(configured));
        }
        try {
            Path code = Paths.get(UserSolrHome.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path beside = code.getParent();
            if (beside != null) {
                candidates.add(beside.resolve("solr"));
                if (beside.getParent() != null) {
                    candidates.add(beside.getParent().resolve("solr"));
                }
            }
        } catch (NullPointerException | URISyntaxException | IllegalArgumentException | SecurityException e) {
            LOGGER.debug("Cannot derive the location of the application", e);
        }
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("solr.xml"))) {
                return candidate;
            }
        }
        return null;
    }
}
