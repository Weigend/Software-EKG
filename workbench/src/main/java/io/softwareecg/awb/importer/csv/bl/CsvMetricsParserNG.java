package io.softwareecg.awb.importer.csv.bl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import io.softwareecg.awb.importer.csv.et.Collector61Metadata;
import io.softwareecg.awb.importer.csv.fileutil.CsvFormat;
import io.softwareecg.awb.importer.csv.fileutil.FileUtil;
import io.softwareecg.awb.sdk.awbapi.repository.FilterDimension;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import io.softwareecg.awb.sdk.datamodel.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Stream;

/**
 * CSV parser for EKG collector logs in all known version (back to the first version) including
 * the v6.1 NextGeneration format with Unix timestamp as 'Date'.
 * The parser is aware for different kinds of number formats at the counters caused by the various sources and
 * handles various kinds of date formats.
 *
 * It can used as Stream source or in traditional way with a RawSeriesData array as return value.
 *
 * @since 14.07.2018
 */
public class CsvMetricsParserNG {

    private static final String FIRST_COLUMN_DATE_FIELDNAME = "Date";

    private static final String ACCEPTED_COMPRESS_FILE_EXTENSION = ".csv.gz";

    private static final Logger LOGGER = EkgLogger.get();

    //------------------------------------------------------------------------------------------------------------------
    // the CsvMetricsParserNG parser API
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Parse the csv file that is specified by the File pointer
     * to a stream of RawSeriesData entries.
     *
     * @param csvFile a file reference to a file in EKG-Collector compatible csv format
     * @return the content of the csv file as stream of RawSeriesData entities
     */
    public Stream<RawSeriesData> parseFile(File csvFile) {
        try {
            return Arrays.stream(parse(csvFile));

        } catch (IOException e) {
            LOGGER.error("IOException during reading the file", e);
        } catch (ParseException | IllegalArgumentException e) {
            LOGGER.error("Invalid CSV file", e);
        }

        return Stream.empty();
    }

    /**
     * Parses the EKG Collector csv file to an array of RawSeriesData
     *
     * The implementation will support different version of EKG Collector logs back to the first version
     * and is also aware for different kinds of number formats caused by the various sources.
     *
     * @param csvFile a file pointer to the EKG Collector csv file the parser should analyse
     * @return the metric data of the csv log as array of RawSeriesData instance (on for each metric in the csv)
     *
     * @throws IOException if the file couldn't read correctly for IO reasons
     * @throws ParseException if there are any validation errors at parsing the datetime or value numbers
     */
    public RawSeriesData[] parse(File csvFile) throws IOException, ParseException {

        StopWatch watch = StopWatch.createStarted();

        File fileToRead = uncompressIfRequired(csvFile);

        CsvFormat csvFormat = CsvFormat.autodetect(fileToRead);

        if (csvFormat == null) {
            // Log as warning instead of throwing exception - allows other files to be imported
            LOGGER.warn("Unable to detect the CSV format for file: {}. Skipping this file.", csvFile.getName());
            return new RawSeriesData[0];
        }

        CsvReader csvReader = new CsvReader();
        csvReader.setFieldSeparator(';');

        try (CsvParser csvParser = csvReader.parse(fileToRead, StandardCharsets.UTF_8)) {

            String[] metricNames = parseHeader(csvParser);

            RawSeriesData[] seriesData = initializeSeriesData(metricNames);

            int parsedCounterValues = readValuesToSeries(csvParser, seriesData, csvFormat);
            if (parsedCounterValues == 0) {
                LOGGER.warn("Don't read any counter values from '" + csvFile + "'. It seems to be empty.");
            } else {
                LOGGER.info("Read " + parsedCounterValues + " counter values for " + metricNames.length
                        + "metrics from file '" + csvFile + "'in " + watch);
            }

            if (csvFormat.useJsonMetaData()) {
                addFilterDimensionsFromJsonStructure(seriesData, metricNames[0]);
            } else {
                addFilterDimensionsFromFilename(seriesData, csvFile);
            }

            return seriesData;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // internal methods to parse the payload of the csv file
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Add all filter dimension values to each entity of the RawSeriesData that are
     * available (not blank) in the given serialized JSON data.
     *
     * @param seriesData the series data entities with metric labels
     * @param jsonStructure a JSON data structure in EKG compatible format that provides the metadata of the metrics
     * @throws IOException thrown than the deserialization of the JSON structure fails
     */
    private void addFilterDimensionsFromJsonStructure(RawSeriesData[] seriesData, String jsonStructure) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Collector61Metadata metadata = mapper.readValue(jsonStructure, Collector61Metadata.class);

        Map<String, String> seriesLabels = new HashMap<>();

        addLabelIfNotEmpty(seriesLabels, FilterDimension.HOST_GROUP, metadata.getHostGroupName());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.HOST, metadata.getHostName());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.NAMESPACE, metadata.getNamespace());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.SERVICE, metadata.getService());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.POD, metadata.getPod());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.CONTAINER, metadata.getContainer());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.MEASUREMENT, metadata.getMeasurement());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.PROCESS, metadata.getProcess());
        addLabelIfNotEmpty(seriesLabels, FilterDimension.METRIC_GROUP, metadata.getMetricGroup());

        for (RawSeriesData rawSeriesEntity : seriesData) {
            String metric = rawSeriesEntity.getMetricName();
            rawSeriesEntity.setSeriesLabels(new HashMap<>(seriesLabels));
            rawSeriesEntity.addLabel(FilterDimension.METRIC_NAME, metric);
        }
    }

    private void addLabelIfNotEmpty(Map<String, String> seriesLabels, FilterDimension dimension, String value) {
        if (StringUtils.isNotBlank(value)) {
            seriesLabels.put(dimension.name(), value);
        }
    }

    /**
     * Creates and initialize an array of RawSeriesData.
     * For each metric name a RawSeriesData instance will created. The order of the metric names
     * will respect and all RawSeriesData (initialized with metric name) will have the same order.
     *
     * @param metricNames an array of metric names the RawSeriesData array should created for
     * @return and initialized array of RawSeriesData the the same metric names in the same order
     */
    private RawSeriesData[] initializeSeriesData(String[] metricNames) {

        // minus 1 because the metric names contains the first csv header field that isn't a metric
        RawSeriesData[] seriesData = new RawSeriesData[metricNames.length - 1];

        for (int i = 1; i < metricNames.length; i++) {
            seriesData[i - 1] = new RawSeriesData(metricNames[i]);
        }

        return seriesData;
    }

    /**
     * Read the time-series values line by line and addAndSum it as {@link Value} instance together with timestamp to
     * the RawSeriesData instance that belongs to the column (identified by column index).
     *
     * @param csvParser a FastCSV parser instance that will used to parse the lines
     * @param seriesData an pre-initialized RawSeriesData array that has an RawSeriesData instance on each index
     * @param csvFormat a CsvFormat instance that matches to the datetime and number-format used in the csv file
     * @return the amount of counters lines read in CSV file
     *
     * @throws IOException if the file couldn't read correctly for IO reasons
     * @throws ParseException if there are any validation errors at parsing the datetime or value numbers
     */
    private static int readValuesToSeries(CsvParser csvParser, RawSeriesData[] seriesData,
                                          CsvFormat csvFormat) throws IOException, ParseException {

        int lineCounter = 0;
        int expectedColumnCount = seriesData.length + 1;

        CsvRow row;
        while ((row = csvParser.nextRow()) != null) {

             long timestamp = csvFormat.parseDateToTimestamp(row.getField(0));

             // Use the minimum of expected columns and actual row field count to avoid ArrayIndexOutOfBoundsException
             int actualColumnCount = Math.min(expectedColumnCount, row.getFieldCount());

             for (int columnIdx = 1; columnIdx < actualColumnCount; columnIdx++) {
                 Value value = parseValue(row.getField(columnIdx), csvFormat, timestamp);
                 // Only add non-null values (skip empty/blank fields)
                 if (value != null) {
                     seriesData[columnIdx -1].addValue(value);
                 }
             }

            lineCounter++;
        }

        return lineCounter;
    }

    /**
     * Parses the counter value from the given character sequence using a NumberFormat parser
     * provided by the {@link CsvFormat} instance. The parsed value will returned together with the timestamp as
     * {@link Value} object that represents a single value in the time series.
     *
     * @param fieldString the counter value as String parsed before
     * @param csvFormat a CsvFormat instance that provides correct NumberFormat parsers for the current csv file
     * @param datetime the Unix timestamp the counter values was measured
     * @return the combination of parsed counter value and according timestamp as {@link Value} object,
     *         or null if the field is empty, blank, or a placeholder like "-"
     * @throws ParseException if there are any validation errors at parsing the value numbers
     */
    private static Value parseValue(String fieldString, CsvFormat csvFormat, long datetime) throws ParseException {
        // Handle empty, blank fields or placeholder values - return null to indicate missing value
        if (fieldString == null) {
            return null;
        }
        String trimmed = fieldString.trim();
        if (trimmed.isEmpty() || "-".equals(trimmed) || "N/A".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return new Value(datetime, csvFormat.getNumberFormat().parse(trimmed).doubleValue());
    }


    /**
     * Parse the header fields in the first line of the CSV file
     * To be a valid collector csv the first field have to be 'Date' and all other must not be blank.
     *
     * @param csvParser the parse which provides the first line of the CSV file
     * @return an String array with metric name which doesn't include 'Date' because this isn't a metric name
     *
     * @throws IOException if the file couldn't read correctly for IO reasons
     * @throws ParseException if there are any validation errors inside the header line
     */
    private static String[] parseHeader(CsvParser csvParser) throws IOException, ParseException {
        CsvRow csvRow = csvParser.nextRow();

        if (csvRow == null) {
            return new String[0];
        }

        if (!csvRow.getField(0).startsWith("{") && !FIRST_COLUMN_DATE_FIELDNAME.equals(csvRow.getField(0))) {
            throw new IllegalArgumentException("find unexpected field name '" + csvRow.getField(0) + "' in the " +
                    "first column. The parsed csv isn't a valid collector log.");
        }

        int count = csvRow.getFieldCount();
        Set<String> headerSet = new HashSet<>(count, 1f);
        String[] headers = new String[count - 1];

        for (int i = 0; i < count; i++) {

            String headerField = csvRow.getField(i);

            if (StringUtils.isBlank(headerField)) {

                // we know that older collector logs have an tailing colon at the header line
                // this is not correct but have to be handled here (skip the field)
                if (i == count - 1) {
                    headers = Arrays.copyOf(headers, headers.length - 1);
                    break;
                }

                throw new ParseException("Find blank headers fields in the first line!", 0);
            }

            headers[i] = headerField;

            if (headerSet.contains(headerField)) {
                throw new ParseException("Find duplicated headers ('" + headerField + "') in the first line!", 0);
            }

            headerSet.add(headerField);
        }


        return headers;
    }

    /**
     * Uncompress the File if necessary.
     *
     * @param file the csv file that is eventually compressed
     * @return File the real file to use.
     * @throws IOException In case of the file can not be uncompressed.
     */
    private static File uncompressIfRequired(File file) throws IOException {
        if (file.getName().endsWith(ACCEPTED_COMPRESS_FILE_EXTENSION)) {
            return FileUtil.unCompressFile(file);
        }
        return file;
    }

    //------------------------------------------------------------------------------------------------------------------
    // internal methods to process file name to extract filter dimensions and addAndSum this data to the series data
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Extract the EKG classic filter dimensions from the file-name (and the one of the parent folder)
     * and set it as {@link FilterDimension} names to the RawSeriesData given as first parameter.
     *
     * @param seriesData an array of series data to fulfill with the filter dimensions the metric belongs to
     * @param csvFile the csv file that will proceed by this parser
     */
    private void addFilterDimensionsFromFilename(RawSeriesData[] seriesData, File csvFile) {

        String[] classicDimensions = extractHostProcessGroup(csvFile.getName());

        for (RawSeriesData rawSeriesEntity : seriesData) {
            rawSeriesEntity.addLabel(FilterDimension.HOST, classicDimensions[0]);
            rawSeriesEntity.addLabel(FilterDimension.PROCESS, classicDimensions[1]);
            rawSeriesEntity.addLabel(FilterDimension.METRIC_GROUP, classicDimensions[2]);
            rawSeriesEntity.addLabel(FilterDimension.MEASUREMENT, csvFile.getParentFile().getName());
        }
    }

    /**
     * Helper to split filename in
     * - host (the physical node the metric come from)
     * - process (the metrics belongs to)
     * - metricGroup (a group the defines the set of metrics the belongs together)
     *
     * @param fileName the file name
     * @return a array with [0] == host [1] == process and [2] == group
     */
    private static String[] extractHostProcessGroup(String fileName) {
        String name = fileName.substring(0, fileName.lastIndexOf('.')); // strip extension
        StringTokenizer tok = new StringTokenizer(name, "_");
        String[] result = {"Unknown", "Unknown", "Unknown"};
        int i = 0;
        while (tok.hasMoreElements() && i < 3) {
            result[i++] = tok.nextToken();
        }
        return result;
    }
}
