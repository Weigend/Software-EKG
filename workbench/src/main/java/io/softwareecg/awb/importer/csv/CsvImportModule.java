package io.softwareecg.awb.importer.csv;

import io.softwareecg.awb.importer.csv.bl.CollectorLogImportStrategy;
import io.softwareecg.awb.importer.csv.ui.CsvImportDialog;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuAware;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuEntry;
import io.softwareecg.awb.sdk.importer.ImportModule;
import jakarta.inject.Singleton;
import io.softwareecg.awb.sdk.importer.api.ClassicViewFlavorSupport;
import io.softwareecg.awb.sdk.importer.api.CloudViewFlavorSupport;
import io.softwareecg.awb.sdk.importer.api.LocalProjectSupport;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.ui.dialogs.ImportDialog;
import javafx.scene.image.Image;

/**
 * Import module that imports EKG-Collector compatible csv files to Software EKG.
 * The importer supports the classic collector format (<v6.1) and the current extended format
 * which will bring cloud native filter data.
 *
 * @since 14.07.2018
 */
@SuppressWarnings("unused") // used by CDI scan and automatic instantiation
@Singleton
public class CsvImportModule extends ImportModule
        implements ContextMenuAware, LocalProjectSupport, CloudViewFlavorSupport, ClassicViewFlavorSupport {

    /**
     * The generic CsvImportDialog used to setup an import procedure
     */
    private CsvImportDialog importDialog;

    //------------------------------------------------------------------------------------------------------------------
    // implementation of ImportModule API (implement abstract methods)
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ImportDialog getImportDialog() {
        if (importDialog == null) {
            importDialog = new CsvImportDialog();
            importDialog.setHeaderDescription("Import of EKG-Collector " +
                    "CSV files of all previously published versions");
            importDialog.setWindowsTitle("EKG Collector-Log Import");
            importDialog.setWindowIcon(new Image(getClass().getResourceAsStream("ekg-collector-logo.png")));

        }

        return importDialog;
    }

    @Override
    public String getMenuText() {
        return "EKG Collector Logs";
    }

    @Override
    public String getImporterID() {
        return "CollectorCsvImporter";
    }

    //------------------------------------------------------------------------------------------------------------------
    // implementation of ContextMenuAware interface
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ContextMenuEntry getImportContextMenuItem() {
        return () -> "Import Collector Logs";
    }

    //------------------------------------------------------------------------------------------------------------------
    // implementation of LocalProjectSupport interface
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public LocalProjectImportStrategy getLocalProjectImportStrategy() {
        return new CollectorLogImportStrategy(importDialog.getSelectedFilesPaths());
    }
}
