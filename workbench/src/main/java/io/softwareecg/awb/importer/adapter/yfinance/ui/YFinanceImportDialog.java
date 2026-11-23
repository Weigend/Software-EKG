package io.softwareecg.awb.importer.adapter.yfinance.ui;

import io.softwareecg.awb.importer.ui.dialogs.fileimport.FileImportDialog;

public class YFinanceImportDialog extends FileImportDialog {

    @Override
    protected void loadView() {
        prefHeight(700);
        super.setController(new YFinanceImportDialogController());
        super.loadView();
    }

    private YFinanceImportDialogController getYFinanceController() {
        return (YFinanceImportDialogController) controller;
    }

    public boolean isImportPriceHistoryEnabled() {
        return getYFinanceController().isImportPriceHistoryEnabled();
    }

    public boolean isImportCorporateActionsEnabled() {
        return getYFinanceController().isImportCorporateActionsEnabled();
    }

    public boolean isImportIntradayEnabled() {
        return getYFinanceController().isImportIntradayEnabled();
    }

    public boolean isImportAdjustedCloseEnabled() {
        return getYFinanceController().isImportAdjustedCloseEnabled();
    }

    public boolean isImportRepairedFlagEnabled() {
        return getYFinanceController().isImportRepairedFlagEnabled();
    }

    public boolean isUseMetadataDimensionsEnabled() {
        return getYFinanceController().isUseMetadataDimensionsEnabled();
    }
}
