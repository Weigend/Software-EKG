package io.softwareecg.awb.uitoolkit.chart.legend;

import io.softwareecg.awb.uitoolkit.chart.ColoredChart;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * This class is a view component that represents on of the background charts and
 * provides a header bar with meta data and buttons to change chart type or remove it.
 * Additionally it list all series that belongs to to the according background chart.
 *
 * @since 30.08.2018
 */
public class LegendBackgroundChartItem extends LegendChartItem {

    /**
     * The decorated (styled) chart that is represented
     * by this LegendChartItem.
     */
    private final ColoredChart backgroundChart;

    /**
     * A wrapper container to layout labels for the chart
     * and according buttons that trigger the legend view
     * actions belongs to the chart.
     */
    private final HBox baseChartCtrlBar = new HBox();

    private final Button bringToFrontButton = new Button("Bring to Front");

    /**
     * Constructs a new instance of the LegendBaseChartItem
     * that will bind to the given chart.
     *
     * @param backgroundChart the underlying background chart that will bind to this legend item.
     */
    public LegendBackgroundChartItem(ColoredChart backgroundChart) {

        super(backgroundChart.getId());
        getStyleClass().add("legendBackgroundChartItem");

        this.backgroundChart = backgroundChart;

        initBackgroundCtrlBar();
        initSeriesItems(backgroundChart);
    }

    //================================================================================================================
    // internal helper to initialize the UI component
    //================================================================================================================

    private void initBackgroundCtrlBar() {
        Label backgroundChartLabel = new Label();
        backgroundChartLabel.textProperty().bind(backgroundChart.toXYChart().getYAxis().labelProperty());

        boolean isEmpty = backgroundChart.toXYChart().getData().isEmpty();

        baseChartCtrlBar.getStyleClass().add("backgroundChartCtrlBar");
        baseChartCtrlBar.getChildren().add(createChartVisibleCheckBox(isEmpty));
        baseChartCtrlBar.getChildren().add(backgroundChartLabel);
        baseChartCtrlBar.getChildren().add(createChartTypeCombo(backgroundChart, isEmpty));
        baseChartCtrlBar.getChildren().add(createBringToFrontButton());
        baseChartCtrlBar.getChildren().add(createDeleteButton());
        setTop(baseChartCtrlBar);
    }

    //================================================================================================================
    // Action handler for the LegendBaseChartItem main bar (checkbox, combo box & buttons)
    //================================================================================================================

    /**
     * The actions are set after the item is built; only they know whether this chart can be
     * brought to the front.
     */
    @Override
    public void setActionHandler(LegendActionCallbacks chartLegendActions) {
        super.setActionHandler(chartLegendActions);
        boolean possible = chartLegendActions.canBringToFront(backgroundChart.getId());
        bringToFrontButton.setDisable(!possible);
        bringToFrontButton.setTooltip(new Tooltip(possible
                ? "Makes this the chart in front, the one the filter header belongs to. "
                        + "What is in front now moves to the background."
                : "A chart calculated from other charts stays in the background: "
                        + "the filter header has no query to show for it."));
    }

    private Node createBringToFrontButton() {
        bringToFrontButton.setDisable(true);
        bringToFrontButton.setOnAction(event -> actions.bringToFront(backgroundChart.getId()));
        return bringToFrontButton;
    }

    private Node createDeleteButton() {

        Button clearButton = new Button("Remove");
        clearButton.setOnMouseClicked(event -> {
            actions.deleteBgChart(backgroundChart.getId());
            LegendBackgroundChartItem.this.requestFocus();
        });
        return clearButton;
    }
}
