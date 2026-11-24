package io.softwareecg.awb.importer.adapter.prometheus;

import io.softwareecg.awb.importer.adapter.prometheus.GenericPrometheusMapper.DimensionalMetric;
import io.softwareecg.awb.importer.adapter.prometheus.client.PrometheusClient;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.ResultStatus;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.label.ListLabelResult;
import io.softwareecg.awb.projectcontext.api.TargetProjectConfig;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.api.TimeSeriesWriteService;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.importer.adapter.prometheus.client.SecurityContext;
import io.softwareecg.awb.importer.model.RawSeriesData;
import io.softwareecg.awb.importer.model.SeriesData;
import io.softwareecg.awb.timeseries.Value;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import io.softwareecg.awb.importer.framework.steps.mapper.RawToSeriesDataMapper;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.ListSeriesResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.MatrixSeries;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.QueryResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.RangeQuery;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.SeriesDefinition;
/**
 * A LocalProject import strategy that imports time series data (metadata AND values)
 * from a Prometheus server directly into the local EKG repository.
 * <p>
 * This differs from {@link PrometheusRemoteImportStrategy} which only imports metadata
 * and fetches values on-demand from Prometheus.
 * <p>
 * Uses {@link GenericPrometheusMapper} for deterministic dimension mapping with adaptive
 * MetricGroup distribution based on metric count analysis.
 *
 * @since 04.02.2026
 */
public class PrometheusLocalImportStrategy extends LocalProjectImportStrategy {

    private static final Logger LOGGER = EkgLogger.get();

    /**
     * Default time range for importing data: last 24 hours (used as fallback)
     */
    private static final long DEFAULT_IMPORT_HOURS = 24;

    /**
     * Maximum lookback period when importing "all" data: 10 years
     * This covers most realistic Prometheus retention periods.
     */
    private static final long MAX_LOOKBACK_HOURS = 10 * 365 * 24; // 10 years

    private static final DateTimeFormatter MEASUREMENT_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final TimeSeriesImportSource sourceRepository;

    private PrometheusClient prometheusClient;

    /**
     * The measurement label for this import, representing the import time range.
     * Set during readRawSeriesData() and used in getRawToSeriesMapper().
     */
    private String importMeasurementLabel;

    /**
     * Pre-analyzed GenericPrometheusMapper for optimal MetricGroup distribution.
     * Set during readRawSeriesData() phase.
     */
    private GenericPrometheusMapper prometheusMapper;

    /**
     * The TimeSeriesWriteService used for delta import to query the last import timestamp.
     * Set in {@link #readRawSeriesData(TargetProjectConfig, TimeSeriesWriteService)}.
     */
    private TimeSeriesWriteService importService;

    /**
     * The project configuration containing delta import settings.
     * Set in {@link #readRawSeriesData(TargetProjectConfig, TimeSeriesWriteService)}.
     */
    private TargetProjectConfig projectConfig;

    /**
     * Constructs a new PrometheusLocalImportStrategy.
     *
     * @param sourceRepository the source repository configuration (URL, auth credentials) as it
     *                         was in the import dialog when the import was started
     */
    public PrometheusLocalImportStrategy(TimeSeriesImportSource sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Override
    public Stream<RawSeriesData> readRawSeriesData(TargetProjectConfig project, TimeSeriesWriteService importService) {
        // needed in calculateImportTimeRange() to ask for the last imported timestamp
        this.projectConfig = project;
        this.importService = importService;

        if (sourceRepository == null) {
            LOGGER.error("No source repository configuration available for Prometheus import");
            return Stream.empty();
        }

        setupPrometheusClient(sourceRepository);

        LOGGER.info("Starting Prometheus local import from {}", sourceRepository.getRepositoryUrl());

        // List all available metrics
        ListLabelResult result = prometheusClient.listMetrics();

        if (ResultStatus.SUCCESS != result.getStatus()) {
            LOGGER.error("Failed to retrieve metrics from {}", sourceRepository.getRepositoryUrl());
            return Stream.empty();
        }

        if (result.getData() == null || result.getData().length == 0) {
            LOGGER.warn("No metrics returned from Prometheus API");
            return Stream.empty();
        }

        final int totalAmountMetrics = result.getData().length;
        final AtomicInteger currentMetricNumber = new AtomicInteger(0);

        LOGGER.info("Found {} metrics to import from Prometheus", totalAmountMetrics);

        notifyImportStarted("Importing " + totalAmountMetrics + " metrics from Prometheus (with values)");

        PrometheusMetricFilter filter = new PrometheusMetricFilter();

        // Calculate time range for data import based on configuration
        long[] timeRange = calculateImportTimeRange();
        long startTime = timeRange[0];
        long endTime = timeRange[1];

        // For "all" range, probe Prometheus for the actual earliest data point
        // to get a meaningful measurement label instead of a theoretical 10-year lookback
        String rangeConfig = PrometheusModuleConfig.getLocalImportRange();
        boolean isDelta = projectConfig != null && projectConfig.isDeltaImport();
        if (!isDelta && "all".equalsIgnoreCase(rangeConfig)) {
            long actualStart = probeActualDataStart(result.getData(), filter, startTime, endTime);
            if (actualStart > startTime) {
                startTime = actualStart;
                LOGGER.info("Adjusted start time from Prometheus actual data range: {}", Instant.ofEpochMilli(actualStart));
            }
        }

        // Create measurement label from effective time range
        if (isDelta) {
            importMeasurementLabel = "Delta " + MEASUREMENT_FORMATTER.format(Instant.ofEpochMilli(startTime)) 
                    + " - " + MEASUREMENT_FORMATTER.format(Instant.ofEpochMilli(endTime));
        } else {
            importMeasurementLabel = MEASUREMENT_FORMATTER.format(Instant.ofEpochMilli(startTime)) 
                    + " - " + MEASUREMENT_FORMATTER.format(Instant.ofEpochMilli(endTime));
        }

        LOGGER.info("Import time range: {} to {} ({} hours) - Measurement: {} - Delta: {}", 
                Instant.ofEpochMilli(startTime), 
                Instant.ofEpochMilli(endTime),
                (endTime - startTime) / (1000 * 60 * 60),
                importMeasurementLabel,
                isDelta);

        // Capture as final for use in lambda
        final long effectiveStartTime = startTime;
        final long effectiveEndTime = endTime;

        // Phase 1: Collect all series definitions first for analysis
        LOGGER.info("Phase 1: Collecting series definitions for metric analysis...");
        List<SeriesDefinition> allSeriesDefinitions = new ArrayList<>();
        
        String[] metricNames = result.getData();
        for (int i = 0; i < metricNames.length; i++) {
            String metricName = metricNames[i];
            if (!filter.test(metricName)) {
                continue;
            }
            // Skip _info metrics - they don't have meaningful time series values
            if (metricName.endsWith("_info")) {
                continue;
            }
            
            ListSeriesResult seriesResult = prometheusClient.listSeriesForMetric(metricName);
            if (seriesResult != null && seriesResult.getData() != null) {
                allSeriesDefinitions.addAll(Arrays.asList(seriesResult.getData()));
            }
            
            if ((i + 1) % 50 == 0) {
                LOGGER.info("Collected series for {} of {} metrics", i + 1, totalAmountMetrics);
            }
        }
        
        LOGGER.info("Collected {} series definitions total", allSeriesDefinitions.size());

        // Phase 2: Analyze all series for optimal MetricGroup distribution
        LOGGER.info("Phase 2: Analyzing metrics for optimal grouping...");
        prometheusMapper = new GenericPrometheusMapper(100);
        
        List<RawSeriesData> allRawSeriesData = new ArrayList<>();
        for (SeriesDefinition sd : allSeriesDefinitions) {
            RawSeriesData rawData = new RawSeriesData(sd.getMetricName());
            for (String label : sd.getLabels()) {
                rawData.addLabel(label, sd.getLabelValue(label));
            }
            allRawSeriesData.add(rawData);
            prometheusMapper.analyze(rawData);
        }
        
        LOGGER.info("Analysis complete: {} metrics analyzed", 
                prometheusMapper.getAnalyzedCount());

        // Phase 3: Return stream that fetches values on-demand
        LOGGER.info("Phase 3: Fetching time series values...");
        return allSeriesDefinitions.stream()
                .peek(sd -> {
                    int current = currentMetricNumber.incrementAndGet();
                    notifyProgress(current, allSeriesDefinitions.size());
                    if (current % 100 == 0) {
                        LOGGER.info("Fetching values {} of {}: {}", current, allSeriesDefinitions.size(), sd.getMetricName());
                    }
                })
                .map(seriesDefinition -> toRawDataWithValues(seriesDefinition, effectiveStartTime, effectiveEndTime));
    }

    /**
     * Returns a mapper that converts RawSeriesData to SeriesData using the
     * deterministic {@link PrometheusMapper} for dimension mapping.
     * <p>
     * The PrometheusMapper provides adaptive MetricGroup distribution based on
     * pre-analysis of all metrics in the Prometheus server:
     * <ul>
     *   <li>ProcessGroup = job label</li>
     *   <li>Process = full instance URL</li>
     *   <li>Host = hostname extracted from instance</li>
     *   <li>HostGroup = "cluster"</li>
     *   <li>MetricGroup = job + adaptive prefix extension</li>
     *   <li>Metric = metric name with label suffixes</li>
     * </ul>
     */
    @Override
    public RawToSeriesDataMapper<RawSeriesData, SeriesData> getRawToSeriesMapper(String projectName) {
        // Use the pre-analyzed mapper from readRawSeriesData phase
        if (prometheusMapper == null) {
            LOGGER.warn("GenericPrometheusMapper not initialized - creating new instance without analysis");
            prometheusMapper = new GenericPrometheusMapper();
        }
        
        LOGGER.info("Using GenericPrometheusMapper for dimension mapping ({} metrics analyzed)", prometheusMapper.getAnalyzedCount());
        
        final AtomicInteger counter = new AtomicInteger(0);
        
        return rawSeriesData -> {
            int count = counter.incrementAndGet();
            
            // Log first few metrics for debugging
            if (count <= 3) {
                LOGGER.debug("[{}] Mapping metric: {} with labels: {}", 
                        count, rawSeriesData.getMetricName(), rawSeriesData.getSeriesLabels());
            }
            
            SeriesData seriesData = mapUsingPrometheusMapper(projectName, rawSeriesData);
            
            if (count <= 3 && seriesData != null) {
                LOGGER.debug("[{}] Mapped to: hostgroup={}, host={}, metricgroup={}, metric={}", 
                        count, seriesData.getHostGroup(), seriesData.getHost(), 
                        seriesData.getMetricGroup(), seriesData.getMetricName());
            }
            
            return seriesData;
        };
    }

    /**
     * Maps raw series data using the deterministic PrometheusMapper.
     */
    private SeriesData mapUsingPrometheusMapper(String projectName, RawSeriesData rawSeriesData) {
        DimensionalMetric dm = prometheusMapper.transform(rawSeriesData);
        
        // For _info metrics, dm will be null
        if (dm == null) {
            LOGGER.debug("Skipping _info metric: {}", rawSeriesData.getMetricName());
            return null;
        }
        
        Map<String, String> labels = rawSeriesData.getSeriesLabels();
        
        // Use the import time range as measurement to identify partial imports
        String measurement = importMeasurementLabel != null ? importMeasurementLabel : "*";
        
        return new SeriesData(
                projectName,
                dm.hostGroup(),                                    // HostGroup: "cluster"
                dm.host(),                                         // Host: extracted from instance
                labels.getOrDefault("namespace", "*"),             // Namespace: from label
                labels.getOrDefault("service", "*"),               // Service: from label
                labels.getOrDefault("pod", "*"),                   // Pod: from label
                labels.getOrDefault("container", "*"),             // Container: from label
                measurement,                                       // Measurement: import time range
                dm.processGroup(),                                 // ProcessGroup: job name
                dm.process(),                                      // Process: full instance URL
                dm.metricGroup(),                                  // MetricGroup: adaptive grouping
                dm.metric(),                                       // Metric: with label suffixes
                rawSeriesData.getValues()
        );
    }

    //=================================================================================================================
    // internal helper logic
    //=================================================================================================================

    /**
     * Configures the Prometheus client with connection details from the source repository.
     */
    private void setupPrometheusClient(TimeSeriesImportSource sourceRepository) {
        prometheusClient = new PrometheusClient();
        prometheusClient.bindToAddress(sourceRepository.getRepositoryUrl());

        SecurityContext securityContext = new SecurityContext();
        securityContext.setUserName(sourceRepository.getUsername());
        securityContext.setPassword(sourceRepository.getPassword());
        securityContext.setResourceAuthType(sourceRepository.getAuthType());
        securityContext.setSsoOAuthServer(sourceRepository.getOAuthServerUrl());

        prometheusClient.setSecurityContext(securityContext);
    }

    /**
     * Converts a SeriesDefinition to RawSeriesData and fetches the actual time series values
     * from Prometheus.
     */
    private RawSeriesData toRawDataWithValues(SeriesDefinition seriesDefinition, long startTime, long endTime) {
        RawSeriesData rawSeriesData = new RawSeriesData(seriesDefinition.getMetricName());

        // Copy all labels
        for (String label : seriesDefinition.getLabels()) {
            rawSeriesData.addLabel(label, seriesDefinition.getLabelValue(label));
        }

        // Fetch actual values from Prometheus
        Value[] values = fetchSeriesValues(seriesDefinition, startTime, endTime);
        rawSeriesData.setValues(Arrays.asList(values));

        return rawSeriesData;
    }

    /**
     * Fetches time series values for a specific series from Prometheus.
     */
    private Value[] fetchSeriesValues(SeriesDefinition seriesDefinition, long startTime, long endTime) {
        try {
            RangeQuery rangeQuery = RangeQuery.Builder
                    .newRangeQuery()
                    .forMetric(seriesDefinition.getMetricName())
                    .withStartEndTimeStamp(startTime, endTime)
                    .build();

            // Add all labels as filter parameters
            for (String label : seriesDefinition.getLabels()) {
                if (!"__name__".equals(label)) {
                    rangeQuery.addLabelFilterParam(label, seriesDefinition.getLabelValue(label));
                }
            }

            QueryResult queryResult = prometheusClient.queryRange(rangeQuery);

            if (queryResult.getData() == null) {
                return new Value[0];
            }

            MatrixSeries[] matrixSeriesArray = queryResult.getData().getResult();

            if (matrixSeriesArray.length == 0) {
                return new Value[0];
            }

            // Take first result (should be unique due to label filtering)
            double[][] seriesData = matrixSeriesArray[0].getValues();
            Value[] resultValues = new Value[seriesData.length];

            for (int i = 0; i < seriesData.length; i++) {
                resultValues[i] = new Value((long) (seriesData[i][0] * 1000), seriesData[i][1]);
            }

            return resultValues;

        } catch (Exception e) {
            LOGGER.warn("Failed to fetch values for metric {}: {}", seriesDefinition.getMetricName(), e.getMessage());
            return new Value[0];
        }
    }

    /**
     * Notifies progress listeners about the import progress.
     */
    private void notifyProgress(int current, int total) {
        double progress = (double) current / total;
        publish(new io.softwareecg.wfx.platform.api.events.ProgressEvent(
                "Importing metric " + current + " of " + total + "...", progress, this));
    }

    /**
     * Calculates the import time range based on configuration and delta import setting.
     * <p>
     * When delta import is enabled ({@link TargetProjectConfig#isDeltaImport()}):
     * <ul>
     *   <li>Queries Solr for the global latest TS_STOP timestamp across all series in the project</li>
     *   <li>Uses that timestamp as the start time for the Prometheus query</li>
     *   <li>Falls back to normal configuration-based range if no existing data is found</li>
     * </ul>
     * <p>
     * When delta import is disabled, falls back to the configured import range (hours or "all").
     * 
     * @return array with [startTime, endTime] in milliseconds
     */
    private long[] calculateImportTimeRange() {
        long endTime = Instant.now().toEpochMilli();
        long startTime;

        // Check if delta import is requested
        if (projectConfig != null && projectConfig.isDeltaImport() && importService != null) {
            startTime = resolveDeltaStartTime();
            if (startTime > 0) {
                LOGGER.info("Delta import: using last import timestamp {} as start time",
                        Instant.ofEpochMilli(startTime));
                return new long[]{startTime, endTime};
            } else {
                LOGGER.info("Delta import requested but no existing data found for project '{}' - falling back to configured range",
                        projectConfig.getProjectName());
            }
        }

        // Check if user selected an import range in the dialog
        String dialogRange = (projectConfig != null) ? projectConfig.getImportRange() : null;

        if (dialogRange != null && !TargetProjectConfig.IMPORT_RANGE_ALL.equalsIgnoreCase(dialogRange)) {
            // Parse the dialog selection (e.g. "1d", "2d", "3d", "5d", "10d")
            long hours = parseDialogRange(dialogRange);
            if (hours > 0) {
                startTime = Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli();
                LOGGER.info("Import range from dialog: '{}' = {} hours", dialogRange, hours);
                return new long[]{startTime, endTime};
            }
        }

        // Fall back to config file range
        String rangeConfig = PrometheusModuleConfig.getLocalImportRange();

        if ("all".equalsIgnoreCase(rangeConfig)) {
            // Import all available data - use maximum lookback
            startTime = Instant.now().minus(MAX_LOOKBACK_HOURS, ChronoUnit.HOURS).toEpochMilli();
            LOGGER.info("Import range configured as 'all' - using {} year lookback", MAX_LOOKBACK_HOURS / (365 * 24));
        } else {
            try {
                long hours = Long.parseLong(rangeConfig);
                startTime = Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli();
                LOGGER.info("Import range configured as {} hours", hours);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid import.range configuration '{}', using default {} hours", 
                        rangeConfig, DEFAULT_IMPORT_HOURS);
                startTime = Instant.now().minus(DEFAULT_IMPORT_HOURS, ChronoUnit.HOURS).toEpochMilli();
            }
        }

        return new long[]{startTime, endTime};
    }

    /**
     * Parses the dialog import range selection (e.g. "1d", "2d", "10d") into hours.
     *
     * @param dialogRange the range string from the ComboBox
     * @return the number of hours, or -1 if parsing fails
     */
    private long parseDialogRange(String dialogRange) {
        if (dialogRange == null || dialogRange.isBlank()) {
            return -1;
        }
        String trimmed = dialogRange.trim().toLowerCase();
        if (trimmed.endsWith("d")) {
            try {
                long days = Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
                return days * 24;
            } catch (NumberFormatException e) {
                LOGGER.warn("Cannot parse dialog import range: '{}'", dialogRange);
                return -1;
            }
        }
        return -1;
    }

    /**
     * Probes Prometheus to find the actual earliest data timestamp by querying a single metric.
     * This avoids showing a 10-year lookback in the measurement label when the actual data
     * only spans a few days/weeks (typical Prometheus retention).
     *
     * @param metricNames all available metric names
     * @param filter the metric filter
     * @param queryStart the query start time (theoretical max lookback)
     * @param queryEnd the query end time
     * @return the actual earliest data timestamp, or queryStart if probe fails
     */
    private long probeActualDataStart(String[] metricNames, PrometheusMetricFilter filter, long queryStart, long queryEnd) {
        try {
            // Find the first metric that passes the filter
            String probeMetric = null;
            for (String metricName : metricNames) {
                if (filter.test(metricName) && !metricName.endsWith("_info")) {
                    probeMetric = metricName;
                    break;
                }
            }
            if (probeMetric == null) {
                return queryStart;
            }

            // Query just this one metric to find the actual earliest data point
            RangeQuery rangeQuery = RangeQuery.Builder
                    .newRangeQuery()
                    .forMetric(probeMetric)
                    .withStartEndTimeStamp(queryStart, queryEnd)
                    .build();

            QueryResult queryResult = prometheusClient.queryRange(rangeQuery);
            if (queryResult.getData() == null) {
                return queryStart;
            }

            MatrixSeries[] results = queryResult.getData().getResult();
            if (results.length == 0) {
                return queryStart;
            }

            double[][] values = results[0].getValues();
            if (values.length == 0) {
                return queryStart;
            }

            // First value's timestamp is the actual earliest data point
            long actualStart = (long) (values[0][0] * 1000);
            LOGGER.info("Probe metric '{}' reveals actual data start: {}", probeMetric, Instant.ofEpochMilli(actualStart));
            return actualStart;

        } catch (Exception e) {
            LOGGER.warn("Failed to probe actual data start from Prometheus: {}", e.getMessage());
            return queryStart;
        }
    }

    /**
     * Queries Solr for the global latest timestamp (TS_STOP) across all series in the project.
     * This is used as the start time for delta imports to avoid re-importing existing data.
     * <p>
     * Creates a minimal TimeSeries with only the project name set, so that
     * {@link TimeSeriesWriteService#resolveLastUpdate(io.softwareecg.awb.timeseries.TimeSeries)}
     * matches all series in the project and returns the latest end timestamp.
     *
     * @return the last import timestamp in millis, or -1 if no existing data
     */
    private long resolveDeltaStartTime() {
        try {
            // Create a minimal SeriesData with only the project name to query globally.
            // resolveLastUpdate uses addWildcardExpressionIfNotNull which skips null/blank fields,
            // so only PROJECT_NAME will be used as filter criterion - giving us the global max TS_STOP.
            SeriesData globalQuery = new SeriesData(projectConfig.getProjectName(),
                    null, null, null, null, null, null, null, null, null, null, null);

            long lastUpdate = importService.resolveLastUpdate(globalQuery);

            if (lastUpdate > 0) {
                LOGGER.info("Delta import: found last import timestamp {} for project '{}'",
                        Instant.ofEpochMilli(lastUpdate), projectConfig.getProjectName());
            } else {
                LOGGER.info("Delta import: no existing data found for project '{}'",
                        projectConfig.getProjectName());
            }

            return lastUpdate;

        } catch (RepositoryException e) {
            LOGGER.warn("Delta import: failed to query last import timestamp for project '{}': {}",
                    projectConfig.getProjectName(), e.getMessage());
            return -1;
        }
    }
}
