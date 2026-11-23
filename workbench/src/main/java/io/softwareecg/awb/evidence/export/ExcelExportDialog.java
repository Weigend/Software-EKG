package io.softwareecg.awb.evidence.export;

import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import io.softwareecg.awb.runtime.resources.ViewLoadResult;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * Dedicated Export dialog to setup the
 * ExcelExport more detailed
 *
 * Note: This dialog is currently not ready and not in use!!!
 *
 * @since 15.09.2018
 */
public class ExcelExportDialog extends Dialog<Void> {

    public ExcelExportDialog() {

        setTitle("Export Chart Data as Excel Sheet");

        ViewLoadResult viewResult = CdiFxmlLoader.loadView("ExcelExportDialogView.fxml");
        setDialogPane(viewResult.getComponent());

        //controller = viewResult.getController();

        // bind controller to Dialog closed with apply
        setResultConverter(this::exportToExcelSheet);

        setHeaderText("Select a path where the Excel sheet with the chart data will be saved.");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
    }

    protected Void exportToExcelSheet(ButtonType buttonType) {
        return null;
    }
}
