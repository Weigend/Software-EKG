package io.softwareecg.awb.evidence.export;

import io.softwareecg.awb.evidence.api.ExportedSeries;
import io.softwareecg.awb.evidence.api.ChartExport;
import jakarta.inject.Singleton;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.function.Function;

/**
 * Exports as a PNG picture and as an Excel sheet.
 */
@Singleton
public class ChartExportImpl implements ChartExport {

    /**
     * Asks for the file of the sheet; {@code null} if there is to be none. A seam for tests, which
     * cannot operate the dialog of the operating system.
     */
    Function<Window, File> excelFile = ChartExportImpl::askForExcelFile;

    @Override
    public void exportAsImage(Window owner, Region chart, double axisWidth) {
        ScreenshotExportDialog dialog = new ScreenshotExportDialog(chart, axisWidth);
        dialog.initOwner(owner);
        dialog.showAndWait();
    }

    /** The user interface does not respond while the sheet is written. */
    @Override
    public void exportAsExcel(Window owner, List<ExportedSeries> series) {
        File chosenFile = excelFile.apply(owner);
        if (chosenFile != null) {
            new ExcelExporter(chosenFile).createExcelSheetFromSeriesData(series);
        }
    }

    private static File askForExcelFile(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the file for the Excel export");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel files", "*.xlsx"),
                new FileChooser.ExtensionFilter("All files", "*")
        );
        return chooser.showSaveDialog(owner);
    }
}
