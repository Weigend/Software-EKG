package io.softwareecg.awb.importer.adapter.yfinance;

import io.softwareecg.awb.projectcontext.api.TargetProjectConfig;
import io.softwareecg.awb.repository.api.TimeSeriesWriteService;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.importer.model.RawSeriesData;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class YFinanceImportStrategy extends LocalProjectImportStrategy {

    private static final Logger LOGGER = EkgLogger.get();

    private final List<String> paths;
    private final Supplier<YFinanceImportOptions> options;

    /**
     * @param paths   the files and directories chosen in the dialog
     * @param options what to import, asked for when an import begins
     */
    public YFinanceImportStrategy(List<String> paths, Supplier<YFinanceImportOptions> options) {
        this.paths = paths;
        this.options = options;
    }

    @Override
    public Stream<RawSeriesData> readRawSeriesData(TargetProjectConfig project, TimeSeriesWriteService importService) {
        String startMessage = "Importing Yahoo Finance market data from '" + paths + "'";
        notifyImportStarted(startMessage);
        LOGGER.info(startMessage);

        Set<File> symbolDirs = new LinkedHashSet<>();
        paths.forEach(path -> collectSymbolDirectories(new File(path), symbolDirs));

        List<File> orderedSymbolDirs = new ArrayList<>(symbolDirs);
        int numberOfDirs = orderedSymbolDirs.size();
        AtomicInteger importedDirs = new AtomicInteger(0);
        YFinanceMarketDataParser parser = new YFinanceMarketDataParser(options.get());

        return orderedSymbolDirs.stream()
                .flatMap(symbolDir -> {
                    int dirNumber = importedDirs.incrementAndGet();
                    double progress = numberOfDirs == 0 ? 1.0d : dirNumber / (double) numberOfDirs;
                    publish(new ProgressEvent("Importing symbol " + dirNumber + " of "
                            + numberOfDirs + ": " + symbolDir.getName(), progress, this));
                    return parser.parseSymbolDirectory(symbolDir);
                });
    }

    private void collectSymbolDirectories(File fileOrDir, Set<File> result) {
        if (!fileOrDir.exists()) {
            return;
        }

        if (fileOrDir.isFile()) {
            File parent = fileOrDir.getParentFile();
            if (parent != null && isSymbolDirectory(parent)) {
                result.add(parent);
            }
            return;
        }

        if (isSymbolDirectory(fileOrDir)) {
            result.add(fileOrDir);
            return;
        }

        File[] children = fileOrDir.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            collectSymbolDirectories(child, result);
        }
    }

    private boolean isSymbolDirectory(File dir) {
        return new File(dir, "metadata.json").isFile()
                && (containsHistoryFile(dir) || containsActionFile(dir));
    }

    private boolean containsHistoryFile(File dir) {
        File[] historyFiles = dir.listFiles(file -> file.isFile() && file.getName().startsWith("history_"));
        return historyFiles != null && historyFiles.length > 0;
    }

    private boolean containsActionFile(File dir) {
        return new File(dir, "actions.csv").isFile()
                || new File(dir, "dividends.csv").isFile()
                || new File(dir, "splits.csv").isFile();
    }
}
