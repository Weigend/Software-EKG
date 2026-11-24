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
 * Was {@link LayerArchitectureTest} nicht sieht: FXML nennt Klassen als Text. Auch eine Ansicht
 * benutzt von einer anderen Komponente nur api und spi.
 */
class FxmlArchitectureTest {

    private static final Path VIEWS = Path.of("src/main/resources/io/softwareecg/awb");

    /** Bibliotheken sind keine Komponenten: sie duerfen ueberall benutzt werden. */
    private static final Set<String> LIBRARIES = Set.of("infrastructure", "runtime", "timeseries", "uitoolkit");

    private static final Pattern TYPE = Pattern.compile("io\\.softwareecg\\.awb\\.(\\w+)\\.([\\w.*]+)");

    @Test
    void viewsUseOtherComponentsThroughTheirApiOnly() throws IOException {
        List<Path> views;
        try (Stream<Path> files = Files.walk(VIEWS)) {
            views = files.filter(file -> file.toString().endsWith(".fxml")).sorted().toList();
        }
        assertThat(views).as("the views of the workbench").hasSizeGreaterThan(10);

        List<String> violations = new ArrayList<>();
        for (Path view : views) {
            String component = VIEWS.relativize(view).getName(0).toString();
            Matcher type = TYPE.matcher(Files.readString(view));
            while (type.find()) {
                String other = type.group(1);
                if (!other.equals(component) && !LIBRARIES.contains(other)
                        && !type.group(2).startsWith("api.") && !type.group(2).startsWith("spi.")) {
                    violations.add(VIEWS.relativize(view) + " -> " + type.group());
                }
            }
        }
        assertThat(violations).as("views that use the inside of another component").isEmpty();
    }
}
