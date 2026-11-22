package io.softwareecg.awb.project.ui.projectbar;

import io.softwareecg.awb.project.api.ProjectDataCollection;
import io.softwareecg.awb.project.api.events.ProjectDataCollectionChangedEvent;
import io.softwareecg.awb.project.api.events.ShowProjectDataSourcesEvent;
import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import java.time.ZoneId;
import io.softwareecg.awb.project.api.ProjectViewFlavorChangedEvent;
import io.softwareecg.awb.project.api.events.ProjectSelectedEvent;
import io.softwareecg.awb.project.api.model.Project;
import io.softwareecg.awb.project.ui.mgrdialog.ProjectEditDialog;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.sdk.awbapi.project.CloudPlatformType;
import io.softwareecg.awb.sdk.awbapi.project.ProjectFlavor;
import io.softwareecg.awb.sdk.awbapi.project.ProjectViewFlavor;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.sdk.importer.ImportModule;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * This controller manages the {@link ProjectBar} UI component
 * and it's actions.
 *
 * @since 21.06.2018
 */
public class ProjectBarController implements Initializable {

    @FXML
    private ComboBox<ProjectViewFlavor> cbViewFlavor;

    @FXML
    private Button btnEditProject;

    @FXML
    private Button btnImportDataToProject;

    @FXML
    private Label lbProjectId;

    @FXML
    private Label lbProjectType;

    @FXML
    private Label lbProjectFlavor;

    @FXML
    private Label lbPlatformFlavor;

    @FXML
    private Label lbCreatedDate;

    @FXML
    private Label lbModifiedDate;

    @FXML
    private Label lbDataCollectionTitle;

    /**
     * Whether data is being collected for the selected project; a click opens its data sources.
     */
    @FXML
    private Label lbDataCollection;

    /** The widths of the two columns that show the data collection, when there is one. */
    private static final double DATA_TITLE_WIDTH = 38;
    private static final double DATA_VALUE_WIDTH = 135;

    private static final DateTimeFormatter LAST_VALUE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Tells whether data is being collected for a project; {@code null} if the application
     * has no data source module.
     */
    private ProjectDataCollection dataCollection;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private GridPane gpProjectBar;

    private EventBus<EventObject> eventBus;

    private Project activeProject = null;

    /**
     * the EKG repository that stores the active (chosen) project
     * viewed in the ProjectBar
     */
    private EkgRepository activeEkgRepository = null;

    private final Map<ProjectViewFlavor, BooleanProperty> disablePropertyMap = new HashMap<>();

    //=================================================================================================================
    //  implementation of Initializable interface
    //=================================================================================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        disablePropertyMap.put(ProjectViewFlavor.LOGICAL_VIEW, new SimpleBooleanProperty(true));
        disablePropertyMap.put(ProjectViewFlavor.PHYSICAL_VIEW, new SimpleBooleanProperty(true));

        eventBus = Lookup.lookup(EventBus.class);

        // Manual subscription required because this controller is instantiated
        // by FXMLLoader, not CDI — so @EventSubscriber is not processed
        eventBus.subscribe(ProjectSelectedEvent.class, event -> {
            changeProject((ProjectSelectedEvent) event);
            return true;
        });

        try {
            dataCollection = Lookup.find(ProjectDataCollection.class).orElse(null);
        } catch (RuntimeException e) {
            dataCollection = null;
        }
        eventBus.subscribe(ProjectDataCollectionChangedEvent.class, event -> {
            String changed = ((ProjectDataCollectionChangedEvent) event).getProjectName();
            Platform.runLater(() -> {
                if (activeProject != null && activeProject.getName().equals(changed)) {
                    showDataCollection();
                }
            });
            return true;
        });
        lbDataCollection.setOnMouseClicked(event -> {
            if (activeProject != null) {
                eventBus.publish(new ShowProjectDataSourcesEvent(this, activeProject.getName()));
            }
        });

        cbViewFlavor.setSkin(new ViewFlavorComboBoxSkin<>(cbViewFlavor, disablePropertyMap));

        cbViewFlavor.getItems().addAll(
                ProjectViewFlavor.PHYSICAL_VIEW,
                ProjectViewFlavor.LOGICAL_VIEW
        );

        cbViewFlavor.setValue(null);
        cbViewFlavor.valueProperty().addListener((observable, oldValue, newValue) -> {
            eventBus.publish(new ProjectViewFlavorChangedEvent(this, activeProject, oldValue, newValue));
        });

        btnEditProject.setOnMouseClicked(event -> new ProjectEditDialog(activeEkgRepository, activeProject).showAndWait());

        btnImportDataToProject.setOnAction(event -> handleImportData());
    }

    //=================================================================================================================
    //  controller own API
    //=================================================================================================================

    @EventSubscriber(eventClass = ProjectSelectedEvent.class)
    public void changeProject(ProjectSelectedEvent event) {

        gpProjectBar.setDisable(false);

        activeProject = event.getSelectedProject();
        activeEkgRepository = event.getRepository();

        lbProjectId.setText(activeProject.getName());
        lbProjectType.setText(activeProject.useSplitSource() ? "Fremdsystem" : "EKG Repository");

        ProjectFlavor flavor = activeProject.getProjectFlavor();
        if (flavor != null) {
            String flavorName = flavor.getName();
            int parenIdx = flavorName.indexOf('(');
            lbProjectFlavor.setText(parenIdx > 0 ? flavorName.substring(0, parenIdx).trim() : flavorName);
        } else {
            lbProjectFlavor.setText("-");
        }

        // a project without platform type is not on a cloud platform
        CloudPlatformType cloudPlatformType = activeProject.getCloudPlatformType();
        if (cloudPlatformType == null || cloudPlatformType == CloudPlatformType.NONE) {
            lbPlatformFlavor.setText("Bare-Metal");
            disablePropertyMap.get(ProjectViewFlavor.PHYSICAL_VIEW).setValue(false);
            disablePropertyMap.get(ProjectViewFlavor.LOGICAL_VIEW).setValue(true);
            cbViewFlavor.setValue(ProjectViewFlavor.PHYSICAL_VIEW);

        } else  {
            if (cloudPlatformType == CloudPlatformType.OTHER) {
                lbPlatformFlavor.setText("Other");
            } else {
                lbPlatformFlavor.setText(cloudPlatformType.getName());
            }
            ProjectFlavor activeFlavor = activeProject.getProjectFlavor();
            disablePropertyMap.get(ProjectViewFlavor.PHYSICAL_VIEW).setValue(activeFlavor == ProjectFlavor.CLASSIC);
            disablePropertyMap.get(ProjectViewFlavor.LOGICAL_VIEW).setValue(false);
            cbViewFlavor.setValue(ProjectViewFlavor.LOGICAL_VIEW);
        }

        btnImportDataToProject.setDisable(false);

        LocalDateTime created = activeProject.getCreatedDate();
        lbCreatedDate.setText(created != null ? created.format(DATE_FORMAT) : "-");

        LocalDateTime modified = activeProject.getLastModifiedDate();
        lbModifiedDate.setText(modified != null ? modified.format(DATE_FORMAT) : "-");

        showDataCollection();
    }

    private static void setColumnWidth(GridPane bar, int column, double width) {
        ColumnConstraints constraints = bar.getColumnConstraints().get(column);
        constraints.setMinWidth(width);
        constraints.setPrefWidth(width);
        constraints.setMaxWidth(width);
    }

    /**
     * Shows whether data is being collected for the selected project: "live" with the time of
     * the newest value, or what keeps it from being live. Hidden for a project without sources.
     */
    private void showDataCollection() {
        ProjectDataCollection.Status status = dataCollection == null || activeProject == null
                ? ProjectDataCollection.Status.NONE
                : dataCollection.statusOf(activeProject.getName());
        boolean collecting = status.state() != ProjectDataCollection.State.NONE;
        lbDataCollectionTitle.setVisible(collecting);
        lbDataCollection.setVisible(collecting);

        // a project without data sources leaves no gap in the bar
        if (lbDataCollection.getParent() instanceof GridPane bar) {
            setColumnWidth(bar, GridPane.getColumnIndex(lbDataCollectionTitle), collecting ? DATA_TITLE_WIDTH : 0);
            setColumnWidth(bar, GridPane.getColumnIndex(lbDataCollection), collecting ? DATA_VALUE_WIDTH : 0);
        }
        if (!collecting) {
            return;
        }
        String text = "\u25CF " + status.state().label();
        if (status.state() == ProjectDataCollection.State.RUNNING && status.lastValue() != null) {
            text += " \u00B7 " + LAST_VALUE_FORMAT.format(status.lastValue());
        }
        lbDataCollection.setText(text);
        lbDataCollection.setStyle("-fx-cursor: hand; -fx-text-fill: " + switch (status.state()) {
            case RUNNING -> "#4ac26b";
            case WAITING -> "#eac54f";
            case ERROR -> "#ff8182";
            default -> "#afb8c1";
        } + ";");
        lbDataCollection.setTooltip(new Tooltip("Data collection of this project: " + status.state().label()
                + (status.lastValue() == null ? "" : ", last value at " + LAST_VALUE_FORMAT.format(status.lastValue()))
                + (status.detail() == null ? "" : "\n" + status.detail())
                + "\nClick to see the data sources."));
    }

    public void setGpProjectBar(GridPane gpProjectBarController) {
        this.gpProjectBar = gpProjectBarController;
    }

    //=================================================================================================================
    //  import data handling
    //=================================================================================================================

    private void handleImportData() {
        List<ImportModule> importModules = Lookup.lookupAll(ImportModule.class);
        if (importModules == null || importModules.isEmpty()) {
            return;
        }

        String projectName = activeProject.getName();

        // if the project has a known importer, open its dialog directly
        String importerId = activeProject.getImporterId();
        if (StringUtils.isNotBlank(importerId)) {
            for (ImportModule module : importModules) {
                if (importerId.equals(module.getImporterID())) {
                    module.showImportDialog(projectName);
                    return;
                }
            }
        }

        // otherwise show a context menu with all available importers
        ContextMenu contextMenu = new ContextMenu();
        for (ImportModule module : ImportModule.inMenuOrder(importModules)) {
            MenuItem item = new MenuItem(module.getMenuText());
            item.setOnAction(e -> module.showImportDialog(projectName));
            contextMenu.getItems().add(item);
        }
        contextMenu.show(btnImportDataToProject, Side.BOTTOM, 0, 0);
    }
}
