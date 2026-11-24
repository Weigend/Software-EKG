package io.softwareecg.awb.evidence.export;

import afester.javafx.svg.SvgLoader;
import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import io.softwareecg.awb.runtime.resources.ViewLoadResult;
import javafx.scene.layout.Region;
import javafx.scene.Group;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Dialog component that will used to setup the chart export as
 * image and to save it in file system.
 *
 * @since 16.11.2018
 */
public class ScreenshotExportDialog extends Dialog<Void> {

    /**
     * Constructs a new instance of the ScreenshotExportDialog
     * that will setup and export the given Node to an image file.
     *
     * @param nodeToExport the (chart) node to export
     * @param axisWidth    the width of the value axis of the chart
     */
    public ScreenshotExportDialog(Region nodeToExport, double axisWidth) {

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResource("png.png").toExternalForm()));

        setTitle("Export Chart as Bitmap Image");

        ViewLoadResult viewResult = CdiFxmlLoader.loadView("ScreenshotExportDialogView.fxml");
        setDialogPane(viewResult.getComponent());
        ScreenshotExportController controller = viewResult.getController();
        controller.setExporter(new ScreenshotExporter(nodeToExport, axisWidth));

        setResultConverter(controller::exportToPngSheet);

        Group iconImageUpdate = new SvgLoader().loadSvg(getClass().getResourceAsStream("screenshot-icon2.svg"));
        iconImageUpdate.setScaleX(0.5);
        iconImageUpdate.setScaleY(0.5);
        setGraphic(new Group(iconImageUpdate));
    }
}