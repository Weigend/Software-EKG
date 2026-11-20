package io.softwareecg.awb.importer.csv.ui;

import io.softwareecg.awb.sdk.importer.ui.dialogs.fileimport.FileImportDialog;

/**
 * Specialized dialog variant of FileImportDialog that adds additional options
 * that will control the compute of time series data during the import.
 *
 * @since 14.07.2018
 */
public class CsvImportDialog extends FileImportDialog {

    /**
     * Resolves the URL of the fxml view to load for this ImportDialog instance
     * and trigger the load itself.
     *
     * This method should be overwritten by deriving classes which need to load it's own views.
     */
    @Override
    protected void loadView() {
        super.setController(new CsvImportDialogController());
        super.loadView();
    }
}
