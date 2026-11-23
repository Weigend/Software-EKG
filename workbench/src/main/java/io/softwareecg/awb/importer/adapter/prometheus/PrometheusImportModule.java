package io.softwareecg.awb.importer.adapter.prometheus;

import io.softwareecg.awb.importer.spi.ContextMenuAware;
import io.softwareecg.awb.importer.spi.ContextMenuEntry;
import io.softwareecg.awb.importer.ImportModule;
import jakarta.inject.Singleton;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import io.softwareecg.awb.importer.framework.SplitSourceProjectImportStrategy;
import io.softwareecg.awb.importer.ui.dialogs.ImportDialog;
import io.softwareecg.awb.importer.ui.dialogs.queryimport.QueryImportDialog;
import javafx.scene.image.Image;

import static io.softwareecg.awb.secrets.model.ResourceAuthType.*;

import io.softwareecg.awb.importer.spi.ClassicViewFlavorSupport;
import io.softwareecg.awb.importer.spi.CloudViewFlavorSupport;
import io.softwareecg.awb.importer.spi.LocalProjectSupport;
import io.softwareecg.awb.importer.spi.RemoteSeriesDataFetcher;
import io.softwareecg.awb.importer.spi.SplitSourceProjectSupport;
/**
 * Import module for time series data collected in a remote Prometheus instance.
 * The import module supports both local project data import and remote project data import
 * to provide the maximum flexible solution to analyse Prometheus time series.
 *
 * @since 13.07.2018
 */
@SuppressWarnings("unused") // resolved and instantiated via CDI classpath scan
@Singleton
public class PrometheusImportModule extends ImportModule implements ContextMenuAware,
        SplitSourceProjectSupport, LocalProjectSupport, CloudViewFlavorSupport, ClassicViewFlavorSupport {

    public static final String IMPORT_MODULE_ID = "PrometheusImporter";

    /**
     * The import dialog instance - stored to access source repository property for local imports
     */
    private QueryImportDialog importDialog;

    //------------------------------------------------------------------------------------------------------------------
    // implementation of ImportModule API (implement abstract methods)
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ImportDialog getImportDialog() {
        if (importDialog == null) {
            importDialog = new QueryImportDialog();
            importDialog.setWindowsTitle("Prometheus Time-Series DB Importer");
            importDialog.setWindowIcon(new Image(getClass().getResourceAsStream("prometheus-logo.png")));
            importDialog.setHeaderDescription("Import of Prometheus time-series data with local & remote project support.");
            importDialog.setSupportedAuthTypes(NONE, USERNAME_PASSWORD, JWT_BEARER_TOKEN, OAUTH2_CLIENT_CREDENTIAL);
        }
        return importDialog;
    }

    @Override
    public int getMenuPosition() {
        return MENU_GROUP_SYSTEMS + 10;
    }

    @Override
    public String getMenuText() {
        return "Prometheus Data";
    }

    @Override
    public String getImporterID() {
        return IMPORT_MODULE_ID;
    }

    //------------------------------------------------------------------------------------------------------------------
    // implementation of ContextMenuAware interface
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ContextMenuEntry getImportContextMenuItem() {
        return () -> "Import Prometheus Data";
    }

    //------------------------------------------------------------------------------------------------------------------
    // implementation of SplitSourceProjectSupport interface
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public SplitSourceProjectImportStrategy getSplitSourceProjectImportStrategy() {
        return new PrometheusRemoteImportStrategy();
    }

    @Override
    public RemoteSeriesDataFetcher getSeriesDataFetcher() {
        return new PrometheusSeriesDataFetcher();
    }

    //------------------------------------------------------------------------------------------------------------------
    // implementation of LocalProjectSupport interface
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public LocalProjectImportStrategy getLocalProjectImportStrategy() {
        // Ensure import dialog is initialized
        if (importDialog == null) {
            getImportDialog();
        }
        return new PrometheusLocalImportStrategy(importDialog.getImportSourceRepositoryProperty()::getValue);
    }
}
