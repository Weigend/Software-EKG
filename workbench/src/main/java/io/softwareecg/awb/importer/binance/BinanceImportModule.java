package io.softwareecg.awb.importer.binance;

import io.softwareecg.awb.importer.binance.bl.BinanceImportStrategy;
import io.softwareecg.awb.importer.binance.ui.BinanceImportDialog;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuAware;
import io.softwareecg.awb.sdk.awbapi.explorer.ContextMenuEntry;
import io.softwareecg.awb.sdk.awbapi.repository.AliasMappingDefinition;
import io.softwareecg.awb.sdk.awbapi.repository.AliasMappingProvider;
import io.softwareecg.awb.sdk.awbapi.repository.FilterDimension;
import io.softwareecg.awb.sdk.importer.ImportModule;
import jakarta.inject.Singleton;
import io.softwareecg.awb.sdk.importer.api.LocalProjectSupport;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.ui.dialogs.ImportDialog;

import java.util.HashMap;
import java.util.Map;

/**
 * Import module for Binance public market data (aggTrades).
 * Reads ZIP files from data.binance.vision and maps trade data
 * to EKG time series with crypto-specific dimension aliases.
 *
 */
@SuppressWarnings("unused") // used by CDI scan and automatic instantiation
@Singleton
public class BinanceImportModule extends ImportModule
        implements LocalProjectSupport, AliasMappingProvider, ContextMenuAware {

    private BinanceImportDialog importDialog;

    //------------------------------------------------------------------------------------------------------------------
    // AliasMappingProvider: crypto-specific dimension labels
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public AliasMappingDefinition getAliasMappingDefinition() {

        return new AliasMappingDefinition() {

            private final Map<FilterDimension, String> aliasMap = new HashMap<>();

            {
                aliasMap.put(FilterDimension.HOST_GROUP, "Exchange");
                aliasMap.put(FilterDimension.HOST, "Market");
                aliasMap.put(FilterDimension.NAMESPACE, "Quote Asset");
                aliasMap.put(FilterDimension.SERVICE, "Symbol");
                aliasMap.put(FilterDimension.MEASUREMENT, "Period");
                aliasMap.put(FilterDimension.METRIC_GROUP, "Category");
                aliasMap.put(FilterDimension.METRIC_NAME, "Metric");
            }

            @Override
            public String getAliasLabel(FilterDimension dimension) {
                return aliasMap.get(dimension);
            }
        };
    }

    //------------------------------------------------------------------------------------------------------------------
    // ImportModule API
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ImportDialog getImportDialog() {
        if (importDialog == null) {
            importDialog = new BinanceImportDialog();
            importDialog.setHeaderDescription("Import of Binance aggregated trade data " +
                    "(aggTrades ZIP files from data.binance.vision)");
            importDialog.setWindowsTitle("Binance Market Data Import");
        }
        return importDialog;
    }

    @Override
    public int getMenuPosition() {
        return MENU_GROUP_OTHER_DATA + 10;
    }

    @Override
    public String getMenuText() {
        return "Binance Market Data";
    }

    @Override
    public String getImporterID() {
        return "BinanceMarketDataImporter";
    }

    //------------------------------------------------------------------------------------------------------------------
    // LocalProjectSupport
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public LocalProjectImportStrategy getLocalProjectImportStrategy() {
        return new BinanceImportStrategy(importDialog.getSelectedFilesPaths());
    }

    //------------------------------------------------------------------------------------------------------------------
    // ContextMenuAware
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public ContextMenuEntry getImportContextMenuItem() {
        return () -> "Import Binance Data";
    }
}
