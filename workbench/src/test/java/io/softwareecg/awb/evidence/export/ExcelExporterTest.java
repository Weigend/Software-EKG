package io.softwareecg.awb.evidence.export;

import javafx.scene.chart.XYChart;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * The sheet of an export can be read, and holds what the chart shows.
 */
public class ExcelExporterTest {

    private static final long START = 1_700_000_000_000L;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void everySeriesHasItsTimesAndValues() throws Exception {
        File file = export(series("cpu", 2, 10), series("load", 1, 20));

        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("Date/Time - cpu", header.getCell(0).getStringCellValue());
            assertEquals("Value - cpu", header.getCell(1).getStringCellValue());
            assertEquals("Date/Time - load", header.getCell(2).getStringCellValue());
            assertEquals("Value - load", header.getCell(3).getStringCellValue());

            Row first = sheet.getRow(1);
            assertEquals(new Date(START), first.getCell(0).getDateCellValue());
            assertEquals("dd.mm.yyyy hh:mm:ss.000", first.getCell(0).getCellStyle().getDataFormatString());
            assertEquals(10, first.getCell(1).getNumericCellValue(), 0);
            assertEquals(new Date(START), first.getCell(2).getDateCellValue());
            assertEquals(20, first.getCell(3).getNumericCellValue(), 0);

            Row second = sheet.getRow(2);
            assertEquals(new Date(START + 1000), second.getCell(0).getDateCellValue());
            assertEquals(11, second.getCell(1).getNumericCellValue(), 0);
            assertNull(second.getCell(2));
        }
    }

    /** As many points as 50 series of a chart have. A style for every cell ended at 64,000. */
    @Test
    public void aFullChartCanBeExported() throws Exception {
        ExcelExportSeriesData[] chart = new ExcelExportSeriesData[50];
        for (int i = 0; i < chart.length; i++) {
            chart[i] = series("metric " + i, 2000, i);
        }
        File file = export(chart);

        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            Row last = workbook.getSheetAt(0).getRow(2000);
            assertEquals(49 + 1999, last.getCell(99).getNumericCellValue(), 0);
            assertEquals(new Date(START + 1999 * 1000), last.getCell(98).getDateCellValue());
        }
    }

    private File export(ExcelExportSeriesData... series) throws Exception {
        File file = folder.newFile("export.xlsx");
        new ExcelExporter(file).createExcelSheetFromSeriesData(List.of(series));
        return file;
    }

    /** A series with a value every second. */
    private static ExcelExportSeriesData series(String name, int points, double firstValue) {
        List<XYChart.Data<Long, Double>> data = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            data.add(new XYChart.Data<>(START + i * 1000L, firstValue + i));
        }
        return new ExcelExportSeriesData(name, data);
    }
}
