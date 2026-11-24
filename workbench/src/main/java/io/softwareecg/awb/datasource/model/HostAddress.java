package io.softwareecg.awb.datasource.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A host a source reads from.
 *
 * @param name    the name of the host in the analysis
 * @param address where it is reached: a name or address, optionally with the SSH port
 */
public record HostAddress(String name, String address) {

    private static final Pattern RANGE = Pattern.compile("\\[(\\d+)-(\\d+)]");

    /**
     * Reads a host as the user types it: {@code db-01}, {@code db-01:2222} or, with a name
     * of its own, {@code db-01=10.0.0.5}.
     */
    public static HostAddress parse(String text) {
        String trimmed = text.trim();
        int equals = trimmed.indexOf('=');
        if (equals > 0) {
            return new HostAddress(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
        }
        int colon = trimmed.lastIndexOf(':');
        boolean port = colon > 0 && trimmed.indexOf(':') == colon
                && trimmed.substring(colon + 1).chars().allMatch(Character::isDigit);
        return new HostAddress(port ? trimmed.substring(0, colon) : trimmed, trimmed);
    }

    /**
     * @return {@code true} if the host is this machine and read without SSH
     */
    public boolean isLocal() {
        return address.startsWith("localhost");
    }

    /**
     * Reads a list of hosts as the user types it, separated by commas, semicolons, blanks or
     * line breaks. A range in square brackets is written out: {@code db-[01-03]} are the hosts
     * {@code db-01}, {@code db-02} and {@code db-03}.
     */
    public static List<HostAddress> parseAll(String text) {
        if (text == null) {
            return List.of();
        }
        List<HostAddress> hosts = new ArrayList<>();
        for (String part : text.split("[,;\\s]+")) {
            if (part.isBlank()) {
                continue;
            }
            for (String expanded : expandRange(part.trim())) {
                HostAddress host = HostAddress.parse(expanded);
                if (hosts.stream().noneMatch(known -> known.name().equals(host.name()))) {
                    hosts.add(host);
                }
            }
        }
        return hosts;
    }

    private static List<String> expandRange(String text) {
        Matcher range = RANGE.matcher(text);
        if (!range.find()) {
            return List.of(text);
        }
        int from = Integer.parseInt(range.group(1));
        int to = Integer.parseInt(range.group(2));
        if (to < from || to - from > 999) {
            return List.of(text);
        }
        // "01" keeps its leading zero: the numbers are as wide as the first one is written
        String format = range.group(1).startsWith("0") ? "%0" + range.group(1).length() + "d" : "%d";
        List<String> result = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            String number = String.format(format, i);
            result.addAll(expandRange(text.substring(0, range.start()) + number + text.substring(range.end())));
        }
        return result;
    }
}
