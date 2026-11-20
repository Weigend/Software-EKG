package io.softwareecg.awb.importer.yfinance.bl;

public record YFinanceImportOptions(
        boolean importPriceHistory,
        boolean importCorporateActions,
        boolean importIntraday,
        boolean importAdjustedClose,
        boolean importRepairedFlag,
        boolean useMetadataDimensions
) {
}
