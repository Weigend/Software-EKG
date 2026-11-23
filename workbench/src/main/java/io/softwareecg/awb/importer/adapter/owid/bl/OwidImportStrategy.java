package io.softwareecg.awb.importer.adapter.owid.bl;

import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.timeseries.RawSeriesData;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * An import strategy deriving from LocalProjectImportStrategy that parses the data of OWID Covid data to
 * RawSeriesData and provides all further steps instances to proceed this series.
 */
public class OwidImportStrategy extends LocalProjectImportStrategy {

    private static final Logger LOGGER = EkgLogger.get();

    private final List<String> paths;

    /**
     * Constructs a new instance of the ImportStrategy
     *
     * @param paths a list of paths to the import file locations
     */
    public OwidImportStrategy(List<String> paths) {
        this.paths = Collections.unmodifiableList(paths);
    }

    @Override
    public Stream<RawSeriesData> readRawSeriesData() {

        // notify other components that the import process begins
        String startMessage = "Importing COVID-19 CSV from '" + paths + "'";
        notifyImportStarted(startMessage);
        LOGGER.info(startMessage);

        // collect all csv files in the source directory and its sub directories (recursive search)
        List<File> inputFiles = new ArrayList<>();
        paths.forEach(path -> collectCsvFiles(new File(path), inputFiles));

        int numberOfFiles = inputFiles.size();
        AtomicInteger importedFiles = new AtomicInteger(0);

        return inputFiles
                .stream()
                .flatMap(file -> {
                    // notify the progress
                    int fileNumber = importedFiles.incrementAndGet();
                    double progress = fileNumber / (double) numberOfFiles;
                    publish(new ProgressEvent("Importing file " + fileNumber + " of "
                            + numberOfFiles + ": " + file.getName(), progress, this));

                    return OwidMetricParser.parseToRawSeriesStream(file);
                });
    }

    private void collectCsvFiles(File fileOrDir, List<File> result) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    collectCsvFiles(child, result);
                }
            }
        } else if (fileOrDir.isFile()) {
            String name = fileOrDir.getName().toLowerCase(Locale.getDefault());
            if (name.endsWith(".csv") || name.endsWith(".csv.gz")) {
                result.add(fileOrDir);
            }
        }
    }
}
