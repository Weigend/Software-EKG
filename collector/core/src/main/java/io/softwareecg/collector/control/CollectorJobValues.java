package io.softwareecg.collector.control;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The values a job read last, for something that shows what a collector is doing.
 *
 * @param time   the time the values belong to; {@code null} if the job read none yet
 * @param labels the names of the metrics, in the order of the values
 * @param values one value per metric; {@code null} where the source had none
 */
public record CollectorJobValues(Instant time, List<String> labels, List<Double> values) {

    /** What a job has before it read anything. */
    public static final CollectorJobValues NONE = new CollectorJobValues(null, List.of(), List.of());

    public CollectorJobValues {
        labels = List.copyOf(labels);
        // values may be missing, which List.copyOf does not take
        values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    /**
     * @return {@code true} if the job read no values yet
     */
    public boolean isEmpty() {
        return time == null;
    }
}
