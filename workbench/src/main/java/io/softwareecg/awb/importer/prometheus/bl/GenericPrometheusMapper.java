package io.softwareecg.awb.importer.prometheus.bl;

import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic Prometheus to Dimensional Model Mapper
 * <p>
 * Transformiert Prometheus-Metriken in das dimensionale Schema der Software-EKG Workbench.
 * Der Mapper sollte in beliebigen Prometheus Installationen funktionieren, 
 * da er sich an den Standard-Labels "job" und "instance" orientiert und custom labels in die MetricGroup/Metric 
 * verkettet.
 * 
 * EKG Schema:
 * ProcessGroup | Process | HostGroup | Host | MetricGroup | Metric
 * <p>
 * Regeln:
 * <ol>
 *   <li>ProcessGroup = job (1:1)</li>
 *   <li>Process = instance URL (vollständig)</li>
 *   <li>Host = erster Teil von instance (vor dem Punkt oder Doppelpunkt)</li>
 *   <li>HostGroup = "cluster" (konstant)</li>
 *   <li>MetricGroup = erstes Segment des Metriknamens (nach job-Präfix Strip)</li>
 *   <li>Metric = Rest des Metriknamens + Labels als suffix</li>
 * </ol>
 * <p>
 * Beispiel: node_cpu_seconds_total mit job=node, cpu=0, mode=idle
 * → ProcessGroup=node, MetricGroup=cpu, Metric=seconds_total_cpu_0_mode_idle
 * <p>
 * Verwendung:
 * <pre>
 * // Analysephase: Alle Metriken sammeln und analysieren
 * GenericPrometheusMapper mapper = new GenericPrometheusMapper(100);
 * seriesDataList.forEach(mapper::analyze);
 * 
 * // Transformationsphase: Dimensionen zuweisen
 * DimensionalMetric result = mapper.transform(rawSeriesData);
 * </pre>
 * Hinweis: In der ersten Phase werden die Metriken mit den Labels multipliziert, daher erscheinen
 * erst weniger Metriken in der EKG Statuszeile, anschliessend werden die Metriken richtig gelesen und die
 * Statuszeile zeigt die tatsächliche Anzahl der Metriken an.
 *
 * @since 7.0
 */
public class GenericPrometheusMapper {

    private static final Logger LOGGER = EkgLogger.get();

    // ========================================================================
    // Konfiguration
    // ========================================================================

    /** Labels die ignoriert werden (redundant oder konstant) */
    private static final Set<String> IGNORE_LABELS = Set.of(
        "base_url", "cluster_id", "core", "fstype", "__name__", "METRIC_NAME"
    );

    /** Standard-Labels (werden zu ProcessGroup/Process/Host) */
    private static final Set<String> STANDARD_LABELS = Set.of("job", "instance");

    /** Quantil → Suffix Mapping */
    private static final Map<String, String> QUANTILE_SUFFIX = Map.of(
        "0.5", "_p50",
        "0.75", "_p75",
        "0.9", "_p90",
        "0.95", "_p95",
        "0.99", "_p99",
        "0.999", "_p999",
        "1", "_p100"
    );

    /** Anzahl analysierter Metriken */
    private int analyzedCount = 0;

    // ========================================================================
    // Datenstrukturen
    // ========================================================================

    /**
     * Ausgabe: Eine transformierte dimensionale Metrik.
     */
    public record DimensionalMetric(
        String processGroup,
        String process,
        String hostGroup,
        String host,
        String metricGroup,
        String metric
    ) {}

    // ========================================================================
    // Konstruktor
    // ========================================================================

    /**
     * Erstellt einen neuen Mapper.
     * 
     * @param threshold ignoriert (nur für API-Kompatibilität)
     * @deprecated Threshold wird nicht mehr verwendet. Nutze {@link #GenericPrometheusMapper()}.
     */
    @Deprecated
    public GenericPrometheusMapper(int threshold) {
    }

    /**
     * Erstellt einen neuen Mapper.
     */
    public GenericPrometheusMapper() {
    }

    // ========================================================================
    // Analyse-Phase
    // ========================================================================

    /**
     * Analysiert eine Metrik und aktualisiert die Präfix-Statistiken.
     * <p>
     * MUSS für alle Metriken aufgerufen werden BEVOR transform() verwendet wird!
     *
     * @param rawSeriesData die Rohdaten der Serie
     */
    public void analyze(RawSeriesData rawSeriesData) {
        String metricName = rawSeriesData.getMetricName();
        
        if (isInfoMetric(metricName)) {
            return;
        }

        analyzedCount++;
    }

    /**
     * Analysiert mehrere Metriken.
     *
     * @param seriesDataList Liste der zu analysierenden Serien
     */
    public void analyzeAll(Collection<RawSeriesData> seriesDataList) {
        for (RawSeriesData rawSeriesData : seriesDataList) {
            analyze(rawSeriesData);
        }
        LOGGER.info("Analysed {} metrics", analyzedCount);
    }

    /**
     * Setzt die Analyse zurück für einen neuen Import.
     */
    public void reset() {
        analyzedCount = 0;
    }

    // ========================================================================
    // Transformation
    // ========================================================================

    /**
     * Transformiert eine Prometheus-Metrik in das dimensionale Schema.
     * 
     * @param rawSeriesData die Rohdaten der Serie
     * @return Die transformierte dimensionale Metrik, oder null wenn ignoriert
     */
    public DimensionalMetric transform(RawSeriesData rawSeriesData) {
        String metricName = rawSeriesData.getMetricName();
        Map<String, String> labels = rawSeriesData.getSeriesLabels();
        
        // Info-Metriken ignorieren
        if (isInfoMetric(metricName)) {
            return null;
        }

        String job = labels.getOrDefault("job", "");
        String instance = labels.getOrDefault("instance", "");

        // Job-Präfix entfernen
        String stripped = stripJobPrefix(metricName, job);

        // MetricGroup = erstes Segment nach Job-Prefix-Strip
        // Rest = Metrikname
        String[] split = splitFirstSegment(stripped);
        String metricGroup = split[0];
        String baseMetric = split[1];

        // Label-Suffix bauen
        String[] suffixes = buildLabelSuffix(labels);
        String labelSuffix = suffixes[0];
        String quantileSuffix = suffixes[1];

        // Metric zusammenbauen
        String finalMetric = baseMetric;
        if (!labelSuffix.isEmpty()) {
            finalMetric = finalMetric + "_" + labelSuffix;
        }
        if (quantileSuffix != null) {
            finalMetric = finalMetric + quantileSuffix;
        }

        return new DimensionalMetric(
            job,
            instance,
            "cluster",
            extractHost(instance),
            metricGroup,
            finalMetric
        );
    }

    /**
     * Transformiert mehrere Metriken.
     *
     * @param seriesDataList Liste der zu transformierenden Serien
     * @return Liste aller transformierten Metriken
     */
    public List<DimensionalMetric> transformAll(Collection<RawSeriesData> seriesDataList) {
        return seriesDataList.stream()
            .map(this::transform)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // ========================================================================
    // Hilfsmethoden
    // ========================================================================

    /**
     * Prüft ob es eine _info Metrik ist (werden ignoriert).
     */
    private boolean isInfoMetric(String metricName) {
        return metricName != null && metricName.endsWith("_info");
    }

    /**
     * Entfernt das Job-Präfix vom Metriknamen wenn vorhanden.
     */
    private String stripJobPrefix(String metricName, String job) {
        if (job != null && !job.isEmpty() && metricName.startsWith(job + "_")) {
            return metricName.substring(job.length() + 1);
        }
        return metricName;
    }

    /**
     * Extrahiert den Host aus der instance URL.
     * <p>
     * node1.cloud.local:9100 → node1
     */
    private String extractHost(String instance) {
        if (instance == null || instance.isEmpty()) {
            return "*";
        }
        // node1.cloud.local:9100 → node1
        int dotIndex = instance.indexOf('.');
        if (dotIndex > 0) {
            return instance.substring(0, dotIndex);
        }
        // Falls kein Punkt: vor dem Doppelpunkt
        int colonIndex = instance.indexOf(':');
        if (colonIndex > 0) {
            return instance.substring(0, colonIndex);
        }
        return instance;
    }

    /**
     * Teilt den Metriknamen in MetricGroup (erstes Segment) und Rest.
     * <p>
     * Das erste Underscore-Segment wird zur MetricGroup, der Rest zum Metriknamen.
     * <p>
     * Beispiel: "cpu_seconds_total" → MetricGroup="cpu", Metric="seconds_total"
     * Beispiel: "gc_collection_time" → MetricGroup="gc", Metric="collection_time"
     * 
     * @return Array mit [metricGroup, remainingMetric]
     */
    private String[] splitFirstSegment(String strippedMetric) {
        int underscoreIndex = strippedMetric.indexOf('_');
        
        if (underscoreIndex < 0) {
            // Kein Underscore: gesamter Name ist MetricGroup und Metrik
            return new String[]{strippedMetric, strippedMetric};
        }

        String metricGroup = strippedMetric.substring(0, underscoreIndex);
        String remaining = strippedMetric.substring(underscoreIndex + 1);

        return new String[]{metricGroup, remaining};
    }

    /**
     * Baut den Label-Suffix für den Metriknamen.
     * 
     * @return Array mit [suffix, quantileSuffix]
     */
    private String[] buildLabelSuffix(Map<String, String> labels) {
        Map<String, String> filteredLabels = new TreeMap<>(); // alphabetisch sortiert
        String quantileSuffix = null;

        for (Map.Entry<String, String> entry : labels.entrySet()) {
            String label = entry.getKey();
            String value = entry.getValue();

            // Standard und ignorierte Labels überspringen
            if (STANDARD_LABELS.contains(label) || IGNORE_LABELS.contains(label)) {
                continue;
            }

            // Histogram-Spezialbehandlung
            if ("quantile".equals(label)) {
                quantileSuffix = QUANTILE_SUFFIX.getOrDefault(value, 
                    "_p" + value.replace(".", ""));
                continue;
            }
            if ("le".equals(label)) {
                // le-Labels werden ignoriert (nur _sum/_count verwenden)
                continue;
            }

            filteredLabels.put(label, value);
        }

        // Suffix bauen: alphabetisch sortiert
        StringBuilder suffix = new StringBuilder();
        for (Map.Entry<String, String> entry : filteredLabels.entrySet()) {
            if (suffix.length() > 0) {
                suffix.append("_");
            }
            // Sonderzeichen im Value behandeln
            String safeValue = entry.getValue()
                .replace("/", "_")
                .replace(" ", "_");
            suffix.append(entry.getKey()).append("_").append(safeValue);
        }

        return new String[]{suffix.toString(), quantileSuffix};
    }

    // ========================================================================
    // Statistik
    // ========================================================================

    /**
     * Gibt die Anzahl der analysierten Metriken zurück.
     */
    public int getAnalyzedCount() {
        return analyzedCount;
    }

    /**
     * Prüft ob die Analysephase bereits durchgeführt wurde.
     */
    public boolean isAnalysisComplete() {
        return analyzedCount > 0;
    }
}
