package io.softwareecg.awb.evidence.export;

import io.softwareecg.awb.evidence.api.ChartExport;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Chooses the file of an Excel export in a running workbench, instead of the dialog of the
 * operating system.
 */
public final class ExcelFileOfTheTest implements AutoCloseable {

    private final ChartExportImpl export = (ChartExportImpl) Lookup.lookup(ChartExport.class);

    private final Function<Window, File> dialog = export.excelFile;

    public ExcelFileOfTheTest(Path file) {
        export.excelFile = owner -> file.toFile();
    }

    @Override
    public void close() {
        export.excelFile = dialog;
    }
}
