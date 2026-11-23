package io.softwareecg.awb.importer.adapter.yfinance.ui;

import io.softwareecg.awb.importer.ui.dialogs.fileimport.FileImportDialogController;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class YFinanceImportDialogController extends FileImportDialogController {

    private CheckBox cbPriceHistory;
    private CheckBox cbCorporateActions;
    private CheckBox cbIntraday;
    private CheckBox cbAdjustedClose;
    private CheckBox cbRepairedFlag;
    private CheckBox cbUseMetadataDimensions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        initExtendedComponents();
    }

    private void initExtendedComponents() {
        defaultFileSelector.setLabelText("Path Yahoo Finance Data:");

        Label label = new Label("Import Options:");
        label.getStyleClass().add("lblHeadlines");

        cbPriceHistory = new CheckBox("Import price history");
        cbPriceHistory.setSelected(true);

        cbCorporateActions = new CheckBox("Import corporate actions");
        cbCorporateActions.setSelected(true);

        cbIntraday = new CheckBox("Import intraday files");
        cbIntraday.setSelected(false);

        cbAdjustedClose = new CheckBox("Import adjusted close");
        cbAdjustedClose.setSelected(true);

        cbRepairedFlag = new CheckBox("Import repaired flag");
        cbRepairedFlag.setSelected(false);

        cbUseMetadataDimensions = new CheckBox("Use metadata for exchange / sector / country mapping");
        cbUseMetadataDimensions.setSelected(true);

        cbAdjustedClose.disableProperty().bind(cbPriceHistory.selectedProperty().not());
        cbRepairedFlag.disableProperty().bind(cbPriceHistory.selectedProperty().not());

        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(18);
        optionsGrid.setVgap(10);
        optionsGrid.setPadding(new Insets(4, 0, 4, 0));

        optionsGrid.add(cbPriceHistory, 0, 0);
        optionsGrid.add(cbCorporateActions, 1, 0);
        optionsGrid.add(cbIntraday, 0, 1);
        optionsGrid.add(cbAdjustedClose, 1, 1);
        optionsGrid.add(cbRepairedFlag, 0, 2);
        optionsGrid.add(cbUseMetadataDimensions, 1, 2);

        vbExtendedChilds.getChildren().addAll(label, optionsGrid);

        btnOK.disableProperty().unbind();
        btnOK.disableProperty().bind(
                projectSelector.isValidProjectDefinitionProperty().not()
                        .or(repositorySelector.selectedRepositoryProperty().isNull())
                        .or(defaultFileSelector.isValidFileSelectedProperty().not())
                        .or(Bindings.createBooleanBinding(
                                () -> !cbPriceHistory.isSelected()
                                        && !cbCorporateActions.isSelected()
                                        && !cbIntraday.isSelected(),
                                cbPriceHistory.selectedProperty(),
                                cbCorporateActions.selectedProperty(),
                                cbIntraday.selectedProperty()
                        )));
    }

    public boolean isImportPriceHistoryEnabled() {
        return cbPriceHistory.isSelected();
    }

    public boolean isImportCorporateActionsEnabled() {
        return cbCorporateActions.isSelected();
    }

    public boolean isImportIntradayEnabled() {
        return cbIntraday.isSelected();
    }

    public boolean isImportAdjustedCloseEnabled() {
        return cbAdjustedClose.isSelected();
    }

    public boolean isImportRepairedFlagEnabled() {
        return cbRepairedFlag.isSelected();
    }

    public boolean isUseMetadataDimensionsEnabled() {
        return cbUseMetadataDimensions.isSelected();
    }
}
