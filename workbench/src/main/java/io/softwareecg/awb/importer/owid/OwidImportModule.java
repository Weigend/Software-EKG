package io.softwareecg.awb.importer.owid;

import io.softwareecg.awb.importer.owid.bl.OwidImportStrategy;
import io.softwareecg.awb.importer.owid.ui.OwidImportDialog;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuAware;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuEntry;
import io.softwareecg.awb.sdk.awbapi.project.ProjectTimeSeriesType;
import io.softwareecg.awb.metadata.model.AliasMappingDefinition;
import io.softwareecg.awb.sdk.awbapi.repository.AliasMappingProvider;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.sdk.importer.ImportModule;
import jakarta.inject.Singleton;
import io.softwareecg.awb.sdk.importer.api.ClassicViewFlavorSupport;
import io.softwareecg.awb.sdk.importer.api.LocalProjectSupport;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.ui.dialogs.ImportDialog;

import java.util.EnumMap;
import java.util.Map;

/**
 * Import module for OWID (Our World In Data) COVID-19 dataset.
 * Reads CSV/CSV.GZ files in OWID format and maps them to EKG time series
 * with COVID-specific dimension aliases (Continent, Country, etc.).
 *
 */
@SuppressWarnings("unused") // used by CDI scan and automatic instantiation
@Singleton
public class OwidImportModule extends ImportModule
        implements AliasMappingProvider, LocalProjectSupport, ClassicViewFlavorSupport, ContextMenuAware {

    private OwidImportDialog importDialog;

    //------------------------------------------------------------------------------------------------------------------
    // AliasMappingProvider: COVID-specific dimension labels
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public AliasMappingDefinition getAliasMappingDefinition() {
        return dimensions -> {
            Map<FilterDimension, String> aliasMap = new EnumMap<>(FilterDimension.class);
            aliasMap.put(FilterDimension.HOST_GROUP, "Continent");
            aliasMap.put(FilterDimension.HOST, "Country");
            aliasMap.put(FilterDimension.MEASUREMENT, "N/D");
            aliasMap.put(FilterDimension.PROCESS, "N/D");
            aliasMap.put(FilterDimension.METRIC_GROUP, "Main category");
            aliasMap.put(FilterDimension.METRIC_NAME, "Metric");
            return aliasMap.get(dimensions);
        };
    }

    //------------------------------------------------------------------------------------------------------------------
    // ImportModule API
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ProjectTimeSeriesType getSupportedProjectType() {
        return ProjectTimeSeriesType.SPECIALIZED;
    }

    @Override
    public ImportDialog getImportDialog() {
        if (importDialog == null) {
            importDialog = new OwidImportDialog();
            importDialog.setHeaderDescription("Import of OWID COVID-19 data");
            importDialog.setWindowsTitle("OWID COVID-19 Data Import");
        }
        return importDialog;
    }

    @Override
    public int getMenuPosition() {
        return MENU_GROUP_OTHER_DATA;
    }

    @Override
    public String getMenuText() {
        return "OWID COVID-19 Data";
    }

    @Override
    public String getImporterID() {
        return "OWID";
    }

    //------------------------------------------------------------------------------------------------------------------
    // LocalProjectSupport
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public LocalProjectImportStrategy getLocalProjectImportStrategy() {
        return new OwidImportStrategy(importDialog.getSelectedFilesPaths());
    }

    //------------------------------------------------------------------------------------------------------------------
    // ContextMenuAware
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ContextMenuEntry getImportContextMenuItem() {
        return () -> "Import OWID COVID-19 Data";
    }
}
