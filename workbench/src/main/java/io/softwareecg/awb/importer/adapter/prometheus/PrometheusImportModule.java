package io.softwareecg.awb.importer.adapter.prometheus;

import io.softwareecg.awb.importer.ImportModule;
import io.softwareecg.awb.query.spi.RemoteSeriesSource;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.timeseries.Value;
import jakarta.inject.Singleton;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import io.softwareecg.awb.importer.framework.SplitSourceProjectImportStrategy;
import io.softwareecg.awb.importer.ui.dialogs.ImportDialog;
import io.softwareecg.awb.importer.ui.dialogs.queryimport.QueryImportDialog;
import javafx.scene.image.Image;

import static io.softwareecg.awb.repository.api.ResourceAuthType.*;

import io.softwareecg.awb.importer.spi.ClassicViewFlavorSupport;
import io.softwareecg.awb.importer.spi.CloudViewFlavorSupport;
import io.softwareecg.awb.importer.spi.LocalProjectSupport;
import io.softwareecg.awb.importer.spi.SplitSourceProjectSupport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Import module for time series data collected in a remote Prometheus instance.
 * The import module supports both local project data import and remote project data import
 * to provide the maximum flexible solution to analyse Prometheus time series.
 *
 * @since 13.07.2018
 */
@SuppressWarnings("unused") // resolved and instantiated via CDI classpath scan
@Singleton
public class PrometheusImportModule extends ImportModule implements SplitSourceProjectSupport, RemoteSeriesSource,
        LocalProjectSupport, CloudViewFlavorSupport, ClassicViewFlavorSupport {

    public static final String IMPORT_MODULE_ID = "PrometheusImporter";

    /**
     * The import dialog instance - stored to access source repository property for local imports
     */
    private QueryImportDialog importDialog;

    /**
     * One for every server: several projects can be shown side by side, each from a server of its own.
     */
    private final Map<TimeSeriesImportSource, PrometheusSeriesDataFetcher> fetchers = new ConcurrentHashMap<>();

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
    // implementation of SplitSourceProjectSupport interface
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public SplitSourceProjectImportStrategy getSplitSourceProjectImportStrategy() {
        return new PrometheusRemoteImportStrategy();
    }

    //------------------------------------------------------------------------------------------------------------------
    // implementation of RemoteSeriesSource interface
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public Value[] fetchSeries(TimeSeriesImportSource source, String seriesKey, long start, long end) {
        return fetchers.computeIfAbsent(source, PrometheusSeriesDataFetcher::new).fetchSeriesData(seriesKey, start, end);
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
