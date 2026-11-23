package io.softwareecg.awb.importer.adapter.binance;

import io.softwareecg.awb.importer.adapter.binance.ui.BinanceImportDialog;
import io.softwareecg.awb.metadata.model.AliasMappingDefinition;
import io.softwareecg.awb.projectcontext.spi.AliasMappingProvider;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.importer.ImportModule;
import jakarta.inject.Singleton;
import io.softwareecg.awb.importer.spi.LocalProjectSupport;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import io.softwareecg.awb.importer.ui.dialogs.ImportDialog;

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
        implements LocalProjectSupport, AliasMappingProvider {

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
}
