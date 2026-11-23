package io.softwareecg.awb.importer.adapter.owid.ui;

import io.softwareecg.awb.importer.ui.dialogs.fileimport.FileImportDialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;

/**
 * Import dialog for OWID COVID-19 data files.
 * Uses the standard FileImportDialog layout with directory/file selection.
 *
 */
public class OwidImportDialog extends FileImportDialog {

    private static final Logger LOG = LoggerFactory.getLogger(OwidImportDialog.class);

    private static final String DATA_URL = "https://catalog.ourworldindata.org/garden/covid/latest/compact/compact.csv";

    @Override
    protected void loadView() {
        super.loadView();
        addDataSourceLink();
    }

    private void addDataSourceLink() {
        VBox header = (VBox) getTop();
        if (header == null) {
            return;
        }

        Hyperlink link = new Hyperlink(DATA_URL);
        link.setStyle("-fx-text-fill: #cce5ff; -fx-border-color: transparent; -fx-padding: 0 0 5 0;");
        link.setOnAction(e -> openUrl());
        header.getChildren().add(link);
    }

    private void openUrl() {
        new Thread(() -> {
            try {
                Desktop.getDesktop().browse(new URI(DATA_URL));
            } catch (Exception ex) {
                LOG.warn("Could not open browser for URL: {}", DATA_URL, ex);
            }
        }).start();
    }
}
