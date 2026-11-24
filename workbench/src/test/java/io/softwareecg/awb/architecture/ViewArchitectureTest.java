package io.softwareecg.awb.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Was {@link LayerArchitectureTest} nicht sieht: FXML nennt Klassen als Text, und Ansichten und
 * Stylesheets nennen Dateien. Auch sie benutzen von einer anderen Komponente nur api und spi.
 */
class ViewArchitectureTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    private static final Path VIEWS = RESOURCES.resolve("io/softwareecg/awb");

    /** Bibliotheken sind keine Komponenten: sie duerfen ueberall benutzt werden. */
    private static final Set<String> LIBRARIES = Set.of("infrastructure", "runtime", "timeseries", "uitoolkit");

    private static final Pattern TYPE = Pattern.compile("io\\.softwareecg\\.awb\\.(\\w+)\\.([\\w.*]+)");

    /** Ein Verweis auf eine Datei, vom Anfang des Klassenpfads oder von hier aus. */
    private static final Pattern FILE = Pattern.compile("(?:@import\\s+(?:url\\()?|url\\(|value=\"@|source=\"|stylesheets=\"@)[\"']?([^\"');]+)");

    @Test
    void viewsUseOtherComponentsThroughTheirApiOnly() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path view : files(".fxml")) {
            Matcher type = TYPE.matcher(Files.readString(view));
            while (type.find()) {
                if (isInside(type.group(1), type.group(2).replace('.', '/'), componentOf(view))) {
                    violations.add(VIEWS.relativize(view) + " -> " + type.group());
                }
            }
        }
        assertThat(violations).as("views that use the inside of another component").isEmpty();
    }

    @Test
    void viewsAndStylesheetsUseNoFilesOfOtherComponents() throws IOException {
        List<Path> sources = new ArrayList<>(files(".fxml"));
        sources.addAll(files(".css"));
        List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            Matcher file = FILE.matcher(Files.readString(source));
            while (file.find()) {
                String reference = file.group(1).trim();
                Path target = (reference.startsWith("/") ? RESOURCES.resolve(reference.substring(1))
                        : source.resolveSibling(reference)).normalize();
                if (target.startsWith(VIEWS) && target.getNameCount() > VIEWS.getNameCount() + 1) {
                    Path inside = VIEWS.relativize(target);
                    if (isInside(inside.getName(0).toString(), inside.subpath(1, inside.getNameCount()).toString(), componentOf(source))) {
                        violations.add(VIEWS.relativize(source) + " -> " + reference);
                    }
                }
            }
        }
        assertThat(violations).as("files that use the inside of another component").isEmpty();
    }

    /** Ob etwas von einer Komponente aus gesehen im Inneren einer anderen liegt. */
    private static boolean isInside(String other, String path, String component) {
        return !other.equals(component) && !LIBRARIES.contains(other)
                && !path.startsWith("api/") && !path.startsWith("spi/");
    }

    private static String componentOf(Path file) {
        return VIEWS.relativize(file).getName(0).toString();
    }

    private static List<Path> files(String extension) throws IOException {
        try (Stream<Path> files = Files.walk(VIEWS)) {
            List<Path> found = files.filter(file -> file.toString().endsWith(extension)).sorted().toList();
            assertThat(found).as("the files " + extension + " of the workbench").hasSizeGreaterThan(10);
            return found;
        }
    }
}
