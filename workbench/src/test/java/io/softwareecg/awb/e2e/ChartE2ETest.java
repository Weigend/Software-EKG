package io.softwareecg.awb.e2e;

import io.softwareecg.awb.uitoolkit.chart.ChartColorStyle;
import io.softwareecg.awb.uitoolkit.chart.ChartType;
import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;
import io.softwareecg.awb.uitoolkit.chart.axis.DateAxis;
import io.softwareecg.awb.metricview.ChartTimeZone;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import io.softwareecg.awb.evidence.export.ExcelFileOfTheTest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The chart and its legend: what the list of chart types in the legend does to the chart.
 */
class ChartE2ETest {

    private static final Map<ChartType, Class<?>> CHART_OF_TYPE = Map.of(
            ChartType.LINE, LineChart.class,
            ChartType.AREA, AreaChart.class,
            ChartType.SUM, StackedAreaChart.class,
            ChartType.POINT, ScatterChart.class);

    private static Workbench workbench;

    @BeforeAll
    static void startTheWorkbenchWithAChart() throws URISyntaxException {
        workbench = Workbench.start();
        Path logs = Path.of(ChartE2ETest.class.getClassLoader().getResource("e2e/collector").toURI());
        ImportDialog dialog = workbench.openImporter("EKG Collector Logs");
        dialog.projectName("Chart").files(logs).importData(Duration.ofMinutes(2));
        workbench.project("Chart").awaitPoints(330, Duration.ofSeconds(30));
    }

    /** Every test begins with one metric in front and nothing behind it. */
    @BeforeEach
    void showOneMetric() {
        workbench.openChart("Chart", "os", "\\Cpu\\us");
    }

    @AfterEach
    void backToLines() {
        choose(ChartType.LINE);
    }

    /**
     * A chart of another type is a new chart with new axes. Its time axis shows the times as
     * the one before did, in the time zone of the chart: it used to fall back to UTC, and the
     * same values stood an hour or two apart after the type was changed.
     */
    @Test
    void everyChartTypeDrawsItsChartAndKeepsTheTimeZone() {
        ZoneId timeZone = timeZoneOfThePanel();
        assertThat(timeZoneOf(baseChart())).as("time zone of the chart at first").isEqualTo(timeZone);

        for (ChartType type : List.of(ChartType.AREA, ChartType.SUM, ChartType.POINT, ChartType.LINE)) {
            choose(type);

            XYChart<?, ?> chart = baseChart();
            assertThat(chart).as("the chart of the type " + type).isInstanceOf(CHART_OF_TYPE.get(type));
            assertThat(workbench.onFx(() -> chart.getData().size())).as("series of the chart " + type).isEqualTo(1);
            assertThat(timeZoneOf(chart)).as("time zone of the chart " + type).isEqualTo(timeZone);
        }
        workbench.screenshot("chart-types");
    }

    /**
     * The points of a point chart have the colour the legend shows for their series. They
     * used to keep the colours JavaFX gives them.
     */
    @Test
    void thePointsOfAPointChartHaveTheColourOfTheLegend() {
        choose(ChartType.POINT);
        workbench.screenshot("chart-points");

        String colourOfTheLegend = workbench.onFx(() -> ChartColorStyle.toRGBCode(
                legend().lookupAll(".color-picker").stream()
                        .filter(ColorPicker.class::isInstance).map(ColorPicker.class::cast)
                        .findFirst().orElseThrow(() -> new AssertionError("The legend has no colour field"))
                        .getValue()));
        List<String> stylesOfThePoints = Workbench.await("the points of the chart", Duration.ofSeconds(10),
                () -> workbench.onFx(() -> {
                    List<String> styles = baseChart().getData().get(0).getData().stream()
                            .map(point -> point.getNode() == null ? "" : String.valueOf(point.getNode().getStyle()))
                            .toList();
                    return styles.isEmpty() || styles.contains("") ? Optional.<List<String>>empty() : Optional.of(styles);
                }));

        assertThat(stylesOfThePoints).isNotEmpty().allMatch(style -> style.contains(colourOfTheLegend),
                "has the colour " + colourOfTheLegend);
    }

    /**
     * A chart of the background comes back to the front, with its series and their colours,
     * and the header shows its query again. It could only be removed before, so that a query
     * in the background could not be changed any more.
     */
    @Test
    void aChartOfTheBackgroundComesBackToTheFront() {
        Color colourOfTheCpu = workbench.onFx(() -> stackedChart().getBaseChart().getAllColoredSeries().get(0).getColor());

        workbench.openChartInFront("Chart", "os", "\\Load\\one");
        awaitInFront("\\Load\\one");
        assertThat(seriesOfTheBackground()).as("series in the background").singleElement().asString().endsWith("\\Cpu\\us");
        assertThat(metricOfTheHeader()).as("metric of the header").isEqualTo("\\Load\\one");
        // the chart in front does not begin with the colours again that the one behind it has
        assertThat(workbench.onFx(() -> stackedChart().getBaseChart().getSeriesColours()))
                .as("colours in front, with " + colourOfTheCpu + " in the background").doesNotContain(colourOfTheCpu);

        bringTheBackgroundChartToFront();
        awaitInFront("\\Cpu\\us");
        workbench.screenshot("chart-brought-to-front");

        assertThat(seriesOfTheBackground()).as("series in the background").singleElement().asString().endsWith("\\Load\\one");
        assertThat(metricOfTheHeader()).as("metric of the header").isEqualTo("\\Cpu\\us");
        assertThat(workbench.onFx(() -> stackedChart().getBaseChart().getAllColoredSeries().get(0).getColor()))
                .as("colour of the series that is in front again").isEqualTo(colourOfTheCpu);

        // what went to the background in exchange can be brought to the front as well
        bringTheBackgroundChartToFront();
        awaitInFront("\\Load\\one");
        assertThat(seriesOfTheBackground()).as("series in the background").singleElement().asString().endsWith("\\Cpu\\us");
        assertThat(metricOfTheHeader()).as("metric of the header").isEqualTo("\\Load\\one");
    }

    /**
     * The lists "Combine metrics" and "Smoothing" say what the chosen mode does: nobody
     * guesses what "Concatenate" means.
     */
    @Test
    void theListsOfTheHeaderSayWhatTheChosenModeDoes() {
        assertThat(tooltipOf("cbxCombineMode")).isEqualTo(
                "Combine metrics - Concatenate: the parts a series has in several measurements are joined to one line");
        assertThat(tooltipOf("cbxSmoothingType")).isEqualTo("Smoothing - Inactive: the values as they were measured");
    }

    // ---------------------------------------------------------------------------------------------

    private static void bringTheBackgroundChartToFront() {
        workbench.onFx(() -> {
            Button button = legend().lookupAll(".button").stream()
                    .filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(candidate -> "Bring to Front".equals(candidate.getText()))
                    .findFirst().orElseThrow(() -> new AssertionError("The legend has no button 'Bring to Front'"));
            assertThat(button.isDisabled()).as("'Bring to Front' is disabled").isFalse();
            button.fire();
            return null;
        });
    }

    private static void awaitInFront(String metric) {
        Workbench.await("the metric " + metric + " in front and one chart in the background", Duration.ofSeconds(20),
                () -> workbench.onFx(() -> {
                    List<XYChart.Series<Long, Double>> inFront = stackedChart().getBaseChart().getAllSeries();
                    return inFront.size() == 1 && inFront.get(0).getName().endsWith(metric)
                            && stackedChart().getBackgroundCharts().size() == 1 ? Optional.of(metric) : Optional.empty();
                }));
        Workbench.pause(Duration.ofMillis(500));
    }

    private static List<String> seriesOfTheBackground() {
        return workbench.onFx(() -> stackedChart().getBackgroundCharts().stream()
                .flatMap(chart -> chart.getAllSeries().stream()).map(XYChart.Series::getName).toList());
    }

    /** The dialog did not open in 7.4.0: its view and icons were not where its class is. */
    @Test
    void theDialogOfThePngExportOpens() {
        Stage dialog = workbench.openDialog("PNG export", () -> ((Button) shown("btnExportToPNG")).fire());
        workbench.screenshot(dialog, "chart-png-export-dialog");
        assertThat(workbench.onFx(dialog::getTitle)).isEqualTo("Export Chart as Bitmap Image");
        workbench.close(dialog);
    }

    /** The picture is written, in twice the size of the chart, and shows its line. */
    @Test
    void thePictureOfAChartIsSaved() throws IOException {
        BufferedImage image = saveAsPicture("chart.png", null);
        // the label at the end of the time axis sticks out of the chart, the picture has all of it
        int width = (int) (2 * workbench.onFx(() -> stackedChart().getWidth()));
        int height = (int) (2 * workbench.onFx(() -> stackedChart().getHeight()));
        assertThat(image.getWidth()).as("width").isBetween(width - 4, width + 150);
        assertThat(image.getHeight()).as("height").isBetween(height - 4, height + 4);
        Color line = workbench.onFx(() -> stackedChart().getBaseChart().getAllColoredSeries().get(0).getColor());
        assertThat(pixelsLike(image, line)).as("pixels in the colour of the line, " + line).isGreaterThan(100);
    }

    /** A picture of a chosen size has this size, whatever the size of the chart, which keeps its own. */
    @Test
    void aPictureOfAChosenSizeIsSaved() throws IOException {
        double widthOfTheChart = workbench.onFx(() -> stackedChart().getWidth());
        BufferedImage image = saveAsPicture("chart-full-hd.png", "Full-HD");
        assertThat(image.getWidth()).as("width").isBetween(1920 - 4, 1920 + 150);
        assertThat(image.getHeight()).as("height").isBetween(1080 - 4, 1080 + 4);
        Color line = workbench.onFx(() -> stackedChart().getBaseChart().getAllColoredSeries().get(0).getColor());
        assertThat(pixelsLike(image, line)).as("pixels in the colour of the line, " + line).isGreaterThan(100);
        assertThat(workbench.onFx(() -> stackedChart().getWidth())).as("the chart on the screen").isEqualTo(widthOfTheChart);
    }

    /** @param size what the name of the size begins with, {@code null} for the one that is chosen */
    private static BufferedImage saveAsPicture(String name, String size) throws IOException {
        Path file = exportFile(name);
        Stage dialog = workbench.openDialog("PNG export", () -> ((Button) shown("btnExportToPNG")).fire());
        workbench.onFx(() -> {
            if (size != null) {
                chooseSize(Workbench.control(dialog, "cbExportDimension", ComboBox.class), size);
            }
            Workbench.control(dialog, "txtFilePath", TextField.class).setText(file.toString());
            Button save = (Button) ((DialogPane) dialog.getScene().getRoot()).lookupButton(ButtonType.APPLY);
            assertThat(save.isDisabled()).as("'Save' is disabled").isFalse();
            Platform.runLater(save::fire);
            return null;
        });
        workbench.awaitTrue("the dialog to close", Duration.ofSeconds(20), () -> workbench.onFx(() -> !dialog.isShowing()));
        BufferedImage image = ImageIO.read(file.toFile());
        assertThat(image).as("the picture").isNotNull();
        return image;
    }

    private static <T> void chooseSize(ComboBox<T> sizes, String name) {
        sizes.setValue(sizes.getItems().stream()
                .filter(size -> sizes.getConverter().toString(size).startsWith(name)).findFirst().orElseThrow());
    }

    /** The sheet has what the chart shows, with the chart in the background. Since 7.1.1 it was empty. */
    @Test
    void theValuesOfAChartAreSavedAsAnExcelSheet() throws Exception {
        workbench.openChartInFront("Chart", "os", "\\Load\\one");
        awaitInFront("\\Load\\one");
        Path file = exportFile("chart.xlsx");
        try (ExcelFileOfTheTest chosen = new ExcelFileOfTheTest(file)) {
            workbench.onFx(() -> {
                ((Button) shown("btnExportToExcel")).fire();
                return null;
            });
        }

        XYChart.Series<Long, Double> front = workbench.onFx(() -> stackedChart().getBaseChart().getAllSeries().get(0));
        XYChart.Series<Long, Double> behind = workbench.onFx(() -> stackedChart().getBackgroundCharts().get(0).getAllSeries().get(0));
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Date/Time - " + front.getName());
            assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Date/Time - " + behind.getName());
            assertThat(front.getData().size()).as("points of the chart").isGreaterThan(10);
            assertThat(columnOf(sheet, 0)).isEqualTo(front.getData().stream().map(point -> (double) point.getXValue()).toList());
            assertThat(columnOf(sheet, 1)).isEqualTo(front.getData().stream().map(XYChart.Data::getYValue).toList());
            assertThat(columnOf(sheet, 2)).isEqualTo(behind.getData().stream().map(point -> (double) point.getXValue()).toList());
            assertThat(columnOf(sheet, 3)).isEqualTo(behind.getData().stream().map(XYChart.Data::getYValue).toList());
        }
    }

    private static Path exportFile(String name) throws IOException {
        Path file = Workbench.WORK.resolve("export").resolve(name);
        Files.createDirectories(file.getParent());
        Files.deleteIfExists(file);
        return file;
    }

    /** The values of a column below the header; a time in milliseconds. */
    private static List<Double> columnOf(Sheet sheet, int column) {
        List<Double> values = new ArrayList<>();
        for (int row = 1; row <= sheet.getLastRowNum(); row++) {
            Cell cell = sheet.getRow(row).getCell(column);
            if (cell != null) {
                values.add(DateUtil.isCellDateFormatted(cell)
                        ? (double) cell.getDateCellValue().getTime() : cell.getNumericCellValue());
            }
        }
        return values;
    }

    private static long pixelsLike(BufferedImage image, Color colour) {
        long found = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                java.awt.Color pixel = new java.awt.Color(image.getRGB(x, y));
                if (Math.abs(pixel.getRed() - 255 * colour.getRed()) < 12
                        && Math.abs(pixel.getGreen() - 255 * colour.getGreen()) < 12
                        && Math.abs(pixel.getBlue() - 255 * colour.getBlue()) < 12) {
                    found++;
                }
            }
        }
        return found;
    }

    private static String metricOfTheHeader() {
        return workbench.onFx(() -> ((ComboBox<?>) shown("cbxMetric")).getEditor().getText());
    }

    private static String tooltipOf(String id) {
        return workbench.onFx(() -> ((ComboBox<?>) shown(id)).getTooltip().getText());
    }

    private static ZoomableStackedChart stackedChart() {
        return (ZoomableStackedChart) ((BorderPane) shown("chartContainer")).getCenter();
    }

    /** Chooses a type in the list of the chart in front, and waits for the chart of that type. */
    private static void choose(ChartType type) {
        workbench.onFx(() -> {
            typeList().setValue(type);
            return null;
        });
        Workbench.await("the chart of the type " + type, Duration.ofSeconds(20),
                () -> workbench.onFx(() -> CHART_OF_TYPE.get(type).isInstance(baseChart())
                        && typeList().getValue() == type ? Optional.of(type) : Optional.empty()));
        Workbench.pause(Duration.ofMillis(500));
    }

    /** The list of chart types of the chart in front: the first one of the legend. */
    @SuppressWarnings("unchecked")
    private static ComboBox<ChartType> typeList() {
        return legend().lookupAll(".combo-box").stream()
                .filter(ComboBox.class::isInstance).map(ComboBox.class::cast)
                .filter(list -> list.getItems().contains(ChartType.SUM))
                .findFirst().orElseThrow(() -> new AssertionError("The legend has no list of chart types"));
    }

    private static Node legend() {
        return shown("chartLegend");
    }

    @SuppressWarnings("unchecked")
    private static XYChart<Long, Double> baseChart() {
        return workbench.onFx(() -> {
            BorderPane container = (BorderPane) shown("chartContainer");
            return ((ZoomableStackedChart) container.getCenter()).getBaseChart().toXYChart();
        });
    }

    private static ZoneId timeZoneOf(XYChart<?, ?> chart) {
        return workbench.onFx(() -> ((DateAxis) chart.getXAxis()).getTimeZone());
    }

    /** The time zone chosen in the list at the lower left corner of the chart. */
    @SuppressWarnings("unchecked")
    private static ZoneId timeZoneOfThePanel() {
        return workbench.onFx(() -> ((ComboBox<ChartTimeZone>) shown("cbTimeZone")).getValue().getTimeZone());
    }

    private static Node shown(String id) {
        return workbench.find(id).orElseThrow(() -> new AssertionError("No control '" + id + "' is shown"));
    }
}
