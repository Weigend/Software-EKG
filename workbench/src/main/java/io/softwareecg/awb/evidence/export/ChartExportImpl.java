package io.softwareecg.awb.evidence.export;

import io.softwareecg.awb.evidence.api.ChartExport;
import io.softwareecg.awb.uitoolkit.chart.ColoredChart;
import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;
import jakarta.inject.Singleton;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

/**
 * Exports as a PNG picture and as an Excel sheet.
 */
@Singleton
public class ChartExportImpl implements ChartExport {

    @Override
    public void exportAsImage(Window owner, ZoomableStackedChart chart) {
        ScreenshotExportDialog dialog = new ScreenshotExportDialog(chart);
        dialog.initOwner(owner);
        dialog.showAndWait();
    }

    /** The user interface does not respond while the sheet is written. */
    @Override
    public void exportAsExcel(Window owner, ZoomableStackedChart chart) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the file for the Excel export");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel files", "*.xlsx"),
                new FileChooser.ExtensionFilter("All files", "*")
        );

        File chosenFile = chooser.showSaveDialog(owner);
        if (chosenFile != null) {
            new ExcelExporter(chosenFile).createExcelSheetFromSeriesData(seriesOf(chart));
        }
    }

    /** What the chart in front shows, then what those behind it show. */
    static List<ExcelExportSeriesData> seriesOf(ZoomableStackedChart chart) {
        return Stream.concat(Stream.of(chart.getBaseChart()), chart.getBackgroundCharts().stream())
                .map(ColoredChart::getAllSeries)
                .flatMap(List::stream)
                .map(series -> new ExcelExportSeriesData(series.getName(), series.getData()))
                .toList();
    }
}
