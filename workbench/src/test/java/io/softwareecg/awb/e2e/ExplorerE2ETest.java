package io.softwareecg.awb.e2e;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import io.softwareecg.awb.common.ui.chartng.ZoomableStackedChart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The explorer: what its tree opens, with the keyboard and by the way that leads to a metric.
 */
class ExplorerE2ETest {

    private static Workbench workbench;

    @BeforeAll
    static void startTheWorkbenchWithAProject() throws URISyntaxException {
        workbench = Workbench.start();
        Path logs = Path.of(ExplorerE2ETest.class.getClassLoader().getResource("e2e/collector").toURI());
        ImportDialog dialog = workbench.openImporter("EKG Collector Logs");
        dialog.projectName("Explorer").files(logs).importData(Duration.ofMinutes(2));
        workbench.project("Explorer").awaitPoints(330, Duration.ofSeconds(30));
    }

    /**
     * Going down a list of metrics with the arrow keys shows one after the other. It used to
     * work for one step: the chart that opened took the focus, and the next key went to it.
     */
    @Test
    void theArrowKeysShowOneMetricAfterTheOther() {
        select(node("Local Repository", "Explorer", "Metrics", "os", "\\Cpu\\hi"));

        press(KeyCode.DOWN);
        awaitMetricOfTheChart("\\Cpu\\id");
        assertThat(focusOwner()).as("what has the focus after the first metric was shown").isSameAs(tree());

        press(KeyCode.DOWN);
        awaitMetricOfTheChart("\\Cpu\\ni");
        press(KeyCode.DOWN);
        awaitMetricOfTheChart("\\Cpu\\si");
        press(KeyCode.UP);
        awaitMetricOfTheChart("\\Cpu\\ni");
        assertThat(focusOwner()).as("what has the focus after four metrics").isSameAs(tree());
    }

    /**
     * The process group is a level of the tree, and a metric opened below one is filtered by
     * it. The header of the chart has a field for it; it had none and dropped the group, so
     * that the chart showed the series of all groups as soon as another field was changed.
     */
    @Test
    void aMetricBelowAProcessGroupFiltersTheChartByTheGroup() {
        select(node("Local Repository", "Explorer", "Processes", "system", "global", "Metrics", "os", "\\Load\\one"));
        press(KeyCode.ENTER);
        awaitMetricOfTheChart("\\Load\\one");

        assertThat(textOf("cbxProcessGroup")).as("process group in the header").isEqualTo("system");
        assertThat(textOf("cbxProcess")).as("process in the header").isEqualTo("global");
        awaitSeriesOfTheChart(1);

        // the header queries with the group: one that does not exist finds nothing
        type("cbxProcessGroup", "batch");
        awaitSeriesOfTheChart(0);
        type("cbxProcessGroup", "sys*");
        awaitSeriesOfTheChart(1);
    }

    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static TreeView<Object> tree() {
        return (TreeView<Object>) workbench.find("explorerTree").orElseThrow();
    }

    /** The node at the end of a way through the tree, which is opened level by level. */
    private static TreeItem<Object> node(String... way) {
        TreeItem<Object> item = workbench.onFx(() -> tree().getRoot());
        for (String name : way) {
            TreeItem<Object> parent = item;
            workbench.onFx(() -> {
                parent.setExpanded(true);
                return null;
            });
            item = Workbench.await("the node '" + name + "' below '" + parent.getValue() + "'", Duration.ofSeconds(30),
                    () -> workbench.onFx(() -> parent.getChildren().stream()
                            .filter(child -> isNamed(child, name)).findFirst()));
        }
        return item;
    }

    /**
     * Whether a node has this name. A repository shows the number of its series behind its
     * name once it has counted them - "Local Repository (660)" - which is the case when other
     * tests have imported before this one.
     */
    private static boolean isNamed(TreeItem<Object> node, String name) {
        String shown = String.valueOf(node.getValue());
        return shown.equals(name) || shown.startsWith(name + " (");
    }

    private static void select(TreeItem<Object> item) {
        workbench.onFx(() -> {
            tree().requestFocus();
            tree().getSelectionModel().select(item);
            return null;
        });
        Workbench.pause(Duration.ofMillis(500));
    }

    /** A key, sent to what has the focus - as the keyboard does. */
    private static void press(KeyCode key) {
        workbench.onFx(() -> {
            Node owner = focusOwner();
            Event.fireEvent(owner, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
            Event.fireEvent(owner, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", key, false, false, false, false));
            return null;
        });
    }

    private static Node focusOwner() {
        return workbench.onFx(() -> workbench.mainStage().getScene().getFocusOwner());
    }

    /** Types a value into a field of the header and confirms it with the Enter key. */
    private static void type(String id, String text) {
        workbench.onFx(() -> {
            ComboBox<?> field = (ComboBox<?>) workbench.find(id).orElseThrow();
            field.getEditor().requestFocus();
            field.getEditor().setText(text);
            Event.fireEvent(field, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
            Event.fireEvent(field, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.ENTER, false, false, false, false));
            return null;
        });
    }

    private static String textOf(String id) {
        return workbench.onFx(() -> workbench.find(id).map(field -> ((ComboBox<?>) field).getEditor().getText()).orElse(null));
    }

    private static void awaitMetricOfTheChart(String metric) {
        Workbench.await("the chart of the metric " + metric + ", not of " + textOf("cbxMetric"), Duration.ofSeconds(20),
                () -> metric.equals(textOf("cbxMetric")) ? Optional.of(metric) : Optional.empty());
    }

    private static void awaitSeriesOfTheChart(int expected) {
        Workbench.await(expected + " series in the chart", Duration.ofSeconds(20), () -> workbench.onFx(() -> {
            BorderPane container = (BorderPane) workbench.find("chartContainer").orElseThrow();
            XYChart<Long, Double> chart = ((ZoomableStackedChart) container.getCenter()).getBaseChart().toXYChart();
            return chart.getData().size() == expected ? Optional.of(expected) : Optional.empty();
        }));
    }
}
