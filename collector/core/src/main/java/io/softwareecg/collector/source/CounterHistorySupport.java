package io.softwareecg.collector.source;

import io.softwareecg.collector.config.CounterDescriptor;

import java.util.Date;
import java.util.function.Consumer;

/**
 * Implemented by counter sources that keep a history of their own and can therefore deliver
 * not only the current values, but every value set since a point in time - a time series
 * database such as Prometheus, for example.
 * <p>
 * A collector job uses this instead of {@link CounterSource#queryCounter(CounterDescriptor)}
 * for such sources. The first fill of a new measurement, the regular update and catching up
 * after the collector was not running are then one and the same call.
 */
public interface CounterHistorySupport {

    /**
     * Delivers all value sets that are newer than the given time, oldest first. Each value set
     * holds one value per metric definition of the descriptor, in their order; a value that
     * is missing at that time is {@code null}.
     *
     * @param descriptor holds host/counter/process/source
     * @param since      only value sets after this time are delivered. {@code null} if nothing
     *                   was collected yet; the source then decides how far back it starts.
     * @param sink       receives the value sets one by one, so that a long history does not
     *                   have to be held in memory
     * @return {@code false} if the source could not be queried; nothing new at the moment is
     *         not an error and yields {@code true} without any value set
     */
    boolean queryCounterSince(CounterDescriptor descriptor, Date since, Consumer<CounterSeries> sink);
}
