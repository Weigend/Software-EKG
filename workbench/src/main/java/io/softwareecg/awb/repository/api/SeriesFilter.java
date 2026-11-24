package io.softwareecg.awb.repository.api;

import io.softwareecg.awb.timeseries.dimension.FilterDimension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Which series of a project are meant. All conditions apply.
 */
public final class SeriesFilter {

    /** How the values of a {@link Condition} are compared. */
    public enum Comparison { IS, MATCHES, MATCHES_NOT }

    /** A condition on one dimension; it holds if one of the values does. */
    public record Condition(FilterDimension dimension, Comparison comparison, List<String> values) {
    }

    private final String projectName;

    private final List<Condition> conditions = new ArrayList<>();

    private long start;

    private long end;

    private SeriesFilter(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @param projectName the name of the project
     * @return a filter for all series of the project
     */
    public static SeriesFilter ofProject(String projectName) {
        return new SeriesFilter(projectName);
    }

    /**
     * @param dimension the dimension
     * @param value     exactly this value; a {@code *} in it is nothing special
     * @return this filter
     */
    public SeriesFilter is(FilterDimension dimension, String value) {
        return add(dimension, Comparison.IS, Collections.singletonList(value));
    }

    /**
     * @param dimension the dimension
     * @param patterns  one of these patterns; {@code *} stands for any characters
     * @return this filter
     */
    public SeriesFilter matches(FilterDimension dimension, Collection<String> patterns) {
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("There is no pattern for " + dimension);
        }
        return add(dimension, Comparison.MATCHES, List.copyOf(patterns));
    }

    /**
     * @param dimension the dimension
     * @param pattern   not this pattern; {@code *} stands for any characters
     * @return this filter
     */
    public SeriesFilter matchesNot(FilterDimension dimension, String pattern) {
        return add(dimension, Comparison.MATCHES_NOT, List.of(pattern));
    }

    /**
     * Only parts of series that reach into the interval.
     *
     * @param start the start in milliseconds, open if it is 0 or less
     * @param end   the end in milliseconds, open if it is 0 or less
     * @return this filter
     */
    public SeriesFilter reachesInto(long start, long end) {
        this.start = start;
        this.end = end;
        return this;
    }

    public String projectName() {
        return projectName;
    }

    public List<Condition> conditions() {
        return Collections.unmodifiableList(conditions);
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    private SeriesFilter add(FilterDimension dimension, Comparison comparison, List<String> values) {
        conditions.add(new Condition(dimension, comparison, values));
        return this;
    }
}
