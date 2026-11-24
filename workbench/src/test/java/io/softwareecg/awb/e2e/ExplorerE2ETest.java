package io.softwareecg.awb.e2e;

import io.softwareecg.awb.metricview.api.ChartInUse;
import io.softwareecg.awb.uitoolkit.chart.ChartType;
import io.softwareecg.awb.repository.connection.EkgRepository;
import io.softwareecg.awb.repository.api.RepositoryService;
import io.softwareecg.awb.repository.connection.RepositoryConfiguration;
import io.softwareecg.awb.explorer.tree.AbstractItem;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.softwareecg.wfx.lookup.api.Lookup;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The explorer: what its tree opens, with the keyboard and by the way that leads to a metric, and
 * what the menus of its nodes do.
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

    /** A test that failed half way leaves no repository behind: an import of the next could go into it. */
    @AfterEach
    void removeAddedRepositories() {
        RepositoryService service = Lookup.lookup(RepositoryService.class);
        service.listRepositories().stream().filter(repository -> !repository.isEmbedded())
                .forEach(service::removeRepositoryWithProjects);
    }

    /**
     * The values of the user are in the home of the user. Beside the application, where Solr looks
     * when nobody tells it, the next build or installation would delete them.
     */
    @Test
    void theIndexOfTheEmbeddedRepositoryIsInTheHomeOfTheUser() {
        Path index = Workbench.HOME.resolve(".ekg/solr/ekgdata/data/index");
        assertThat(index).as("the index").isDirectoryContaining("glob:**/segments_*");
        assertThat(System.getProperty("ekg.solr.baseDir")).isEqualTo(Workbench.HOME.resolve(".ekg/solr").toString());
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

    /**
     * A repository keeps its id when it is edited, because its projects refer to it. The dialog
     * comes up with what is stored. The server of this one cannot be reached: none of it needs it.
     */
    @Test
    void aRepositoryIsAddedEditedAndRemoved() {
        Stage add = workbench.openDialog("Repository", "Add Repository...");
        fill(add, "txtRepositoryName", "Measurements");
        fill(add, "txtRepositoryUrl", "http://127.0.0.1:1/solr/ekgdata");
        confirm(add, ButtonType.APPLY);
        TreeItem<Object> added = node("Measurements");
        EkgRepository stored = repository("Measurements");

        Stage edit = workbench.openDialog("Edit Repository", () -> choose(added, "Edit Repository"));
        workbench.screenshot(edit, "repository-edit-dialog");
        assertThat(contentOf(edit, "txtRepositoryName")).isEqualTo("Measurements");
        assertThat(contentOf(edit, "txtRepositoryUrl")).isEqualTo("http://127.0.0.1:1/solr/ekgdata");
        fill(edit, "txtRepositoryName", "Measurements of the plant");
        fill(edit, "txtRepositoryUrl", "http://127.0.0.1:1/solr/plant");
        confirm(edit, ButtonType.APPLY);

        TreeItem<Object> edited = node("Measurements of the plant");
        EkgRepository changed = repository("Measurements of the plant");
        assertThat(changed.getId()).as("id of the repository").isEqualTo(stored.getId());
        assertThat(changed.getConnectionUrl()).isEqualTo("http://127.0.0.1:1/solr/plant");
        assertThat(repositoryNames()).as("the repositories").doesNotContain("Measurements");

        workbench.onFx(() -> {
            choose(edited, "Remove from Explorer");
            return null;
        });
        awaitGone("Measurements of the plant");
        assertThat(repositoryNames()).doesNotContain("Measurements of the plant");
    }

    /** The menu of the root asks for a repository in the same dialog as the menu of the window. */
    @Test
    void theMenuOfTheRootAddsARepository() {
        TreeItem<Object> root = workbench.onFx(() -> tree().getRoot());
        Stage add = workbench.openDialog("Add EKG Repository", () -> choose(root, "Add EKG Repository"));
        assertThat(workbench.onFx(add::getTitle)).isEqualTo("Add EKG Repository");
        assertThat(workbench.onFx(add::getOwner)).as("the owner of the dialog").isSameAs(workbench.mainStage());
        assertThat(contentOf(add, "txtRepositoryName")).isNullOrEmpty();
        fill(add, "txtRepositoryName", "From the root");
        fill(add, "txtRepositoryUrl", "http://127.0.0.1:1/solr/ekgdata");
        confirm(add, ButtonType.APPLY);

        node("From the root");
        assertThat(repositoryNames()).contains("From the root");
    }

    /** Deleting a project deletes its values too - its own, and nothing of the project beside it. */
    @Test
    void aProjectIsDeletedWithItsDataAndNothingElse() throws URISyntaxException {
        Path logs = Path.of(ExplorerE2ETest.class.getClassLoader().getResource("e2e/collector").toURI());
        workbench.openImporter("EKG Collector Logs").projectName("To be deleted").files(logs).importData(Duration.ofMinutes(2));
        ImportedProject project = workbench.project("To be deleted");
        project.awaitPoints(330, Duration.ofSeconds(30));
        TreeItem<Object> item = node("Local Repository", "To be deleted");

        workbench.onFx(() -> {
            choose(item, "Delete project and its data");
            return null;
        });

        workbench.awaitTrue("the project to be deleted", Duration.ofSeconds(30), () -> !project.exists());
        project.awaitPoints(0, Duration.ofSeconds(30));
        awaitGone("Local Repository", "To be deleted");
        assertThat(workbench.project("Explorer").exists()).as("the project beside it").isTrue();
        assertThat(workbench.project("Explorer").points()).as("the values of the project beside it").isEqualTo(330);
    }

    /** A group of bookmarks is created and deleted in the menus of its nodes. */
    @Test
    void aGroupOfBookmarksIsCreatedAndDeleted() {
        TreeItem<Object> bookmarks = node("Local Repository", "Explorer", "Bookmarks");
        Stage create = workbench.openDialog("Create Bookmark Group", () -> choose(bookmarks, "Create Bookmark Group"));
        fill(create, "bookmarkGroupName", "Findings");
        confirm(create, ButtonType.APPLY);
        TreeItem<Object> group = node("Local Repository", "Explorer", "Bookmarks", "Findings");

        Stage warning = workbench.openDialog("Delete Group", () -> choose(group, "Delete Group"));
        confirm(warning, ButtonType.YES);
        awaitGone("Local Repository", "Explorer", "Bookmarks", "Findings");
    }

    /** A bookmark brings back the query and the type of the chart it was made of. */
    @Test
    void aBookmarkOfAProjectIsCreatedOpenedAndDeleted() {
        workbench.openChart("Explorer", "os", "\\Cpu\\us");
        chooseChartType(ChartType.AREA);
        bookmark("Cpu of the user", null);
        TreeItem<Object> bookmark = node("Local Repository", "Explorer", "Bookmarks", "Cpu of the user");

        workbench.openChart("Explorer", "os", "\\Load\\one");
        chooseChartType(ChartType.LINE);
        assertThat(metricOfTheHeader()).isEqualTo("\\Load\\one");

        open(bookmark);
        workbench.awaitTrue("the metric of the bookmark", Duration.ofSeconds(20), () -> "\\Cpu\\us".equals(metricOfTheHeader()));
        workbench.awaitTrue("the chart type of the bookmark", Duration.ofSeconds(20),
                () -> workbench.onFx(() -> chartTypes().getValue() == ChartType.AREA));

        chooseChartType(ChartType.LINE);
        workbench.onFx(() -> {
            choose(bookmark, "Delete Bookmark");
            return null;
        });
        awaitGone("Local Repository", "Explorer", "Bookmarks", "Cpu of the user");
    }

    /** A bookmark in a group that is not there yet creates the group, which is not of one project. */
    @Test
    void aBookmarkInANewGroupIsGlobal() {
        workbench.openChart("Explorer", "os", "\\Cpu\\us");
        bookmark("Cpu anywhere", "Shared");
        TreeItem<Object> bookmark = node("Local Repository", "Global Bookmarks", "Shared", "Cpu anywhere");

        workbench.openChart("Explorer", "os", "\\Load\\one");
        open(bookmark);
        workbench.awaitTrue("the metric of the bookmark", Duration.ofSeconds(20), () -> "\\Cpu\\us".equals(metricOfTheHeader()));

        TreeItem<Object> group = node("Local Repository", "Global Bookmarks", "Shared");
        Stage warning = workbench.openDialog("Delete Group", () -> choose(group, "Delete Group"));
        confirm(warning, ButtonType.YES);
        awaitGone("Local Repository", "Global Bookmarks", "Shared");
    }

    /** A global bookmark is a template: it shows the project that is looked at, every time it is opened. */
    @Test
    void aGlobalBookmarkOpensInTheProjectInUse() throws URISyntaxException {
        Path logs = Path.of(ExplorerE2ETest.class.getClassLoader().getResource("e2e/collector").toURI());
        workbench.openImporter("EKG Collector Logs").projectName("Second").files(logs).importData(Duration.ofMinutes(2));
        workbench.project("Second").awaitPoints(330, Duration.ofSeconds(30));
        workbench.openChart("Explorer", "os", "\\Cpu\\us");
        bookmark("Cpu of any project", "Templates");
        TreeItem<Object> bookmark = node("Local Repository", "Global Bookmarks", "Templates", "Cpu of any project");

        for (String project : List.of("Explorer", "Second", "Explorer")) {
            workbench.openChart(project, "os", "\\Load\\one");
            open(bookmark);
            workbench.awaitTrue("the metric of the bookmark", Duration.ofSeconds(20), () -> "\\Cpu\\us".equals(metricOfTheHeader()));
            assertThat(workbench.onFx(() -> Lookup.lookup(ChartInUse.class).shownChart().orElseThrow()
                    .query().getProjectName())).as("project of the chart").isEqualTo(project);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** Makes a bookmark of what the chart shows, in the group of this name if there is one. */
    private static void bookmark(String name, String group) {
        Stage dialog = workbench.openDialog("Bookmark it!", () -> ((Button) workbench.find("btBookmark").orElseThrow()).fire());
        fill(dialog, "bookmarkName", name);
        if (group != null) {
            workbench.onFx(() -> {
                Workbench.control(dialog, "fcbBookmarkGroup", ComboBox.class).getEditor().setText(group);
                return null;
            });
        }
        confirm(dialog, ButtonType.APPLY);
    }

    private static void open(TreeItem<Object> bookmark) {
        workbench.onFx(() -> {
            choose(bookmark, "Load in Chart View");
            return null;
        });
    }

    private static String metricOfTheHeader() {
        return workbench.onFx(() -> ((ComboBox<?>) workbench.find("cbxMetric").orElseThrow()).getEditor().getText());
    }

    private static void chooseChartType(ChartType type) {
        workbench.onFx(() -> {
            chartTypes().setValue(type);
            return null;
        });
        Workbench.pause(Duration.ofMillis(500));
    }

    /** The list of chart types in the legend, of the chart in front. */
    @SuppressWarnings("unchecked")
    private static ComboBox<ChartType> chartTypes() {
        return workbench.find("chartLegend").orElseThrow().lookupAll(".combo-box").stream()
                .filter(ComboBox.class::isInstance).map(ComboBox.class::cast)
                .filter(list -> list.getItems().contains(ChartType.SUM))
                .findFirst().orElseThrow(() -> new AssertionError("The legend has no list of chart types"));
    }

    /** Chooses an entry of the menu of a node. To be called on the JavaFX thread. */
    private static void choose(TreeItem<Object> item, String entry) {
        tree().scrollTo(tree().getRow(item));
        tree().layout();
        ((AbstractItem<?>) (TreeItem<?>) item).getContextMenu().getItems().stream()
                .filter(menuItem -> entry.equals(menuItem.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("The menu of '" + item.getValue() + "' has no entry '" + entry + "'"))
                .fire();
    }

    private static void fill(Stage dialog, String field, String text) {
        workbench.onFx(() -> {
            Workbench.control(dialog, field, TextField.class).setText(text);
            return null;
        });
    }

    private static String contentOf(Stage dialog, String field) {
        return workbench.onFx(() -> Workbench.control(dialog, field, TextField.class).getText());
    }

    /** Presses a button of a dialog once it can be pressed. */
    private static void confirm(Stage dialog, ButtonType type) {
        Button button = workbench.onFx(() -> (Button) ((DialogPane) dialog.getScene().getRoot()).lookupButton(type));
        workbench.awaitTrue("'" + type.getText() + "' to be enabled", Duration.ofSeconds(10),
                () -> workbench.onFx(() -> !button.isDisabled()));
        workbench.onFx(() -> {
            button.fire();
            return null;
        });
        workbench.awaitTrue("the dialog to close", Duration.ofSeconds(10), () -> workbench.onFx(() -> !dialog.isShowing()));
    }

    private static List<EkgRepository> repositories() {
        return Lookup.lookup(RepositoryConfiguration.class).listEkgRepositories();
    }

    private static List<String> repositoryNames() {
        return repositories().stream().map(EkgRepository::getRepositoryName).toList();
    }

    private static EkgRepository repository(String name) {
        return repositories().stream().filter(repository -> name.equals(repository.getRepositoryName())).findFirst()
                .orElseThrow(() -> new AssertionError("No repository '" + name + "' is stored"));
    }

    /** Waits until the last node of a way is no longer there. */
    private static void awaitGone(String... way) {
        String name = way[way.length - 1];
        TreeItem<Object> parent = node(Arrays.copyOf(way, way.length - 1));
        workbench.awaitTrue("the node '" + name + "' to be gone", Duration.ofSeconds(30), () -> {
            TreeItem<Object> current = way.length == 1 ? parent : node(Arrays.copyOf(way, way.length - 1));
            return workbench.onFx(() -> current.getChildren().stream().noneMatch(child -> isNamed(child, name)));
        });
    }

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
