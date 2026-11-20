package io.softwareecg.collector.config.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.softwareecg.collector.config.InvalidConfigurationException;
import io.softwareecg.collector.config.model.CollectorConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes a configuration as YAML.
 * <p>
 * Reading is strict: a key that is not known, a key given twice or a value of the wrong kind
 * is an error that names the place. Nothing is skipped silently - a setting that is misspelled
 * would otherwise just have no effect.
 */
final class YamlConfig {

    private static final ObjectMapper MAPPER = createMapper();

    private YamlConfig() {
    }

    /**
     * @throws InvalidConfigurationException if the text is not a valid configuration
     */
    static CollectorConfig read(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            throw new InvalidConfigurationException("The configuration is empty.");
        }
        CollectorConfig config;
        try {
            config = MAPPER.readValue(yaml, CollectorConfig.class);
        } catch (JsonProcessingException e) {
            throw new InvalidConfigurationException(describe(e, yaml), e);
        }
        if (config == null) {
            throw new InvalidConfigurationException("The configuration is empty.");
        }
        config.validate();
        return config;
    }

    static String write(CollectorConfig config) {
        try {
            // an empty line before every section, as one would write it by hand
            return MAPPER.writeValueAsString(config).replaceAll("\\n(?=[A-Za-z])", "\n\n");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The configuration can not be written: " + e.getOriginalMessage(), e);
        }
    }

    private static ObjectMapper createMapper() {
        YAMLFactory factory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                // a password of digits stays a text
                .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build();
        factory.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

        SimpleModule durations = new SimpleModule("durations");
        durations.addDeserializer(Duration.class, new DurationReader());
        durations.addSerializer(Duration.class, new DurationWriter());

        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.registerModule(durations);
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        return mapper;
    }

    // ---------------------------------------------------------------------------------------------
    // errors
    // ---------------------------------------------------------------------------------------------

    private static String describe(JsonProcessingException e, String yaml) {
        StringBuilder text = new StringBuilder();
        JsonLocation location = e.getLocation();
        int line = location == null ? 0 : location.getLineNr();

        if (e instanceof UnrecognizedPropertyException unknown) {
            line = lineOfKey(yaml, unknown.getPropertyName(), line);
            if (line > 0) {
                text.append("line ").append(line).append(": ");
            }
            text.append("unknown key '").append(unknown.getPropertyName()).append("'");
            String parent = path(unknown, 1);
            if (!parent.isEmpty()) {
                text.append(" in ").append(parent);
            }
            text.append(". Known here: ")
                    .append(new TreeSet<>(unknown.getKnownPropertyIds().stream().map(String::valueOf).toList()));
            return text.toString();
        }

        if (line > 0) {
            text.append("line ").append(line).append(": ");
        }
        if (e instanceof JsonMappingException mapping) {
            String path = path(mapping, 0);
            if (!path.isEmpty()) {
                text.append(path).append(": ");
            }
        }
        return text.append(firstLine(e.getOriginalMessage())).toString();
    }

    /**
     * The parser reports an unknown key where it stands after reading it, which is the line
     * after. This finds the line the key is written in.
     *
     * @param reported the line the parser reported
     * @return the last line up to the reported one that has the key, or the reported line
     */
    private static int lineOfKey(String yaml, String key, int reported) {
        String[] lines = yaml.split("\\R", -1);
        Pattern written = Pattern.compile("(^|[\\s{,-])[\"']?" + Pattern.quote(key) + "[\"']?\\s*:");
        for (int i = Math.min(reported, lines.length); i >= 1; i--) {
            if (written.matcher(lines[i - 1]).find()) {
                return i;
            }
        }
        return reported;
    }

    /**
     * @return where in the configuration the error is, as {@code collectors[0].interval}
     */
    private static String path(JsonMappingException e, int withoutLast) {
        StringBuilder path = new StringBuilder();
        int end = e.getPath().size() - withoutLast;
        for (int i = 0; i < end; i++) {
            JsonMappingException.Reference reference = e.getPath().get(i);
            if (reference.getFieldName() != null) {
                if (path.length() > 0) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "not readable";
        }
        int end = message.indexOf('\n');
        return end < 0 ? message : message.substring(0, end);
    }

    // ---------------------------------------------------------------------------------------------
    // durations: 15s, 5m, 500ms ...
    // ---------------------------------------------------------------------------------------------

    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*(ms|s|m|h|d)?");

    /**
     * @param text a number with a unit: {@code 500ms}, {@code 15s}, {@code 5m}, {@code 1h},
     *             {@code 1d}. A number alone counts milliseconds.
     */
    static Duration parseDuration(String text) {
        Matcher matcher = DURATION.matcher(text.strip().toLowerCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + text + "' is not a duration. Write it as 500ms, 15s, 5m, 1h or 1d.");
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2) == null ? "ms" : matcher.group(2);
        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> Duration.ofMillis(amount);
        };
    }

    /**
     * @return the duration with the largest unit that expresses it exactly
     */
    static String formatDuration(Duration duration) {
        long millis = duration.toMillis();
        if (millis != 0 && millis % 86_400_000 == 0) {
            return millis / 86_400_000 + "d";
        }
        if (millis != 0 && millis % 3_600_000 == 0) {
            return millis / 3_600_000 + "h";
        }
        if (millis != 0 && millis % 60_000 == 0) {
            return millis / 60_000 + "m";
        }
        if (millis % 1_000 == 0) {
            return millis / 1_000 + "s";
        }
        return millis + "ms";
    }

    private static final class DurationReader extends StdDeserializer<Duration> {

        private DurationReader() {
            super(Duration.class);
        }

        @Override
        public Duration deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                return Duration.ofMillis(parser.getLongValue());
            }
            if (parser.currentToken() == JsonToken.VALUE_STRING) {
                try {
                    return parseDuration(parser.getText());
                } catch (IllegalArgumentException e) {
                    throw JsonMappingException.from(parser, e.getMessage());
                }
            }
            throw JsonMappingException.from(parser, "a duration is expected here, for example 15s");
        }
    }

    private static final class DurationWriter extends StdSerializer<Duration> {

        private DurationWriter() {
            super(Duration.class);
        }

        @Override
        public void serialize(Duration value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(formatDuration(value));
        }
    }
}
