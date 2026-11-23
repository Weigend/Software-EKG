package io.softwareecg.awb.importer.yfinance;

import io.softwareecg.awb.importer.yfinance.bl.YFinanceImportOptions;
import io.softwareecg.awb.importer.yfinance.bl.YFinanceImportStrategy;
import io.softwareecg.awb.importer.yfinance.ui.YFinanceImportDialog;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuAware;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuEntry;
import io.softwareecg.awb.metadata.model.AliasMappingDefinition;
import io.softwareecg.awb.sdk.awbapi.repository.AliasMappingProvider;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.sdk.importer.ImportModule;
import jakarta.inject.Singleton;
import io.softwareecg.awb.sdk.importer.api.ClassicViewFlavorSupport;
import io.softwareecg.awb.sdk.importer.api.LocalProjectSupport;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.ui.dialogs.ImportDialog;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Singleton
public class YFinanceImportModule extends ImportModule
        implements LocalProjectSupport, ClassicViewFlavorSupport, AliasMappingProvider, ContextMenuAware {

    private YFinanceImportDialog importDialog;

    @Override
    public AliasMappingDefinition getAliasMappingDefinition() {
        return new AliasMappingDefinition() {
            private final Map<FilterDimension, String> aliasMap = new HashMap<>();

            {
                aliasMap.put(FilterDimension.HOST_GROUP, "Value Group");
                aliasMap.put(FilterDimension.HOST, "Value");
                aliasMap.put(FilterDimension.MEASUREMENT, "Period");
                aliasMap.put(FilterDimension.PROCESS_GROUP, "Country");
                aliasMap.put(FilterDimension.PROCESS, "Exchange");
                aliasMap.put(FilterDimension.METRIC_GROUP, "Metric Group");
                aliasMap.put(FilterDimension.METRIC_NAME, "Metric");
            }

            @Override
            public String getAliasLabel(FilterDimension dimension) {
                return aliasMap.get(dimension);
            }
        };
    }

    @Override
    public ImportDialog getImportDialog() {
        if (importDialog == null) {
            importDialog = new YFinanceImportDialog();
            importDialog.setHeaderDescription("Import Yahoo Finance historical market data "
                    + "(CSV/JSON directories created by the yfinance downloader)");
            importDialog.setWindowsTitle("Yahoo Finance Market Data Import");
        }
        return importDialog;
    }

    @Override
    public int getMenuPosition() {
        return MENU_GROUP_OTHER_DATA + 20;
    }

    @Override
    public String getMenuText() {
        return "Yahoo Finance Market Data";
    }

    @Override
    public String getImporterID() {
        return "YFinanceMarketDataImporter";
    }

    @Override
    public LocalProjectImportStrategy getLocalProjectImportStrategy() {
        // The strategy is created once, with the dialog. What is chosen in the dialog is
        // therefore asked for when an import runs: asked here, it would be the defaults for ever.
        return new YFinanceImportStrategy(importDialog.getSelectedFilesPaths(), () -> new YFinanceImportOptions(
                importDialog.isImportPriceHistoryEnabled(),
                importDialog.isImportCorporateActionsEnabled(),
                importDialog.isImportIntradayEnabled(),
                importDialog.isImportAdjustedCloseEnabled(),
                importDialog.isImportRepairedFlagEnabled(),
                importDialog.isUseMetadataDimensionsEnabled()
        ));
    }

    @Override
    public ContextMenuEntry getImportContextMenuItem() {
        return () -> "Import Yahoo Finance Data";
    }
}
