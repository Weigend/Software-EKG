package io.softwareecg.collector.source.log;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The arguments of a process read by the source {@code log}, as written in its {@code args}:
 * <pre>
 *   args: 'lookback=24h; timestamp=dd.MM.yyyy HH:mm:ss; zone=Europe/Berlin'
 * </pre>
 * Arguments are separated by semicolons, because a timestamp pattern contains blanks and commas.
 * An argument that is not known is an error, like an unknown key of the configuration: a
 * setting that is misspelled would otherwise just have no effect.
 *
 * @param lookbackMillis   how far back the content the file already has is read when collecting
 *                         starts; 0 for only what is written from then on
 * @param timestampPattern the pattern of the timestamps in the lines, as
 *                         {@link java.time.format.DateTimeFormatter} reads it; {@code null} to
 *                         recognise the usual formats
 * @param zone             the time zone of timestamps that do not name one
 * @param charset          the character set of the file
 * @param port             the SSH port of a host that is not this machine
 */
record LogArguments(long lookbackMillis, String timestampPattern, ZoneId zone, Charset charset, int port) {

    static final int DEFAULT_SSH_PORT = 22;

    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*([smhd])");

    private static final String KNOWN = "lookback, timestamp, zone, charset, port";

    /**
     * @param arguments the {@code args} of the process, may be {@code null}
     * @throws IllegalArgumentException if an argument is not known or its value is not usable
     */
    static LogArguments parse(String arguments) {
        long lookback = 0;
        String timestamp = null;
        ZoneId zone = ZoneId.systemDefault();
        Charset charset = StandardCharsets.UTF_8;
        int port = DEFAULT_SSH_PORT;

        if (arguments != null) {
            for (String argument : arguments.split(";")) {
                if (argument.isBlank()) {
                    continue;
                }
                int equals = argument.indexOf('=');
                if (equals < 0) {
                    throw invalid("'" + argument.strip() + "' is not written as name=value.");
                }
                String name = argument.substring(0, equals).strip();
                String value = argument.substring(equals + 1).strip();
                switch (name) {
                    case "lookback" -> lookback = duration(value);
                    case "timestamp" -> {
                        if (value.isEmpty()) {
                            throw invalid("'timestamp' is empty.");
                        }
                        timestamp = value;
                    }
                    case "zone" -> {
                        try {
                            zone = ZoneId.of(value);
                        } catch (DateTimeException e) {
                            throw invalid("'" + value + "' is not a time zone. Write it as Europe/Berlin, UTC or +01:00.");
                        }
                    }
                    case "charset" -> {
                        try {
                            charset = Charset.forName(value);
                        } catch (IllegalArgumentException e) {
                            throw invalid("'" + value + "' is not a character set this Java knows, such as UTF-8 "
                                    + "or ISO-8859-1.");
                        }
                    }
                    case "port" -> {
                        try {
                            port = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            port = -1;
                        }
                        if (port < 1 || port > 65535) {
                            throw invalid("'" + value + "' is not a port.");
                        }
                    }
                    default -> throw invalid("there is no argument '" + name + "'. Known: " + KNOWN + ".");
                }
            }
        }
        return new LogArguments(lookback, timestamp, zone, charset, port);
    }

    private static long duration(String text) {
        Matcher matcher = DURATION.matcher(text);
        if (!matcher.matches()) {
            throw invalid("'" + text + "' is not a duration. Write the lookback as 30m, 24h or 7d.");
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> amount * 1_000;
            case "m" -> amount * 60_000;
            case "h" -> amount * 3_600_000;
            default -> amount * 86_400_000;
        };
    }

    private static IllegalArgumentException invalid(String what) {
        return new IllegalArgumentException("The args of a process read by the source log: " + what
                + " Arguments are separated by semicolons.");
    }
}
