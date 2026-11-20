package io.softwareecg.awb.importer.yfinance.bl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import io.softwareecg.awb.sdk.awbapi.repository.FilterDimension;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import io.softwareecg.awb.sdk.datamodel.Value;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class YFinanceMarketDataParser {

    private static final Logger LOGGER = EkgLogger.get();

    private static final DateTimeFormatter OFFSET_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX", Locale.US);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DATASET_HISTORY = "History";
    private static final String DATASET_INTRADAY = "Intraday";
    private static final String DATASET_CORPORATE_ACTIONS = "Corporate Actions";

    private final YFinanceImportOptions options;

    public YFinanceMarketDataParser(YFinanceImportOptions options) {
        this.options = options;
    }

    public Stream<RawSeriesData> parseSymbolDirectory(File symbolDir) {
        try {
            SymbolMetadata metadata = loadMetadata(symbolDir);
            List<RawSeriesData> result = new ArrayList<>();

            File[] csvFiles = symbolDir.listFiles(file -> file.isFile() && file.getName().endsWith(".csv"));
            if (csvFiles == null) {
                return Stream.empty();
            }

            for (File csvFile : csvFiles) {
                result.addAll(parseCsvFile(symbolDir.getName(), metadata, csvFile));
            }

            LOGGER.info("Parsed Yahoo Finance symbol directory {} into {} time series",
                    symbolDir.getName(), result.size());
            return result.stream();
        } catch (Exception e) {
            LOGGER.error("Failed to parse Yahoo Finance symbol directory: {}", symbolDir.getAbsolutePath(), e);
            return Stream.empty();
        }
    }

    private List<RawSeriesData> parseCsvFile(String symbol, SymbolMetadata metadata, File csvFile) throws IOException {
        String fileName = csvFile.getName();

        if (fileName.startsWith("intraday_") && !options.importIntraday()) {
            return List.of();
        }

        if (fileName.startsWith("history_") && !options.importPriceHistory()) {
            return List.of();
        }

        if (isCorporateActionFile(fileName) && !options.importCorporateActions()) {
            return List.of();
        }

        CsvReader csvReader = new CsvReader();
        try (CsvParser csvParser = csvReader.parse(csvFile, StandardCharsets.UTF_8)) {
            CsvRow headerRow = csvParser.nextRow();
            if (headerRow == null) {
                return List.of();
            }

            String[] headers = headerRow.getFields().toArray(new String[0]);
            Map<String, RawSeriesData> seriesByMetric = createSeriesMap(symbol, metadata, fileName);

            CsvRow row;
            while ((row = csvParser.nextRow()) != null) {
                appendRow(headers, row, seriesByMetric);
            }

            return seriesByMetric.values().stream()
                    .filter(series -> !series.getValues().isEmpty())
                    .toList();
        }
    }

    private Map<String, RawSeriesData> createSeriesMap(String symbol, SymbolMetadata metadata, String fileName) {
        Map<String, RawSeriesData> result = new LinkedHashMap<>();

        for (MetricDefinition metric : metricsForFile(fileName)) {
            RawSeriesData series = new RawSeriesData(metric.metricName());
            enrichDimensions(series, symbol, metadata, fileName, metric);
            result.put(metric.columnName(), series);
        }

        return result;
    }

    private void enrichDimensions(RawSeriesData series,
                                  String symbol,
                                  SymbolMetadata metadata,
                                  String fileName,
                                  MetricDefinition metric) {
        series.addLabel(FilterDimension.HOST_GROUP, metadata.assetClass());
        series.addLabel(FilterDimension.HOST, symbol);
        series.addLabel(FilterDimension.MEASUREMENT, formatPeriodLabel(fileName));
        series.addLabel(FilterDimension.PROCESS_GROUP, metadata.country());
        series.addLabel(FilterDimension.PROCESS, metadata.exchange());
        series.addLabel(FilterDimension.METRIC_GROUP, metric.metricGroup());
    }

    private void appendRow(String[] headers, CsvRow row, Map<String, RawSeriesData> seriesByMetric) {
        String timestampValue = row.getField(0);
        if (timestampValue == null) {
            return;
        }

        long timestamp = parseTimestamp(timestampValue);

        for (int index = 1; index < headers.length; index++) {
            String header = headers[index];
            RawSeriesData series = seriesByMetric.get(header);
            if (series == null) {
                continue;
            }

            String rawValue = row.getField(index);
            Double numericValue = parseNumericValue(rawValue);
            if (numericValue == null) {
                continue;
            }

            series.addValue(new Value(timestamp, numericValue));
        }
    }

    private List<MetricDefinition> metricsForFile(String fileName) {
        if (fileName.startsWith("history_") || fileName.startsWith("intraday_")) {
            List<MetricDefinition> metrics = new ArrayList<>();
            metrics.add(new MetricDefinition("Open", "Open", "Price"));
            metrics.add(new MetricDefinition("High", "High", "Price"));
            metrics.add(new MetricDefinition("Low", "Low", "Price"));
            metrics.add(new MetricDefinition("Close", "Close", "Price"));
            if (options.importAdjustedClose()) {
                metrics.add(new MetricDefinition("Adj Close", "Adjusted Close", "Price"));
            }
            metrics.add(new MetricDefinition("Volume", "Volume", "Trading"));
            if (options.importCorporateActions()) {
                metrics.add(new MetricDefinition("Dividends", "Dividends", DATASET_CORPORATE_ACTIONS));
                metrics.add(new MetricDefinition("Stock Splits", "Stock Splits", DATASET_CORPORATE_ACTIONS));
            }
            if (options.importRepairedFlag()) {
                metrics.add(new MetricDefinition("Repaired?", "Repaired", "Data Quality"));
            }
            return metrics;
        }

        if ("actions.csv".equals(fileName)) {
            return List.of(
                    new MetricDefinition("Dividends", "Dividends", DATASET_CORPORATE_ACTIONS),
                    new MetricDefinition("Stock Splits", "Stock Splits", DATASET_CORPORATE_ACTIONS)
            );
        }

        if ("dividends.csv".equals(fileName)) {
            return List.of(new MetricDefinition("dividends", "Dividends", DATASET_CORPORATE_ACTIONS));
        }

        if ("splits.csv".equals(fileName)) {
            return List.of(new MetricDefinition("splits", "Stock Splits", DATASET_CORPORATE_ACTIONS));
        }

        if ("capital_gains.csv".equals(fileName)) {
            return List.of(new MetricDefinition("capital_gains", "Capital Gains", DATASET_CORPORATE_ACTIONS));
        }

        return List.of();
    }

    private SymbolMetadata loadMetadata(File symbolDir) throws IOException {
        if (!options.useMetadataDimensions()) {
            return SymbolMetadata.fallback(symbolDir.getName());
        }

        File metadataFile = new File(symbolDir, "metadata.json");
        if (!metadataFile.isFile()) {
            return SymbolMetadata.fallback(symbolDir.getName());
        }

        JsonNode rootNode = OBJECT_MAPPER.readTree(metadataFile);
        JsonNode infoNode = rootNode.path("info");

        String quoteType = asText(infoNode, "quoteType", "UNKNOWN");
        String exchange = normalizeExchange(
                asText(infoNode, "fullExchangeName", asText(infoNode, "exchange", "Unknown Exchange"))
        );
        String country = normalizeCountry(asText(infoNode, "country", inferCountry(symbolDir.getName())));

        return new SymbolMetadata(
                normalizeLabel(exchange, "Unknown Exchange"),
                normalizeAssetClass(quoteType),
                normalizeLabel(country, "Unknown Country")
        );
    }

    private String inferCountry(String symbol) {
        if (symbol.endsWith(".DE")) {
            return "Germany";
        }
        if (symbol.endsWith(".SW")) {
            return "Switzerland";
        }
        if (symbol.endsWith(".T")) {
            return "Japan";
        }
        if (symbol.endsWith(".L") || symbol.endsWith(".IL")) {
            return "United Kingdom";
        }
        return "United States";
    }

    private String normalizeCountry(String country) {
        return switch (country) {
            case "United States" -> "USA";
            case "United Kingdom" -> "UK";
            default -> normalizeLabel(country, "Unknown Country");
        };
    }

    private String normalizeExchange(String exchange) {
        return switch (exchange) {
            case "NasdaqGS", "NMS" -> "Nasdaq";
            case "NYSE" -> "NYSE";
            case "XETRA", "GER" -> "XETRA";
            case "Swiss", "SIX" -> "SIX";
            case "Tokyo", "TSE" -> "TSE";
            case "LSE", "London" -> "LSE";
            case "IOB" -> "IOB";
            default -> normalizeLabel(exchange, "Unknown Exchange");
        };
    }

    private String normalizeAssetClass(String quoteType) {
        if (quoteType == null || quoteType.isBlank()) {
            return "Unknown";
        }

        return switch (quoteType.toUpperCase(Locale.ROOT)) {
            case "EQUITY" -> "Equity";
            case "ETF" -> "ETF";
            case "MUTUALFUND" -> "Mutual Fund";
            case "INDEX" -> "Index";
            case "CRYPTOCURRENCY" -> "Crypto";
            case "CURRENCY" -> "FX";
            default -> normalizeLabel(quoteType, "Unknown");
        };
    }

    private String datasetForFile(String fileName) {
        if (fileName.startsWith("intraday_")) {
            return DATASET_INTRADAY;
        }
        if (fileName.startsWith("history_")) {
            return DATASET_HISTORY;
        }
        return DATASET_CORPORATE_ACTIONS;
    }

    private String extractInterval(String fileName) {
        if (fileName.startsWith("history_")) {
            return fileName.substring("history_".length(), fileName.length() - ".csv".length());
        }
        if (fileName.startsWith("intraday_")) {
            return fileName.substring("intraday_".length(), fileName.length() - ".csv".length());
        }
        return "events";
    }

    private String formatPeriodLabel(String fileName) {
        return datasetForFile(fileName) + "/" + extractInterval(fileName);
    }

    private boolean isCorporateActionFile(String fileName) {
        return Objects.equals(fileName, "actions.csv")
                || Objects.equals(fileName, "dividends.csv")
                || Objects.equals(fileName, "splits.csv")
                || Objects.equals(fileName, "capital_gains.csv");
    }

    private long parseTimestamp(String rawValue) {
        try {
            return Instant.parse(rawValue.replace(" ", "T")).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // fallback below
        }

        try {
            return OffsetDateTime.parse(rawValue, OFFSET_DATE_TIME_FORMAT).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // fallback below
        }

        return LocalDate.parse(rawValue).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private Double parseNumericValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        if ("True".equalsIgnoreCase(rawValue)) {
            return 1.0d;
        }
        if ("False".equalsIgnoreCase(rawValue)) {
            return 0.0d;
        }

        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asText(JsonNode node, String fieldName, String fallback) {
        JsonNode child = node.path(fieldName);
        return child.isMissingNode() || child.isNull() || child.asText().isBlank()
                ? fallback
                : child.asText();
    }

    private String normalizeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record MetricDefinition(String columnName, String metricName, String metricGroup) {
    }

    private record SymbolMetadata(
            String exchange,
            String assetClass,
            String country
    ) {
        private static SymbolMetadata fallback(String symbol) {
            return new SymbolMetadata(
                    "Unknown Exchange",
                    "Unknown",
                    normalizeFallbackCountry(symbol)
            );
        }

        private static String normalizeFallbackCountry(String symbol) {
            if (symbol.endsWith(".DE")) {
                return "Germany";
            }
            if (symbol.endsWith(".SW")) {
                return "Switzerland";
            }
            if (symbol.endsWith(".T")) {
                return "Japan";
            }
            if (symbol.endsWith(".L") || symbol.endsWith(".IL")) {
                return "UK";
            }
            return "USA";
        }
    }
}
