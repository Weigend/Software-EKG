package io.softwareecg.collector.source.log;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One metric of the source {@code log}: a regular expression, and what a line that contains it
 * contributes to the series.
 * <p>
 * Without anything else a matching line counts: the series is the number of matching lines per
 * interval - logins per minute, errors of one code per second. If the expression has a group
 * named {@code sum}, {@code avg}, {@code min}, {@code max} or {@code last}, the group captures a
 * number and the series is that aggregate of the numbers of an interval - the bytes sent, the
 * average or the slowest response time.
 * <p>
 * The expression is searched in the line, it does not have to describe all of it; an expression
 * written for the whole line ({@code .*failed.*}) finds the same lines.
 */
final class LogMetric {

    enum Aggregate {
        /** the number of matching lines; 0 if there are none */
        COUNT,
        /** the sum of the captured numbers; 0 if there are none */
        SUM,
        /** their average; no value if there are none */
        AVG,
        /** the smallest; no value if there are none */
        MIN,
        /** the largest; no value if there are none */
        MAX,
        /** the one of the last matching line; no value if there are none */
        LAST;

        String groupName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final List<Aggregate> CAPTURING = List.of(
            Aggregate.SUM, Aggregate.AVG, Aggregate.MIN, Aggregate.MAX, Aggregate.LAST);

    private final String definition;

    private final Pattern pattern;

    private final Aggregate aggregate;

    private LogMetric(String definition, Pattern pattern, Aggregate aggregate) {
        this.definition = definition;
        this.pattern = pattern;
        this.aggregate = aggregate;
    }

    /**
     * @param definition the metric as it is written in the configuration
     * @throws IllegalArgumentException if it is not a regular expression, or names more than one
     *                                  aggregate
     */
    static LogMetric of(String definition) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(definition);
        } catch (PatternSyntaxException e) {
            // Without the cause: a job reports the innermost exception, and that would be the one
            // of the pattern, which does not say which metric of which source it is about.
            throw new IllegalArgumentException("The metric '" + definition + "' of the source log is not a regular "
                    + "expression: " + e.getDescription() + " (at position " + e.getIndex() + ")");
        }

        Map<String, Integer> groups = pattern.namedGroups();
        Aggregate aggregate = Aggregate.COUNT;
        for (Aggregate candidate : CAPTURING) {
            if (groups.containsKey(candidate.groupName())) {
                if (aggregate != Aggregate.COUNT) {
                    throw new IllegalArgumentException("The metric '" + definition + "' of the source log names two "
                            + "aggregates, " + aggregate.groupName() + " and " + candidate.groupName()
                            + ". A metric is one series: write two metrics.");
                }
                aggregate = candidate;
            }
        }
        return new LogMetric(definition, pattern, aggregate);
    }

    Aggregate aggregate() {
        return aggregate;
    }

    String definition() {
        return definition;
    }

    /**
     * @return what the line contributes: 1 for a line that counts, the captured number for an
     *         aggregate of numbers; {@code null} if the line does not match or what it captures is
     *         not a number
     */
    Double valueOf(String line) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        if (aggregate == Aggregate.COUNT) {
            return 1.0;
        }
        return number(matcher.group(aggregate.groupName()));
    }

    /**
     * @return the number, also if it is written with a decimal comma; {@code null} if it is none
     */
    static Double number(String text) {
        if (text == null) {
            return null;
        }
        String number = text.strip();
        if (number.indexOf(',') >= 0 && number.indexOf('.') < 0 && number.indexOf(',') == number.lastIndexOf(',')) {
            number = number.replace(',', '.');
        }
        try {
            double value = Double.parseDouble(number);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return aggregate.groupName() + " of /" + definition + "/";
    }
}
