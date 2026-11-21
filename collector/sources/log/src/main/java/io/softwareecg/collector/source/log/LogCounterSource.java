package io.softwareecg.collector.source.log;

import org.slf4j.LoggerFactory;
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Turns the lines of a log file into time series - what an operator would otherwise look for
 * with {@code tail -f} and {@code grep}, on every host, and count by hand.
 * <p>
 * In the collector configuration the {@code source} of the process is the log file and every
 * metric is a regular expression. The series of a metric is the number of lines per interval
 * that contain it, or - with a group named {@code sum}, {@code avg}, {@code min}, {@code max} or
 * {@code last} - that aggregate of the numbers the group captures ({@link LogMetric}):
 * <pre>
 *   metricGroups:
 *     checkout-log:
 *       type: log
 *       metrics:
 *         - metric: 'user login succeeded'
 *           label: logins
 *         - metric: 'transaction failed.*code=TX-41'
 *           label: 'failed TX-41'
 *         - metric: 'request took (?&lt;max&gt;\d+) ms'
 *           label: 'slowest request ms'
 *   processGroups:
 *     checkout:
 *       - name: checkout
 *         source: '/var/log/checkout/checkout.log'
 *         args: 'lookback=24h'
 * </pre>
 * The file is read on the host of the job: directly if that is this machine ({@code localhost}
 * or {@code 127.0.0.1}), over SSH with the login of the host otherwise. {@code ${HOSTNAME}} in
 * the name of the file stands for the name of the host.
 * <p>
 * The events lie on the time grid of the fetch interval and carry the time of their line, not
 * the time they were read ({@link LogFollower}, {@link LineTimestamps}). The arguments of the
 * process ({@link LogArguments}) say how far back the content the file already has is read
 * ({@code lookback}) and how its timestamps are written ({@code timestamp}, {@code zone}).
 * <p>
 * The source delivers the intervals since the last one collected
 * ({@link CounterHistorySupport}), so the first fill, the regular update and catching up after
 * the host could not be reached are the same call.
 */
public class LogCounterSource implements CounterSource, CounterWildcardSupport, CounterHistorySupport {

    /**
     * The id of the source in a configuration.
     */
    public static final String COUNTER_SOURCE_ID = "log";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCounterSource.class);

    /**
     * A job asks for a new source object now and then - after every failed reading, for one -
     * but where its file was read up to has to survive that. It is kept for the thread of the
     * job, which is what stays: it ends with the job, and no other job shares it.
     */
    private static final ThreadLocal<Following> FOLLOWING = new ThreadLocal<>();

    private final LongSupplier clock;

    public LogCounterSource() {
        this(System::currentTimeMillis);
    }

    LogCounterSource(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    public String getId() {
        return COUNTER_SOURCE_ID;
    }

    /**
     * There is nothing to expand: a metric is one series. The job asks this before it collects,
     * and it is where it learns that the file cannot be read - it then says so in its state and
     * keeps trying.
     *
     * @throws IllegalArgumentException if a metric is not a regular expression or an argument of
     *                                  the process is not usable
     * @throws IllegalStateException    if the configuration lacks what reading the file needs
     */
    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {
        String problem = follower(descriptor).check();
        if (problem != null) {
            return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE, problem);
        }
        return new ExpandWildcardResponse(descriptor.getMetricDefinitions().clone());
    }

    @Override
    public void addCounterListener(CounterSourceListener sourceListener) {
        // the series of a job never change
    }

    @Override
    public boolean queryCounterSince(CounterDescriptor descriptor, Date since, Consumer<CounterSeries> sink) {
        if (descriptor.getMetricDefinitions().length == 0) {
            return false;
        }
        long interval = Math.max(1_000, descriptor.getMetricFetchInterval());
        return follower(descriptor).poll(clock.getAsLong(), interval, since, sink);
    }

    /**
     * @return the newest interval that is over and was not delivered yet; no values if there is
     *         none or the file cannot be read. A job does not call this: it collects all intervals
     *         with {@link #queryCounterSince}.
     */
    @Override
    public CounterSeries queryCounter(CounterDescriptor descriptor) {
        List<CounterSeries> delivered = new ArrayList<>();
        if (!queryCounterSince(descriptor, null, delivered::add) || delivered.isEmpty()) {
            return new CounterSeries(List.of());
        }
        return delivered.get(delivered.size() - 1);
    }

    // ---------------------------------------------------------------------------------------------
    // the file a job follows
    // ---------------------------------------------------------------------------------------------

    /**
     * @param configuration everything the follower was made from; a job whose configuration is
     *                      another one gets a new follower
     */
    private record Following(String configuration, LogFollower follower) {
    }

    private LogFollower follower(CounterDescriptor descriptor) {
        String configuration = configurationOf(descriptor);
        Following following = FOLLOWING.get();
        if (following == null || !following.configuration().equals(configuration)) {
            following = new Following(configuration, createFollower(descriptor));
            FOLLOWING.set(following);
        }
        return following.follower();
    }

    /**
     * Forgets where the file of the current thread was read up to.
     */
    static void forget() {
        FOLLOWING.remove();
    }

    private static String configurationOf(CounterDescriptor descriptor) {
        StringBuilder text = new StringBuilder();
        for (String part : new String[]{descriptor.getNodeGroupId(), descriptor.getNodeName(),
                descriptor.getNodeAddress(), descriptor.getUsername(), descriptor.getMetricGroupId(),
                descriptor.getProcessLabel(), descriptor.getProcessMetricsSource(),
                descriptor.getAdditionalArguments()}) {
            text.append(part).append('\u0000');
        }
        for (MetricDefinition metric : descriptor.getMetricDefinitions()) {
            text.append(metric.getDefinition()).append('\u0000');
        }
        return text.toString();
    }

    private LogFollower createFollower(CounterDescriptor descriptor) {
        String source = descriptor.getProcessMetricsSource();
        if (source == null || source.isBlank()) {
            throw new IllegalStateException("A process read by the source log needs the log file as its 'source'.");
        }
        String host = descriptor.getNodeAddress();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("The host of a log file has no address.");
        }
        String file = descriptor.getNodeName() == null ? source
                : source.replace("${HOSTNAME}", descriptor.getNodeName());

        LogArguments arguments = LogArguments.parse(descriptor.getAdditionalArguments());

        List<LogMetric> metrics = new ArrayList<>();
        for (MetricDefinition definition : descriptor.getMetricDefinitions()) {
            metrics.add(LogMetric.of(definition.getDefinition()));
        }
        LineTimestamps timestamps = new LineTimestamps(arguments.timestampPattern(), arguments.zone(), clock);

        LogFileReader reader;
        if (HostNameUtil.isLocalhost(host)) {
            reader = new LocalLogFileReader(Path.of(file), arguments.charset());
        } else {
            if (isBlank(descriptor.getUsername())) {
                throw new IllegalStateException("The source log reads " + file + " on " + host + " over SSH and "
                        + "needs a user for it: 'username' of the host or of its group.");
            }
            if (isBlank(descriptor.getPassword())) {
                throw new IllegalStateException("The source log reads " + file + " on " + host + " over SSH and "
                        + "needs a password for it: 'password' of the host or of its group.");
            }
            reader = new SshLogFileReader(host, arguments.port(), descriptor.getUsername(), descriptor.getPassword(),
                    file, arguments.charset());
        }
        LOGGER.info("Following the log file {} for {} metric(s){}.", reader.describe(), metrics.size(),
                arguments.lookbackMillis() > 0 ? ", with what it already has of the last "
                        + arguments.lookbackMillis() / 60_000 + " minutes" : "");
        return new LogFollower(reader, metrics, timestamps, arguments.lookbackMillis());
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
