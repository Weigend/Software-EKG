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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Views, stylesheets and icons are found by their path, which the compiler does not check: when a
 * class moves to another package and its files do not, the dialog fails only when it is opened.
 * This test looks every such reference up that is written out in the sources.
 */
class ResourceReferencesTest {

    private static final Path JAVA = Path.of("src/main/java");
    private static final Path RESOURCES = Path.of("src/main/resources");

    /** Not written yet: the menu entry "Rename Group" of a bookmark group has only an empty view. */
    private static final Set<String> KNOWN = Set.of(
            "EditBookmarkGroupDialogView.fxml -> io.softwareecg.awb.bookmark.ui.EditBookmarkGroupDialogViewXml");

    private static final Pattern CSS_IMPORT = Pattern.compile("@import\\s+(?:url\\()?[\"']?([^\"');]+)");
    private static final Pattern FXML_FILE = Pattern.compile("(?:<fx:include[^>]*source=\"|value=\"@|stylesheets=\"@)([^\"]+)\"");
    private static final Pattern FXML_CONTROLLER = Pattern.compile("fx:controller=\"(io\\.softwareecg\\.awb\\.[^\"]+)\"");
    private static final Pattern JAVA_RESOURCE = Pattern.compile(
            "(?:(getClass\\(\\)|(\\w+)\\.class)\\s*\\.getResource(?:AsStream)?|loadView)\\(\\s*\"([^\"]+)\"\\s*[,)]");

    @Test
    void stylesheets_find_what_they_import() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path css : files(RESOURCES, ".css")) {
            for (Matcher m = CSS_IMPORT.matcher(Files.readString(css)); m.find(); ) {
                check(css, m.group(1).trim(), css.getParent(), missing);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void views_find_their_files_and_controllers() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path fxml : files(RESOURCES, ".fxml")) {
            String text = Files.readString(fxml);
            for (Matcher m = FXML_FILE.matcher(text); m.find(); ) {
                check(fxml, m.group(1), fxml.getParent(), missing);
            }
            for (Matcher m = FXML_CONTROLLER.matcher(text); m.find(); ) {
                String source = m.group(1).replace('.', '/').replaceAll("\\$.*", "") + ".java";
                if (!Files.exists(JAVA.resolve(source))) {
                    report(fxml, m.group(1), missing);
                }
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void classes_find_their_resources() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path java : files(JAVA, ".java")) {
            String text = Files.readString(java);
            Path ownPackage = JAVA.relativize(java.getParent());
            for (Matcher m = JAVA_RESOURCE.matcher(text); m.find(); ) {
                Path base = m.group(2) == null ? ownPackage : packageOf(m.group(2), text, ownPackage);
                check(java, m.group(3), RESOURCES.resolve(base), missing);
            }
        }
        assertEquals(List.of(), missing);
    }

    private static Path packageOf(String type, String source, Path ownPackage) {
        Matcher m = Pattern.compile("import ([\\w.]+)\\." + type + ";").matcher(source);
        return m.find() ? Path.of(m.group(1).replace('.', '/')) : ownPackage;
    }

    private static void check(Path from, String reference, Path base, List<String> missing) {
        if (reference.startsWith("http")) {
            return;
        }
        boolean found = reference.startsWith("/")
                // what is not ours may come from a library
                ? Files.exists(RESOURCES.resolve(reference.substring(1))) || !reference.startsWith("/io/softwareecg/awb")
                : Files.exists(base.resolve(reference).normalize());
        if (!found) {
            report(from, reference, missing);
        }
    }

    private static void report(Path from, String reference, List<String> missing) {
        String entry = from.getFileName() + " -> " + reference;
        if (!KNOWN.contains(entry)) {
            missing.add(entry);
        }
    }

    private static List<Path> files(Path root, String suffix) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(suffix)).sorted().toList();
        }
    }
}
