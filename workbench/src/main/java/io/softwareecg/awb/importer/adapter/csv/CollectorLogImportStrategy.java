package io.softwareecg.awb.importer.adapter.csv;

import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.importer.model.RawSeriesData;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * An import strategy deriving from LocalProjectImportStrategy that parses the data of collector log csv's to
 * RawSeriesData and provides all further steps instances to proceed this series.
 *
 * @since 14.07.2018
 */
public class CollectorLogImportStrategy extends LocalProjectImportStrategy {

    private static final Logger LOGGER = EkgLogger.get();

    private final List<String> paths;

    /**
     * Constructs a new instance of CollectorLogImportStrategy that will import
     * all Collector logs found at the specified paths.
     *
     * @param paths the paths where the files to import are
     */
    public CollectorLogImportStrategy(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public Stream<RawSeriesData> readRawSeriesData() {

        // notify other components that the import process begins
        String startMessage = "Importing collector logs from '" + paths + "'";
        notifyImportStarted(startMessage);
        LOGGER.info(startMessage);

        // collect all csv files in the source directory and it's sub directories (recursive search)
        List<File> csvFiles = new ArrayList<>();
        paths.forEach(path -> {
            File dir = new File(path);

            if (dir.isDirectory()) {
                csvFiles.addAll(FileUtils.listFiles(dir, new String[]{"csv", "CSV", "csv.gz", "CSV.GZ"}, true));

            } else if (dir.isFile() && (path.toLowerCase().endsWith("csv") || path.toLowerCase().endsWith("csv.gz"))) {
                csvFiles.add(dir);
            }
        });

        int numberOfFiles = csvFiles.size();
        AtomicInteger importedFiles = new AtomicInteger(0);
        CsvMetricsParserNG parser = new CsvMetricsParserNG();

        return csvFiles
                // the csv files can easily imported parallel
                .parallelStream()
                // read in collector log and extract the data to a stream of RawSeriesData
                .flatMap(uri -> {

                    // notify the progress
                    int fileNumber = importedFiles.incrementAndGet();
                    double progress = fileNumber / (double) numberOfFiles;
                    publish(new ProgressEvent("Importing file " + fileNumber + " of "
                            + numberOfFiles + "...", progress, this));

                    // parse the collector logs and create multiple series per csv
                    return parser.parseFile(uri);
                });
    }
}
