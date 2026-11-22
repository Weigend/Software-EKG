package io.softwareecg.awb.e2e;

import io.softwareecg.awb.application.base.EkgAvajeApplication;
import io.softwareecg.awb.common.ui.events.OpeningMode;
import io.softwareecg.awb.datasource.bl.DataCollectionService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryContextEvent;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.EventObject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * The real Workbench, started once in the JVM of the tests and operated the way a user does:
 * through its menus, dialogs and buttons.
 * <p>
 * It is the whole application: its window, its embedded Solr, its metadata database. What it
 * keeps goes into a directory of its own below {@code target/e2e}, never into the home of the
 * person running the tests. Controls are found by what they are (the menu item with this text,
 * the field with this id), not by where they are on the screen, so that a test survives a
 * change of the layout.
 * <p>
 * An application can be started only once in a JVM. The tests that use it therefore run in a
 * JVM of their own: {@code mvn -Pe2e}, or {@code run-e2e-tests.sh}.
 */
public final class Workbench {

    /** What the Workbench under test keeps: its home directory, and the pictures taken of it. */
    public static final Path WORK = Path.of("target", "e2e").toAbsolutePath();
    public static final Path HOME = WORK.resolve("home");
    public static final Path SCREENSHOTS = WORK.resolve("screenshots");

    private static final Duration STARTUP = Duration.ofMinutes(2);

    private static Workbench running;

    private final Stage mainStage;

    private Workbench(Stage mainStage) {
        this.mainStage = mainStage;
    }

    /**
     * @return the Workbench; started with an empty home directory if it does not run yet
     */
    public static synchronized Workbench start() {
        if (running == null) {
            deleteRecursively(WORK);
            createDirectories(HOME);
            createDirectories(SCREENSHOTS);
            // everything the application keeps is below the home of the user
            System.setProperty("user.home", HOME.toString());
            System.setProperty("ekg.solr.template", solrTemplate().toString());

            Thread application = new Thread(() -> EkgAvajeApplication.main(new String[0]), "workbench-under-test");
            application.setDaemon(true);
            application.start();

            Stage stage = await("the main window of the Workbench", STARTUP, Workbench::findMainStage);
            running = new Workbench(stage);
            // the importers register themselves after the window is there
            running.awaitTrue("the Import menu to be filled", STARTUP,
                    () -> running.onFx(() -> running.menu("Import").map(m -> m.getItems().size() >= 5).orElse(false)));
        }
        return running;
    }

    private static Path solrTemplate() {
        for (String candidate : List.of("src/main/solr", "target/solr")) {
            Path path = Path.of(candidate).toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        throw new IllegalStateException("No Solr configuration found; the tests have to run in the directory of the module");
    }

    private static Optional<Stage> findMainStage() {
        return onFxThread(() -> Window.getWindows().stream()
                .filter(Stage.class::isInstance).map(Stage.class::cast)
                .filter(Window::isShowing)
                .filter(stage -> stage.getTitle() != null && stage.getTitle().startsWith("Software EKG"))
                .findFirst());
    }

    // ---------------------------------------------------------------------------------------------
    // what a test does with the Workbench
    // ---------------------------------------------------------------------------------------------

    /**
     * Chooses an importer in the menu <b>Import</b>.
     *
     * @param menuText the text of the menu item, for example "OWID COVID-19 Data"
     * @return its dialog, shown and ready
     */
    public ImportDialog openImporter(String menuText) {
        return new ImportDialog(this, openDialog("Import", menuText));
    }

    /**
     * Chooses an item of a menu that opens a dialog.
     *
     * @param menu the text of the menu, for example "Help"
     * @param item the text of the item
     * @return the window that opened
     */
    public Stage openDialog(String menu, String item) {
        List<Window> before = onFx(() -> List.copyOf(Window.getWindows()));
        // not waited for: the dialog is modal, and choosing the item returns when it is closed
        Platform.runLater(() -> menuItem(menu, item).fire());
        return await("the dialog of '" + item + "'", Duration.ofSeconds(30), () -> onFx(() ->
                Window.getWindows().stream()
                        .filter(window -> !before.contains(window))
                        .filter(Stage.class::isInstance).map(Stage.class::cast)
                        .filter(Window::isShowing)
                        .findFirst()));
    }

    /**
     * Closes a dialog as its close button does.
     */
    public void close(Stage dialog) {
        onFx(() -> {
            dialog.close();
            return null;
        });
    }

    /**
     * Takes a picture of a dialog into {@link #SCREENSHOTS}.
     */
    public Path screenshot(Stage dialog, String name) {
        return screenshot((Window) dialog, name);
    }

    /**
     * @param menu the text of a menu of the main window
     * @return the texts of the items a user sees, from top to bottom, a line between them as "---"
     */
    public List<String> menuItems(String menu) {
        return onFx(() -> menu(menu).orElseThrow(() -> new AssertionError("The Workbench has no menu '" + menu + "'"))
                .getItems().stream()
                .filter(MenuItem::isVisible)
                .map(item -> item instanceof SeparatorMenuItem ? "---" : item.getText())
                .toList());
    }

    /**
     * @param project the name of a project
     * @return what the repository holds of it
     */
    public ImportedProject project(String project) {
        return new ImportedProject(this, project);
    }

    /**
     * Opens a chart of one metric of a project, as a double click on the metric in the
     * explorer does, and waits until its header is there.
     */
    public void openChart(String project, String metricGroup, String metric) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        QueryFilterParams query = (QueryFilterParams) new QueryFilterParams.Builder()
                .withProject(project)
                .withMetricGroup(metricGroup)
                .withMetric(metric)
                .withMultiMetricMode(false)
                .build();
        onFx(() -> {
            eventBus().publish(new QueryContextEvent(this, query, embeddedRepository(), OpeningMode.CLEAR_VIEW));
            return null;
        });
        awaitTrue("the chart of '" + metric + "'", Duration.ofSeconds(30),
                () -> onFx(() -> mainStage.getScene().getRoot().lookup("#chartHeaderGridPane") != null));
        pause(Duration.ofSeconds(2));
    }

    /**
     * @return the control of the main window with this id, if it is there. Every project has a
     *         chart panel of its own, with the same ids in each: the one that is shown is taken.
     */
    public Optional<Node> find(String id) {
        return onFx(() -> {
            Set<Node> all = mainStage.getScene().getRoot().lookupAll("#" + id);
            return all.stream().filter(Workbench::isShown).findFirst().or(() -> all.stream().findFirst());
        });
    }

    /** Whether a control is in a part of the window that is shown, not in a tab behind another. */
    private static boolean isShown(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return node.getScene() != null;
    }

    /**
     * Sets the size of the main window, to see how the Workbench copes with a small screen.
     */
    public void resize(double width, double height) {
        onFx(() -> {
            mainStage.setMaximized(false);
            mainStage.setWidth(width);
            mainStage.setHeight(height);
            return null;
        });
        pause(Duration.ofMillis(500));
    }

    /**
     * Takes a picture of the main window into {@link #SCREENSHOTS}.
     */
    public Path screenshot(String name) {
        return screenshot(mainStage, name);
    }

    public Stage mainStage() {
        return mainStage;
    }

    /**
     * @return the repository the Workbench has on board
     */
    public EkgRepository embeddedRepository() {
        return Lookup.lookup(DataCollectionService.class).getRepository();
    }

    // ---------------------------------------------------------------------------------------------
    // tools of the drivers
    // ---------------------------------------------------------------------------------------------

    Optional<Menu> menu(String text) {
        return mainStage.getScene().getRoot().lookupAll(".menu-bar").stream()
                .filter(MenuBar.class::isInstance).map(MenuBar.class::cast)
                .flatMap(bar -> bar.getMenus().stream())
                .filter(menu -> text.equals(menu.getText()))
                .findFirst();
    }

    private MenuItem menuItem(String menu, String item) {
        Menu found = menu(menu).orElseThrow(() -> new AssertionError("The Workbench has no menu '" + menu + "'"));
        return found.getItems().stream()
                .filter(candidate -> item.equals(candidate.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The menu '" + menu + "' has no item '" + item + "', but "
                        + found.getItems().stream().map(MenuItem::getText).toList()));
    }

    @SuppressWarnings("unchecked")
    private static EventBus<EventObject> eventBus() {
        return Lookup.lookup(EventBus.class);
    }

    Path screenshot(Window window, String name) {
        WritableImage image = onFx(() -> window.getScene().snapshot(null));
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        PixelReader pixels = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                picture.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        Path file = SCREENSHOTS.resolve(name + ".png");
        try {
            ImageIO.write(picture, "png", file.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    /** Runs something on the thread of the user interface and waits for what it returns. */
    <T> T onFx(Callable<T> action) {
        return onFxThread(action);
    }

    private static <T> T onFxThread(Callable<T> action) {
        if (Platform.isFxApplicationThread()) {
            return call(action);
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            Platform.runLater(() -> {
                try {
                    result.complete(action.call());
                } catch (Throwable e) {
                    result.completeExceptionally(e);
                }
            });
        } catch (IllegalStateException toolkitNotStartedYet) {
            return call(() -> null);
        }
        try {
            return result.get(60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException | TimeoutException e) {
            throw new IllegalStateException("The user interface did not answer", e);
        }
    }

    private static <T> T call(Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Waits until something is there, and fails with what was waited for if it does not come. */
    static <T> T await(String what, Duration timeout, Callable<Optional<T>> probe) {
        long end = System.nanoTime() + timeout.toNanos();
        while (true) {
            Optional<T> found = call(probe);
            if (found != null && found.isPresent()) {
                return found.get();
            }
            if (System.nanoTime() > end) {
                throw new AssertionError("Waited " + timeout.toSeconds() + " s for " + what);
            }
            pause(Duration.ofMillis(200));
        }
    }

    void awaitTrue(String what, Duration timeout, BooleanSupplier condition) {
        await(what, timeout, () -> condition.getAsBoolean() ? Optional.of(true) : Optional.empty());
    }

    static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Looks a control up by its id; the id is the {@code fx:id} or the {@code id} of the FXML. */
    static <T extends Node> T control(Window window, String id, Class<T> type) {
        Node node = window.getScene().getRoot().lookup("#" + id);
        if (node == null) {
            throw new AssertionError("The window '" + ((Stage) window).getTitle() + "' has no control with the id '" + id + "'");
        }
        return type.cast(node);
    }

    private static void createDirectories(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
