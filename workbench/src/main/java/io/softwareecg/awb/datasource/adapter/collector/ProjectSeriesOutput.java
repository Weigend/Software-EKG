package io.softwareecg.awb.datasource.adapter.collector;

import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusDimensions;
import io.softwareecg.collector.source.prometheus.PrometheusCounterSource;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.api.TimeSeriesWriteService;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.source.prometheus.PrometheusSeriesName;
import io.softwareecg.collector.config.CollectorJobConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import io.softwareecg.awb.datasource.model.SeriesDimensions;
import io.softwareecg.awb.datasource.model.SeriesEnds;
/**
 * The output of the embedded collector: writes what the collector jobs of one data source
 * collect straight into the project's repository.
 * <p>
 * Every series is written under one fixed measurement, so each update appends to the same
 * series instead of starting a new one. The measurement belongs to the data source: two
 * sources of a project that monitor systems of the same names keep their series apart by it.
 * The values of an update go into the repository as one chunk per series; the repository
 * joins the chunks of a series when it is read.
 * <p>
 * Where a series stands in the analysis depends on the source. A Prometheus server holds many
 * systems, so system and process are read from the labels of each series
 * ({@link PrometheusDimensions}). Every other source is collected by one job per host and
 * process, which therefore names both; the metric is named by its label in the configuration.
 * <p>
 * Where collecting resumes is known <b>for each series</b>. The repository is asked where a
 * series ends when it is first met; from then on the output keeps track itself. A series that
 * is new - a system that joined - starts earlier than the ones beside it, and the job is told
 * to collect from the earliest of these points. What is delivered again for the series that
 * were already further is left out, so nothing is stored twice.
 * <p>
 * If the repository cannot take the values, the output does not hold on to them: it drops what
 * it buffered and reports the failure to the collector job. The job shows the problem in its
 * state, retries with growing pauses and collects again from where the stored data really
 * ends - the source still has the values. Memory therefore stays bounded however long the
 * repository is down.
 */
public final class ProjectSeriesOutput implements RepositoryFactory {

    /**
     * The measurement of the first data source of a project.
     */
    public static final String MEASUREMENT = "live";

    /**
     * A long first fill is written in several chunks, so that it does not have to fit into memory.
     */
    private static final int MAX_BUFFERED_VALUES = 200_000;

    private static final String NONE = "*";

    /** The end of a series is not known yet: the repository has to be asked. */
    private static final long UNKNOWN = -1;

    /** Nothing is stored for a series. */
    private static final long EMPTY = 0;

    /**
     * How long values of a source without history are gathered before they are written. Such a
     * source delivers one value set at a time and never asks for them to be written.
     */
    private static final long WRITE_INTERVAL = 10_000;

    /**
     * How far before the last look for new systems a series found since then may have begun.
     */
    private static final long NEW_SERIES_MARGIN = 60_000;

    private final String projectName;

    private final String measurement;

    private final TimeSeriesWriteService importService;

    private final SeriesEnds seriesEnds;

    private final LongConsumer onDataWritten;

    private final LongSupplier clock;

    /**
     * What each collector job (by its metric group) has written since this output exists. The
     * storage of a job is replaced whenever its series change; this outlives the replacement.
     */
    private final Map<String, JobProgress> progressByJob = new ConcurrentHashMap<>();

    /**
     * @param projectName   the project the series belong to
     * @param measurement   the measurement the series of this data source are stored under
     * @param importService writes into the project's repository
     * @param seriesEnds    tells where the stored values of a series end
     * @param onDataWritten called after values were written and committed, with the time of the
     *                      newest of them; lets the AWB refresh what it shows
     */
    public ProjectSeriesOutput(String projectName, String measurement, TimeSeriesWriteService importService,
                               SeriesEnds seriesEnds, LongConsumer onDataWritten) {
        this(projectName, measurement, importService, seriesEnds, onDataWritten, System::currentTimeMillis);
    }

    ProjectSeriesOutput(String projectName, String measurement, TimeSeriesWriteService importService,
                        SeriesEnds seriesEnds, LongConsumer onDataWritten, LongSupplier clock) {
        this.projectName = projectName;
        this.measurement = measurement == null || measurement.isBlank() ? MEASUREMENT : measurement;
        this.importService = importService;
        this.seriesEnds = seriesEnds;
        this.onDataWritten = onDataWritten;
        this.clock = clock;
    }

    @Override
    public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {
        String job = descriptor == null || descriptor.getMetricGroupId() == null ? "" : descriptor.getMetricGroupId();
        long lookInterval = 0;
        if (descriptor instanceof CollectorJobConfiguration configuration && configuration.isRefreshWildcards()) {
            lookInterval = configuration.getWildcardExpandRefreshInterval() + 2 * descriptor.getMetricFetchInterval();
        }
        boolean prometheus = !(descriptor instanceof CollectorJobConfiguration configuration)
                || PrometheusCounterSource.ID.equals(configuration.getCounterGroupType());
        // jobs of other sources exist once per host and process, so that is part of their identity
        String key = prometheus ? job : job + "|" + descriptor.getNodeName() + "|" + descriptor.getProcessLabel();
        return new Writer(progressByJob.computeIfAbsent(key, k -> new JobProgress()), lookInterval, prometheus);
    }

    /**
     * What one collector job has written through its storages so far.
     */
    private static final class JobProgress {

        /** The time of the newest value written and committed; 0 before the first write. */
        long newestWritten;

        /** When the storage before the current one was created; 0 if there was none. */
        long previousStorageCreated;
    }

    /**
     * The storage of one collector job for one set of series.
     */
    private final class Writer implements StaticTimeSeriesRepository {

        private final JobProgress progress;

        /**
         * A series that is new to this storage and to the repository cannot have begun before
         * this time, because the job did not find it when it last looked. 0 if nothing is known
         * about that - then the series is filled from the very beginning.
         */
        private final long newSeriesBegin;

        /** Whether the series are Prometheus series, which carry system and process in their labels. */
        private final boolean prometheus;

        /** When values were last written, for sources that do not ask for it. */
        private long lastWritten;

        private SeriesDimensions[] dimensions = new SeriesDimensions[0];

        private List<Value>[] buffers;

        /**
         * The time of the newest stored value of each series, {@link #EMPTY} if it has none and
         * {@link #UNKNOWN} until the repository was asked.
         */
        private long[] ends = new long[0];

        private int bufferedValues;

        Writer(JobProgress progress, long lookInterval, boolean prometheus) {
            this.progress = progress;
            this.prometheus = prometheus;
            long now = clock.getAsLong();
            // the first values of a job are written at once: they are what the first chart waits for
            this.lastWritten = 0;
            if (progress.newestWritten == 0) {
                newSeriesBegin = 0;     // the job starts: what is not stored is fetched in full
            } else if (lookInterval > 0) {
                newSeriesBegin = Math.max(0, now - lookInterval - NEW_SERIES_MARGIN);
            } else {
                newSeriesBegin = Math.max(0, progress.previousStorageCreated - NEW_SERIES_MARGIN);
            }
            progress.previousStorageCreated = now;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {
            dimensions = new SeriesDimensions[metricLabels.length];
            buffers = new List[metricLabels.length];
            ends = new long[metricLabels.length];
            for (int i = 0; i < metricLabels.length; i++) {
                dimensions[i] = prometheus
                        ? PrometheusDimensions.of(PrometheusSeriesName.parse(metricLabels[i]))
                        : new SeriesDimensions(timeSeriesHeader.getHostGroupId(), timeSeriesHeader.getHostName(),
                                timeSeriesHeader.getHostGroupId(), timeSeriesHeader.getProcessLabel(),
                                SeriesDimensions.metricGroupOf(metricLabels[i]), metricLabels[i]);
                buffers[i] = new ArrayList<>();
                ends[i] = UNKNOWN;
            }
        }

        @Override
        public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {
            resolveEnds();
            long time = captureTime.getTime();
            for (int i = 0; i < metricValues.length && i < buffers.length; i++) {
                // what a series already has is delivered again when another series of the job
                // lies further back; it must not be stored a second time
                // the sources that ask a system for its current values say "nothing read" with a
                // negative number; none of what they measure can be negative
                boolean nothingRead = !prometheus && metricValues[i] != null && metricValues[i] < 0;
                if (metricValues[i] != null && !nothingRead && time > ends[i]) {
                    buffers[i].add(new Value(time, metricValues[i]));
                    bufferedValues++;
                }
            }
            if (bufferedValues >= MAX_BUFFERED_VALUES
                    || (!prometheus && clock.getAsLong() - lastWritten >= WRITE_INTERVAL)) {
                flush();
            }
        }

        @Override
        public void flush() {
            lastWritten = clock.getAsLong();
            if (bufferedValues == 0) {
                return;
            }
            List<TimeSeries> chunks = new ArrayList<>();
            long newest = 0;
            for (int i = 0; i < buffers.length; i++) {
                if (!buffers[i].isEmpty()) {
                    chunks.add(series(dimensions[i], buffers[i]));
                    newest = Math.max(newest, buffers[i].get(buffers[i].size() - 1).getTimestamp());
                }
            }
            try {
                importService.add(chunks);
                importService.commit();
            } catch (RepositoryException | RuntimeException e) {
                // Do not keep the values: they would pile up for as long as the repository is
                // down. The job collects them again from the source once writing works - from
                // where the repository says the series end, since part of them may be stored.
                discardBuffers();
                Arrays.fill(ends, UNKNOWN);
                throw new IllegalStateException("Could not write to the repository of project '" + projectName
                        + "': " + rootMessage(e), e);
            }
            discardBuffers();
            // The value sets come in the order of time and each holds all series, so every
            // series is complete up to the newest value written - also one that had no value.
            for (int i = 0; i < ends.length; i++) {
                ends[i] = Math.max(ends[i], newest);
            }
            progress.newestWritten = Math.max(progress.newestWritten, newest);
            onDataWritten.accept(newest);
        }

        private void discardBuffers() {
            for (List<Value> buffer : buffers) {
                buffer.clear();
            }
            bufferedValues = 0;
        }

        /**
         * @return the earliest end of all series: from there on something is missing. {@code null}
         *         if a series has to be filled from the very beginning.
         */
        @Override
        public Date getLastCaptureTime() {
            if (ends.length == 0) {
                return null;
            }
            resolveEnds();
            long earliest = Long.MAX_VALUE;
            for (long end : ends) {
                earliest = Math.min(earliest, end);
            }
            return earliest <= EMPTY ? null : new Date(earliest);
        }

        /**
         * Asks the repository for the end of every series whose end is not known.
         */
        private void resolveEnds() {
            boolean anyUnknown = false;
            for (long end : ends) {
                anyUnknown |= end == UNKNOWN;
            }
            if (!anyUnknown) {
                return;
            }
            try {
                // one question first: where nothing is stored at all, no series needs to be asked for
                boolean nothingStored = importService.resolveLastUpdate(new TimeSeries(projectName, commonHostGroup(),
                        null, null, null, null, null, measurement, null, null, null, null)) <= 0;
                for (int i = 0; i < ends.length; i++) {
                    if (ends[i] != UNKNOWN) {
                        continue;
                    }
                    long stored = nothingStored ? EMPTY : seriesEnds.endOf(projectName, measurement, dimensions[i]);
                    ends[i] = stored > 0 ? stored : newSeriesBegin;
                }
            } catch (RepositoryException | RuntimeException e) {
                // "unknown" must not be mistaken for "empty": that would fill in from the start
                // again and store the existing values twice
                throw new IllegalStateException("Could not read where the data of project '" + projectName
                        + "' ends: " + rootMessage(e), e);
            }
        }

        /** The group of systems all series belong to, {@code null} if they belong to several. */
        private String commonHostGroup() {
            String hostGroup = dimensions[0].hostGroup();
            for (SeriesDimensions dimension : dimensions) {
                if (!dimension.hostGroup().equals(hostGroup)) {
                    return null;
                }
            }
            return hostGroup;
        }

        private String rootMessage(Throwable e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
        }

        private TimeSeries series(SeriesDimensions dimension, List<Value> values) {
            return new TimeSeries(projectName, dimension.hostGroup(), dimension.host(), NONE, NONE, NONE, NONE,
                    measurement, dimension.processGroup(), dimension.process(), dimension.metricGroup(),
                    dimension.metric(), new ArrayList<>(values));
        }
    }
}
