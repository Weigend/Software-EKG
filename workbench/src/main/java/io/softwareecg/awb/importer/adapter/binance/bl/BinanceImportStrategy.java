package io.softwareecg.awb.importer.adapter.binance.bl;

import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.timeseries.RawSeriesData;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Import strategy for Binance market data files (ZIP archives with aggTrades CSV).
 * Recursively scans selected paths for ZIP files and parses each one.
 *
 */
public class BinanceImportStrategy extends LocalProjectImportStrategy {

    private static final Logger LOGGER = EkgLogger.get();

    private final List<String> paths;

    public BinanceImportStrategy(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public Stream<RawSeriesData> readRawSeriesData() {

        String startMessage = "Importing Binance market data from '" + paths + "'";
        notifyImportStarted(startMessage);
        LOGGER.info(startMessage);

        // Collect all ZIP files from selected paths
        List<File> zipFiles = new ArrayList<>();
        paths.forEach(path -> collectZipFiles(new File(path), zipFiles));

        int numberOfFiles = zipFiles.size();
        AtomicInteger importedFiles = new AtomicInteger(0);
        BinanceAggTradesParser parser = new BinanceAggTradesParser();

        return zipFiles.stream()
                .flatMap(zipFile -> {
                    int fileNumber = importedFiles.incrementAndGet();
                    double progress = fileNumber / (double) numberOfFiles;
                    publish(new ProgressEvent("Importing file " + fileNumber + " of "
                            + numberOfFiles + ": " + zipFile.getName(), progress, this));

                    return parser.parseZipFile(zipFile);
                });
    }

    private void collectZipFiles(File fileOrDir, List<File> result) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    collectZipFiles(child, result);
                }
            }
        } else if (fileOrDir.isFile() && fileOrDir.getName().toLowerCase().endsWith(".zip")) {
            result.add(fileOrDir);
        }
    }
}
