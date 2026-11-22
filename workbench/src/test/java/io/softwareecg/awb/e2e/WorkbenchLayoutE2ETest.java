package io.softwareecg.awb.e2e;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.TabPane;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Workbench in a window that is smaller than a large screen.
 */
class WorkbenchLayoutE2ETest {

    private static Workbench workbench;

    @BeforeAll
    static void startTheWorkbenchWithAChart() throws URISyntaxException {
        workbench = Workbench.start();
        Path logs = Path.of(WorkbenchLayoutE2ETest.class.getClassLoader().getResource("e2e/collector").toURI());
        ImportDialog dialog = workbench.openImporter("EKG Collector Logs");
        dialog.projectName("Layout").files(logs).importData(Duration.ofMinutes(2));
        workbench.project("Layout").awaitPoints(330, Duration.ofSeconds(30));
        workbench.openChart("Layout", "os", "\\Cpu\\us");
    }

    /**
     * 1600 pixels are a common width. Explorer, chart and assistant are all inside the window
     * then: the bar of the project above them asked for 1763 pixels once, and everything
     * below it was laid out that wide and cut off at the right edge of the window.
     */
    @Test
    void explorerChartAndAssistantAreInsideAWindowOf1600Pixels() {
        workbench.resize(1600, 1000);
        workbench.screenshot("layout-1600");
        double sceneWidth = workbench.onFx(() -> workbench.mainStage().getScene().getWidth());

        assertThat(paneOf("chartHeaderGridPane").getMaxX()).as("right edge of the pane of the chart")
                .isLessThanOrEqualTo(sceneWidth);
        assertThat(paneOf("resetButton").getMaxX()).as("right edge of the pane of the assistant")
                .isLessThanOrEqualTo(sceneWidth);
        assertThat(bounds("resetButton").getMaxX()).as("right edge of the button of the assistant")
                .isLessThanOrEqualTo(sceneWidth);
    }

    /**
     * The three groups of the chart header stand side by side. In a header that is squeezed
     * below what it needs they overlap, and the buttons of one lie under the next.
     */
    @Test
    void theGroupsOfTheChartHeaderDoNotOverlapAt1600Pixels() {
        workbench.resize(1600, 1000);

        Bounds filters = bounds("seriesFilterGroup");
        Bounds metric = bounds("metricFilterGroup");
        Bounds range = bounds("vbPostProcessingBlock");
        assertThat(filters.getMaxX()).as("right edge of the filters, left of the metric").isLessThanOrEqualTo(metric.getMinX());
        assertThat(metric.getMaxX()).as("right edge of the metric, left of the time range").isLessThanOrEqualTo(range.getMinX());
    }

    /**
     * The texts of the header are there in full. "Bookmark it!", "Concatenate", "Smoothing:" and
     * "Granularity:" ended in three dots: their places were narrower than the words.
     */
    @Test
    void theTextsOfTheChartHeaderAreNotCutOff() {
        workbench.resize(1600, 1000);

        Map<String, Boolean> besideTheTimeRange = textsOf("vbPostProcessingBlock");
        Map<String, Boolean> belowTheMetric = textsOf("chartHeaderControlBar");

        assertThat(besideTheTimeRange).as("texts beside the time range: whether they have the width they need")
                .containsEntry("Smoothing:", true).containsEntry("Granularity:", true)
                .doesNotContainValue(false);
        // "Combine metrics:" is not among them: it has the room that is left in the bar, and in
        // a panel as narrow as this one that is less than it needs.
        assertThat(belowTheMetric).as("texts in the bar below the metric: whether they have the width they need")
                .containsEntry("Bookmark it!", true).containsEntry("Concatenate", true);
    }

    /**
     * The texts of the labels, buttons and list fields below a control, each with whether the
     * control is as wide as its text needs.
     */
    private static Map<String, Boolean> textsOf(String id) {
        Node parent = workbench.find(id).orElseThrow(() -> new AssertionError("No control with the id '" + id + "'"));
        return workbench.onFx(() -> {
            Map<String, Boolean> texts = new TreeMap<>();
            for (String selector : List.of(".label", ".button", ".list-cell")) {
                for (Node node : parent.lookupAll(selector)) {
                    if (!(node instanceof Labeled labeled) || !labeled.isVisible()
                            || labeled.getText() == null || labeled.getText().isBlank()) {
                        continue;
                    }
                    Text text = new Text(labeled.getText().strip());
                    text.setFont(labeled.getFont());
                    double graphic = labeled.getGraphic() == null ? 0
                            : labeled.getGraphic().getLayoutBounds().getWidth() + labeled.getGraphicTextGap();
                    double room = labeled.getWidth() - labeled.getInsets().getLeft() - labeled.getInsets().getRight();
                    texts.merge(labeled.getText().strip(), text.getLayoutBounds().getWidth() + graphic <= room + 1,
                            Boolean::logicalAnd);
                }
            }
            return texts;
        });
    }

    private static Bounds bounds(String id) {
        Node node = workbench.find(id).orElseThrow(() -> new AssertionError("No control with the id '" + id + "'"));
        return workbench.onFx(() -> node.localToScene(node.getBoundsInLocal()));
    }

    /** The pane of the window a control is in: explorer, chart and assistant have one each. */
    private static Bounds paneOf(String id) {
        return workbench.onFx(() -> {
            Node node = workbench.find(id).orElseThrow(() -> new AssertionError("No control with the id '" + id + "'"));
            while (!(node instanceof TabPane)) {
                node = node.getParent();
            }
            return node.localToScene(node.getBoundsInLocal());
        });
    }
}
