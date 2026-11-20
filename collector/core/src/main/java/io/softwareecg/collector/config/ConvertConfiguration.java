package io.softwareecg.collector.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts a configuration in the XML format of earlier versions to YAML:
 * <pre>
 *   ekg-collector.sh --convert config.xml [config.yaml]
 * </pre>
 * The comments of the XML file are not carried over.
 */
public final class ConvertConfiguration {

    private ConvertConfiguration() {
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: --convert <config.xml> [<config.yaml>]");
            System.exit(2);
        }
        Path source = Path.of(args[0]);
        Path target = args.length == 2 ? Path.of(args[1]) : yamlNextTo(source);
        try {
            if (Files.exists(target)) {
                System.err.println(target + " exists already. Remove it first or name another file.");
                System.exit(1);
            }
            Files.writeString(target, convert(Files.readString(source, StandardCharsets.UTF_8), source.getFileName().toString()),
                    StandardCharsets.UTF_8);
            System.out.println("Written: " + target);
        } catch (IOException | InvalidConfigurationException e) {
            System.err.println(source + ": " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * @param text       a configuration, as XML of an earlier version or as YAML
     * @param sourceName the name of the file it comes from, for the first line of the result
     * @return the configuration as YAML
     * @throws InvalidConfigurationException if the text is not a valid configuration
     */
    public static String convert(String text, String sourceName) {
        return "# Software-EKG collector configuration, converted from " + sourceName + "\n\n"
                + CollectorConfigurations.toYaml(CollectorConfigurations.read(text));
    }

    private static Path yamlNextTo(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return source.resolveSibling((dot < 0 ? name : name.substring(0, dot)) + ".yaml");
    }
}
