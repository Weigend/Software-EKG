package io.softwareecg.awb.datasource.bl.prometheus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A selection of names as the user types it: names or patterns separated by commas, where
 * {@code *} stands for any text and {@code ?} for one character - {@code db*, web-0?}.
 * Upper and lower case make no difference. An empty text selects everything.
 * <p>
 * The pattern is used twice: here, to show at once what it selects, and in Prometheus, where
 * it stays a pattern ({@link #regex()}) so that names that match later are included too.
 */
public final class WildcardPattern {

    /**
     * Selects everything.
     */
    public static final WildcardPattern ALL = new WildcardPattern(List.of());

    private static final String REGEX_SPECIAL = "\\.+()[]{}|^$";

    private final List<String> parts;

    private final Pattern pattern;

    private WildcardPattern(List<String> parts) {
        this.parts = List.copyOf(parts);
        this.pattern = parts.isEmpty() ? null : Pattern.compile(regex(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param text names or patterns separated by commas or semicolons; {@code null}, blank or
     *             a single {@code *} select everything
     */
    public static WildcardPattern parse(String text) {
        if (text == null) {
            return ALL;
        }
        List<String> parts = new ArrayList<>();
        for (String part : text.split("[,;]")) {
            String trimmed = part.trim();
            if (trimmed.matches("\\*+")) {
                return ALL;
            }
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts.isEmpty() ? ALL : new WildcardPattern(parts);
    }

    /**
     * @return {@code true} if nothing is excluded
     */
    public boolean isAll() {
        return parts.isEmpty();
    }

    public boolean matches(String name) {
        return pattern == null || pattern.matcher(name).matches();
    }

    /**
     * @return the names the pattern selects, in the order given
     */
    public List<String> select(Collection<String> names) {
        return names.stream().filter(this::matches).collect(Collectors.toList());
    }

    /**
     * The pattern as a regular expression that Prometheus and Java read alike: the parts as
     * alternatives, without flags. It has to match the whole name.
     *
     * @throws IllegalStateException if the pattern selects everything and so has no parts
     */
    public String regex() {
        if (parts.isEmpty()) {
            throw new IllegalStateException("A pattern that selects everything has no expression.");
        }
        return parts.stream().map(WildcardPattern::regexOf).collect(Collectors.joining("|"));
    }

    private static String regexOf(String part) {
        StringBuilder regex = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append('.');
            } else if (REGEX_SPECIAL.indexOf(c) >= 0) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
        }
        return regex.toString();
    }

    @Override
    public String toString() {
        return parts.isEmpty() ? "*" : String.join(", ", parts);
    }
}
