package io.softwareecg.collector.source.prometheus;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterHistorySupport;
import io.softwareecg.collector.source.CounterSeries;
import io.softwareecg.collector.source.CounterSource;
import io.softwareecg.collector.source.CounterSourceListener;
import io.softwareecg.collector.source.CounterWildcardSupport;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.ExpandWildcardResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects time series from a Prometheus server.
 * <p>
 * In the collector configuration the node is the Prometheus server (its address is the URL)
 * and every metric definition is a PromQL expression - a plain metric name, a selector or a
 * calculation such as a rate. An expression usually yields many series, one per monitored
 * system; they are resolved like wildcards and named in Prometheus notation
 * ({@link PrometheusSeriesName}).
 * <p>
 * Prometheus keeps the history itself, so this source delivers every value since the last one
 * collected ({@link CounterHistorySupport}). The values lie on a fixed time grid whose step is
 * the fetch interval of the job. How far back the very first fill reaches is set with the
 * process argument {@code lookback}, for example {@code args="lookback=24h"}; without it
 * collecting starts now.
 */
public class PrometheusCounterSource implements CounterSource, CounterWildcardSupport, CounterHistorySupport {

    public static final String ID = "prometheus";

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusCounterSource.class);

    private static final Pattern LOOKBACK = Pattern.compile("(?:^|[\\s,;])lookback=(\\d+)([smhd])(?=$|[\\s,;])");

    /**
     * Prometheus answers at most 11,000 points per series and query.
     */
    private static final int MAX_POINTS_PER_QUERY = 10_000;

    /**
     * Upper bound for the values held in memory for one time window of a history fill.
     */
    private static final int MAX_VALUES_PER_WINDOW = 500_000;

    private final LongSupplier clock;

    private PrometheusApi api;

    /**
     * Whether the last query succeeded; used to log changes only, not every failed cycle.
     */
    private boolean reachable = true;

    public PrometheusCounterSource() {
        this(System::currentTimeMillis);
    }

    PrometheusCounterSource(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    public String getId() {
        return ID;
    }

    //=================================================================================================================
    // resolving expressions to concrete series
    //=================================================================================================================

    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {
        List<MetricDefinition> expanded = new ArrayList<>();
        try {
            for (MetricDefinition definition : descriptor.getMetricDefinitions()) {
                List<String> names = new ArrayList<>();
                for (PrometheusApi.Series series : api(descriptor).query(definition.getDefinition())) {
                    names.add(PrometheusSeriesName.of(definition.getLabel(), series.labels()).toString());
                }
                names.stream().sorted().distinct()
                        .forEach(name -> expanded.add(new MetricDefinition(definition.getDefinition(), name)));
            }
            markReachable(descriptor);
        } catch (PrometheusException e) {
            // not "no such metric": the server did not answer, and the job has to say so
            markUnreachable(descriptor, e);
            return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE, e.getMessage());
        }

        if (expanded.isEmpty()) {
            LOGGER.warn("The expressions of metric group '{}' yield no series at {}.",
                    descriptor.getMetricGroupId(), descriptor.getNodeAddress());
            return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_NOT_FOUND);
        }
        return new ExpandWildcardResponse(expanded.toArray(new MetricDefinition[0]));
    }

    @Override
    public void addCounterListener(CounterSourceListener sourceListener) {
        // new and vanished series are picked up by the periodic wildcard refresh of the job
    }

    //=================================================================================================================
    // current values
    //=================================================================================================================

    @Override
    public CounterSeries queryCounter(CounterDescriptor descriptor) {
        Columns columns = new Columns(descriptor.getMetricDefinitions());
        Double[] values = new Double[columns.count()];
        try {
            for (String expression : columns.expressions()) {
                for (PrometheusApi.Series series : api(descriptor).query(expression)) {
                    Integer column = columns.columnOf(expression, series);
                    if (column != null && !series.samples().isEmpty()) {
                        values[column] = series.samples().get(0).value();
                    }
                }
            }
            markReachable(descriptor);
        } catch (PrometheusException e) {
            markUnreachable(descriptor, e);
            return new CounterSeries(List.of());
        }
        return new CounterSeries(new Date(clock.getAsLong()), Arrays.asList(values));
    }

    //=================================================================================================================
    // all values since a point in time
    //=================================================================================================================

    @Override
    public boolean queryCounterSince(CounterDescriptor descriptor, Date since, Consumer<CounterSeries> sink) {
        Columns columns = new Columns(descriptor.getMetricDefinitions());
        if (columns.count() == 0) {
            return false;
        }

        final long step = Math.max(1_000, descriptor.getMetricFetchInterval());
        final long now = clock.getAsLong();

        // all values lie on the grid of multiples of the step, so that the series stays
        // regular across cycles and restarts
        final long end = (now / step) * step;
        long start = since != null
                ? (since.getTime() / step) * step + step
                : ceilToGrid(now - lookbackMillis(descriptor), step);

        final long pointsPerWindow = Math.max(1, Math.min(MAX_POINTS_PER_QUERY, MAX_VALUES_PER_WINDOW / columns.count()));

        try {
            while (start <= end) {
                if (Thread.currentThread().isInterrupted()) {
                    return true;
                }
                long windowEnd = Math.min(end, start + (pointsPerWindow - 1) * step);

                TreeMap<Long, Double[]> rows = new TreeMap<>();
                for (String expression : columns.expressions()) {
                    for (PrometheusApi.Series series : api(descriptor).queryRange(expression, start, windowEnd, step)) {
                        Integer column = columns.columnOf(expression, series);
                        if (column == null) {
                            continue;   // a series that appeared after the last wildcard refresh
                        }
                        for (PrometheusApi.Sample sample : series.samples()) {
                            if (sample.value() != null) {
                                rows.computeIfAbsent(sample.timestampMillis(), t -> new Double[columns.count()])[column]
                                        = sample.value();
                            }
                        }
                    }
                }
                rows.forEach((timestamp, values) -> sink.accept(new CounterSeries(new Date(timestamp), Arrays.asList(values))));

                start = windowEnd + step;
            }
            markReachable(descriptor);
            return true;
        } catch (PrometheusException e) {
            markUnreachable(descriptor, e);
            return false;
        }
    }

    //=================================================================================================================
    // helpers
    //=================================================================================================================

    /**
     * Reads the process argument {@code lookback=<number><s|m|h|d>}.
     *
     * @return the lookback in milliseconds, 0 if none is configured
     */
    static long lookbackMillis(CounterDescriptor descriptor) {
        String arguments = descriptor.getAdditionalArguments();
        if (arguments == null) {
            return 0;
        }
        Matcher matcher = LOOKBACK.matcher(arguments);
        if (!matcher.find()) {
            return 0;
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> amount * 1_000;
            case "m" -> amount * 60_000;
            case "h" -> amount * 3_600_000;
            default -> amount * 86_400_000;
        };
    }

    private static long ceilToGrid(long time, long step) {
        long floor = (time / step) * step;
        return floor == time ? floor : floor + step;
    }

    private PrometheusApi api(CounterDescriptor descriptor) {
        if (api == null) {
            api = new PrometheusApi(descriptor.getNodeAddress(), descriptor.getUsername(), descriptor.getPassword());
        }
        return api;
    }

    private void markReachable(CounterDescriptor descriptor) {
        if (!reachable) {
            LOGGER.info("Prometheus at {} is reachable again.", descriptor.getNodeAddress());
        }
        reachable = true;
    }

    private void markUnreachable(CounterDescriptor descriptor, PrometheusException e) {
        if (reachable) {
            LOGGER.warn("Metric group '{}': {}", descriptor.getMetricGroupId(), e.getMessage());
        }
        reachable = false;
    }

    /**
     * Maps the series of each expression to its position in the value sets. The metric
     * definitions of a job are the resolved series: the expression they came from as
     * definition, the series name as label.
     */
    private static final class Columns {

        private final Map<String, Map<String, Integer>> columnsByExpression = new LinkedHashMap<>();

        private final Map<String, String> fallbackMetricByExpression = new HashMap<>();

        private final int count;

        Columns(MetricDefinition[] definitions) {
            count = definitions == null ? 0 : definitions.length;
            for (int i = 0; i < count; i++) {
                MetricDefinition definition = definitions[i];
                columnsByExpression.computeIfAbsent(definition.getDefinition(), e -> new HashMap<>())
                        .putIfAbsent(definition.getLabel(), i);
                fallbackMetricByExpression.putIfAbsent(definition.getDefinition(),
                        PrometheusSeriesName.parse(definition.getLabel()).metric());
            }
        }

        int count() {
            return count;
        }

        Iterable<String> expressions() {
            return columnsByExpression.keySet();
        }

        Integer columnOf(String expression, PrometheusApi.Series series) {
            String name = PrometheusSeriesName.of(fallbackMetricByExpression.get(expression), series.labels()).toString();
            return columnsByExpression.get(expression).get(name);
        }
    }
}
