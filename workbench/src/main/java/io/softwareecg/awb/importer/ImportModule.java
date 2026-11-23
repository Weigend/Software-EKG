package io.softwareecg.awb.importer;

import io.softwareecg.awb.importer.spi.ImporterTreeIconProvider;
import io.softwareecg.awb.importer.spi.TreeIconProviderPluginRegistry;
import io.softwareecg.awb.projectcontext.spi.ProjectImporter;
import io.softwareecg.awb.projectcontext.api.ProjectSelector;
import io.softwareecg.awb.projectcontext.model.ProjectTimeSeriesType;
import io.softwareecg.awb.projectcontext.spi.AliasMappingProvider;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.importer.spi.ClassicViewFlavorSupport;
import io.softwareecg.awb.importer.spi.CloudViewFlavorSupport;
import io.softwareecg.awb.importer.spi.LocalProjectSupport;
import io.softwareecg.awb.importer.spi.SplitSourceProjectSupport;
import io.softwareecg.awb.importer.ui.dialogs.ImportDialog;
import io.softwareecg.awb.importer.ui.skins.DefaultImportTreeIconProvider;
import io.softwareecg.wfx.extension.uiutils.MenuUtil;
import io.softwareecg.wfx.platform.api.Module;
import io.softwareecg.wfx.platform.api.exceptions.PlatformException;
import io.softwareecg.wfx.windowmanager.api.ApplicationWindow;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Base class of all import modules that bootstraps the import module and provides
 * hooks to customize importer behaviour. This base class will be searched and instantiated by
 * CDI classpath scan.
 *
 * @since 14.07.2018
 */
public abstract class ImportModule implements Module, ProjectImporter {

    private static final Logger LOGGER = EkgLogger.get();

    /** First in the menu: what software systems measure - the files of a Collector, a monitoring server. */
    public static final int MENU_GROUP_SYSTEMS = 100;

    /** Then: data sets that have nothing to do with software. */
    public static final int MENU_GROUP_OTHER_DATA = 200;

    /** Last: importers that do not say where they belong. */
    public static final int MENU_GROUP_UNSORTED = 900;

    private static final String MENU_POSITION = "ekg.importer.menuPosition";

    private Stage importDialogModal = null;

    private ImportDialog importDialog = null;

    /**
     * Get the human readable module name.
     *
     * @return the readable id of the importer
     */
    public String getName() {
        return getImporterID();
    }


    /**
     * Preload the module while starting the application.
     * <p>
     * It will be executed in an separate thread while showing the splash screen.
     *
     * @throws PlatformException in case of any error while preloading the module.
     */
    public void preload() throws PlatformException {

        importDialog = getImportDialog();
        configureImportDialog(importDialog);

        Lookup.lookup(TreeIconProviderPluginRegistry.class).registerImportTreeIconProvider(getImporterID(), getTreeIconProvider());
    }

    /**
     * Configures the import dialog with project selector settings and import strategies.
     * This method is idempotent and can be called multiple times safely.
     *
     * @param importDialog the dialog to configure
     */
    protected void configureImportDialog(ImportDialog importDialog) {
        ProjectSelector projectSelector = importDialog.getProjectSelector();
        projectSelector.setSupportedTimeSeriesType(getSupportedProjectType());
        projectSelector.setImporterId(getImporterID());
        projectSelector.setCloudFlavorSupportEnabled(this instanceof CloudViewFlavorSupport);
        projectSelector.setClassicFlavorSupportEnabled(this instanceof ClassicViewFlavorSupport);

        if (this instanceof AliasMappingProvider) {
            projectSelector.setImporterAliasMappingProvider((AliasMappingProvider) this);
        }

        projectSelector.enableSplitSourceProjectSupport(this instanceof SplitSourceProjectSupport);

        if (this instanceof SplitSourceProjectSupport) {
            importDialog.setRemoteImportStrategy(((SplitSourceProjectSupport)this).getSplitSourceProjectImportStrategy());
        }

        projectSelector.enableLocalProjectSupport(this instanceof LocalProjectSupport);
        if (this instanceof LocalProjectSupport) {
            importDialog.setLocalImportStrategy(((LocalProjectSupport)this).getLocalProjectImportStrategy());
        }
    }

    @Override
    public void start() {
        bindToAwbFileMenu(getMenuText(), event -> showImportDialog(importDialog));
    }

    @Override
    public void stop() {
        // NoOp
    }

    public abstract ImportDialog getImportDialog();

    @Override
    public abstract String getMenuText();

    @Override
    public abstract String getImporterID();

    public ImporterTreeIconProvider getTreeIconProvider() {
        return new DefaultImportTreeIconProvider();
    }

    public ProjectTimeSeriesType getSupportedProjectType() {
        return ProjectTimeSeriesType.DEFAULT;
    }


    /**
     * Where the importer stands in the menu <b>Import</b> and wherever else the importers are
     * listed. Importers are ordered by this number and, where it is the same, by their text.
     * The hundreds are groups, with a line between them in the menu: {@link #MENU_GROUP_SYSTEMS}
     * for what software systems measure, {@link #MENU_GROUP_OTHER_DATA} for data sets that have
     * nothing to do with software. An importer that says nothing comes last.
     *
     * @return the position in the menu
     */
    @Override
    public int getMenuPosition() {
        return MENU_GROUP_UNSORTED;
    }

    /**
     * Orders the items of the importers and puts a line between their groups.
     */
    static List<MenuItem> menuItemsInOrder(List<MenuItem> items) {
        List<MenuItem> ordered = new ArrayList<>(items);
        // the lines are drawn anew for what is in the menu now
        ordered.removeIf(SeparatorMenuItem.class::isInstance);
        ordered.sort(Comparator.comparingInt(ImportModule::positionOf).thenComparing(MenuItem::getText));

        List<MenuItem> result = new ArrayList<>();
        for (MenuItem item : ordered) {
            if (!result.isEmpty() && groupOf(result.get(result.size() - 1)) != groupOf(item)) {
                result.add(new SeparatorMenuItem());
            }
            result.add(item);
        }
        return result;
    }

    private static int positionOf(MenuItem item) {
        return item.getProperties().get(MENU_POSITION) instanceof Integer position ? position : MENU_GROUP_UNSORTED;
    }

    private static int groupOf(MenuItem item) {
        return positionOf(item) / 100;
    }

    //----------------------------------------------------------------------------------------------------------------

    protected void bindToAwbFileMenu(String name, EventHandler<ActionEvent> clickHandler) {

        ObservableList<Menu> mainMenu = Lookup.lookup(ApplicationWindow.class).getMenu();

        if (mainMenu == null) {
            LOGGER.error("unable to register MenuItem " + name + " to menu bar because it couldn't resolved!");
            throw new IllegalStateException("menu bar doesn't exists");
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (mainMenu) {

            MenuItem importAreaStartMenuItem = MenuUtil.findItem(mainMenu, "importerMenuAreaStart", true);

            if (importAreaStartMenuItem == null) {
                LOGGER.error("unable to register MenuItem " + name
                        + " because the importer MenuItem area in File-Menu couldn't resolved!");
                throw new IllegalStateException("menu item area doesn't exists - check id's of the separators!");
            }

            ObservableList<MenuItem> fileMenuItems = importAreaStartMenuItem.getParentMenu().getItems();

            int menuItemIndex = fileMenuItems.indexOf(importAreaStartMenuItem) + 1;

            List<MenuItem> importerMenuItems = new ArrayList<>();

            while (!"importerMenuAreaEnd".equals(fileMenuItems.get(menuItemIndex).getId())) {
                importerMenuItems.add(fileMenuItems.remove(menuItemIndex));
            }

            MenuItem item = MenuUtil.createMenuItem(name, clickHandler);
            item.getProperties().put(MENU_POSITION, getMenuPosition());
            importerMenuItems.add(item);

            fileMenuItems.addAll(menuItemIndex, menuItemsInOrder(importerMenuItems));
        }
    }

    /**
     * Shows the import dialog of this module. Ensures the dialog is
     * properly configured with strategies before showing.
     * If the dialog has not been shown before, a new modal Stage is created and cached for reuse.
     */
    public void showImportDialog() {
        ImportDialog importDialog = getImportDialog();
        configureImportDialog(importDialog);
        showImportDialog(importDialog);
    }

    /**
     * Shows the import dialog with a pre-selected existing project.
     * The dialog will open in "existing project" mode with the given project selected.
     *
     * @param preselectedProjectName the project name to pre-select
     */
    @Override
    public void showImportDialog(String preselectedProjectName) {
        ImportDialog importDialog = getImportDialog();
        configureImportDialog(importDialog);
        showImportDialog(importDialog);
        // Defer pre-selection until after all initialization (repository setup,
        // project list loading, updateState resets) has completed
        javafx.application.Platform.runLater(() ->
                importDialog.getProjectSelector().preselectProject(preselectedProjectName)
        );
    }

    protected void showImportDialog(ImportDialog importDialog) {
        if (importDialogModal == null) {
            importDialogModal = setupImportDialog(importDialog);
        }

        importDialogModal.show();
    }

    protected Stage setupImportDialog(ImportDialog importDialog) {
        // Bewusst ohne feste Masse: Die pref size aus dem FXML ist fuer mehrere Dialoge
        // zu klein (OWID etwa braucht durch den zusaetzlichen Link 582 statt 540), eine
        // fest gesetzte Scene-Groesse schneidet sie unten ab. Die Groesse wird deshalb
        // beim Anzeigen aus dem fertigen Layout ermittelt.
        Scene scene = new Scene(importDialog);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(importDialog.getDialogTitle());

        if (importDialog.getDialogIcon() != null) {
            stage.getIcons().add(importDialog.getDialogIcon());
        }

        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(true);
        stage.initOwner(Lookup.lookup(ApplicationWindow.class).getStage());

        // Workaround fuer eine Groessenabweichung unter XWayland: Dort kommt die initiale
        // Groesse gelegentlich nicht am echten Fenster an. JavaFX haelt die Stage dann fuer
        // korrekt dimensioniert (gemessen: 740x619, passend zur pref size aus dem FXML),
        // waehrend das X-Fenster auf dem Default von 200x200 stehen bleibt und den Inhalt
        // abschneidet.
        //
        // sizeToScene() allein behebt das nicht: JavaFX haelt intern bereits den richtigen
        // Wert und stellt deshalb gar keine neue Groessenanforderung. Erst ein echter
        // Wertwechsel nach dem Mapping erzwingt sie - daher die Aenderung um einen Pixel
        // und zurueck. Der Handler entfernt sich selbst, damit eine spaeter vom Benutzer
        // veraenderte Fenstergroesse beim Wiederoeffnen erhalten bleibt.
        boolean[] firstShow = {true};
        stage.setOnShown(event -> {
            // Nur beim ersten Anzeigen aus dem Layout bestimmen. Danach steht die Groesse
            // fest - oder der Benutzer hat sie veraendert, und die soll erhalten bleiben.
            if (firstShow[0]) {
                stage.sizeToScene();
                firstShow[0] = false;
            }

            // Bei JEDEM Anzeigen noetig: Die Stage wird zwischengespeichert und
            // wiederverwendet, das Fenster beim Wiederoeffnen also neu gemappt - und
            // verliert dabei erneut seine Groesse. Die aktuellen Werte werden gelesen und
            // wieder gesetzt, damit eine vom Benutzer veraenderte Groesse erhalten bleibt.
            double width = stage.getWidth();
            double height = stage.getHeight();
            Platform.runLater(() -> {
                stage.setWidth(width + 1);
                stage.setHeight(height + 1);
                Platform.runLater(() -> {
                    stage.setWidth(width);
                    stage.setHeight(height);
                });
            });
        });

        stage.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                importDialog.updateState();
            }
        });

        return stage;
    }
}
