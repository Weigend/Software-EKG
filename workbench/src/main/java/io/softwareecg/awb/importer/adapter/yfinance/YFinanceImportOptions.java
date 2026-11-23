package io.softwareecg.awb.importer.adapter.yfinance;

public record YFinanceImportOptions(
        boolean importPriceHistory,
        boolean importCorporateActions,
        boolean importIntraday,
        boolean importAdjustedClose,
        boolean importRepairedFlag,
        boolean useMetadataDimensions
) {
}
