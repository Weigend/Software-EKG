//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.explorer.items;

import io.softwareecg.awb.explorer.icons.ExplorerTreeIconsProvider;
import io.softwareecg.awb.explorer.icons.TreeIconsProviderRegistry;
import io.softwareecg.awb.projectcontext.api.ProjectDataAccessService;
import io.softwareecg.awb.projectcontext.spi.ProjectDataCollection;
import io.softwareecg.awb.projectcontext.events.ProjectDataCollectionChangedEvent;
import io.softwareecg.awb.projectcontext.events.ShowProjectDataSourcesEvent;
import io.softwareecg.awb.projectcontext.api.ProjectViewFlavorChangedEvent;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventBusListener;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

import static io.softwareecg.awb.explorer.items.TaskUtils.runAsTask;

/**
 * The explorerView item that shows a specific project.
 *
 */
public class ProjectItem extends RepositoryBaseItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectItem.class);

    /**
     * The types representation of an workbench project
     */
    private final Project project;

    /**
     * The active view flavor with a default state that should be overwritten
     */
    private ProjectViewFlavor activeViewFlavor = ProjectViewFlavor.PHYSICAL_VIEW;


    protected TreeIconsProviderRegistry iconsProviderRegistry = Lookup.lookup(TreeIconsProviderRegistry.class);

    private static final DateTimeFormatter LAST_VALUE =
            DateTimeFormatter.ofPattern("dd.MM. HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Tells whether data is being collected for a project; {@code null} if the application
     * has no data source module.
     */
    private final ProjectDataCollection dataCollection = findDataCollection();

    /**
     * The dot next to the project icon: green while values arrive, yellow while connecting,
     * red on an error, grey when paused; hidden for a project without data sources.
     */
    private final Circle collectionIndicator = new Circle(4);

    private Tooltip collectionTooltip;

    /**
     * Creates a new project item based on the given context and the project
     *
     * @param project     - the project
     * @param repository the containing types.
     */
    public ProjectItem(Project project, Repository repository) {
        super(project.getName(), repository);

        ExplorerTreeIconsProvider provider = (ExplorerTreeIconsProvider) iconsProviderRegistry
                .getTreeIconsProvider(project.getImporterId());

        this.project = project;

        // the icon, followed by a dot that tells whether data is being collected for the project
        collectionIndicator.setVisible(false);
        collectionIndicator.setManaged(false);
        HBox graphic = new HBox(5, provider.getProjectItemIcon(this), collectionIndicator);
        graphic.setAlignment(Pos.CENTER_LEFT);
        super.setGraphic(graphic);
        setId(repository.getId() + "_" + project.getName());

        if (dataCollection != null && repository.isEmbedded()) {
            addContextMenuEntry("Data Sources...", event -> Lookup.lookup(EventBus.class)
                    .publish(new ShowProjectDataSourcesEvent(this, project.getName())));
            watchDataCollection();
        }

        addContextMenuEntry("Delete project and its data",
                event -> runAsTask(
                        "delete-project-" + project.getName() + "-task",
                        () -> {
                            // delete time series data from Solr/search index first
                            deleteTimeSeriesData(repository, project.getName());

                            // then delete project metadata from H2 (cascades to aliases, bookmarks, etc.)
                            ServiceDiscovery.lookup(ProjectDataAccessService.class, repository).deleteProjectByName(project.getName());
                        },
                        finishedEvent -> Lookup.lookup(EventBus.class)
                                .publish(new ExplorerUpdateEvent(refreshTargetAfterDelete()))
                )
        );
    }

    /**
     * What to refresh once this project is deleted: normally the repository node above it.
     * If the tree was rebuilt while the deletion ran, this item is no longer part of it and
     * has no parent anymore - then the repository is addressed by its id, which the explorer
     * resolves to the node currently shown.
     */
    private Object refreshTargetAfterDelete() {
        TreeItem<String> parent = getParent();
        return parent != null ? parent : getRepository().getId();
    }

    //=================================================================================================================
    // showing that data is being collected for the project
    //=================================================================================================================

    /**
     * Shows the state of the project's data collection at the node and keeps it up to date
     * for as long as the node is part of the tree.
     */
    @SuppressWarnings("unchecked")
    private void watchDataCollection() {
        EventBus<java.util.EventObject> eventBus = Lookup.lookup(EventBus.class);
        EventBusListener<ProjectDataCollectionChangedEvent> listener = event -> {
            if (project.getName().equals(event.getProjectName())) {
                showDataCollection();
            }
            return true;
        };
        eventBus.subscribe(ProjectDataCollectionChangedEvent.class, listener);

        // a tree refresh builds new nodes; one that has left the tree stops listening.
        // Checked one pulse later, so that a node that is merely moved is left alone.
        parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent == null) {
                Platform.runLater(() -> {
                    if (getParent() == null) {
                        eventBus.unsubscribe(ProjectDataCollectionChangedEvent.class, listener);
                    }
                });
            }
        });
        showDataCollection();
    }

    private static ProjectDataCollection findDataCollection() {
        try {
            return Lookup.find(ProjectDataCollection.class).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void showDataCollection() {
        ProjectDataCollection.Status status = dataCollection.statusOf(project.getName());
        Platform.runLater(() -> {
            boolean collecting = status.state() != ProjectDataCollection.State.NONE;
            collectionIndicator.setVisible(collecting);
            collectionIndicator.setManaged(collecting);
            if (!collecting) {
                return;
            }
            collectionIndicator.setFill(switch (status.state()) {
                case RUNNING -> Color.web("#2da44e");
                case WAITING -> Color.web("#d4a72c");
                case ERROR -> Color.web("#cf222e");
                default -> Color.web("#8c959f");
            });
            String text = "Data collection: " + status.state().label()
                    + (status.sourceCount() > 1 ? " (" + status.sourceCount() + " sources)" : "")
                    + (status.lastValue() == null ? "" : "\nLast value: " + LAST_VALUE.format(status.lastValue()))
                    + (status.detail() == null ? "" : "\n" + status.detail());
            if (collectionTooltip == null) {
                collectionTooltip = new Tooltip(text);
                Tooltip.install(collectionIndicator, collectionTooltip);
            } else {
                collectionTooltip.setText(text);
            }
        });
    }

    @EventSubscriber(eventClass = ProjectViewFlavorChangedEvent.class)
    public void changeProjectViewFlavor(ProjectViewFlavorChangedEvent event) {
        if (!project.equals(event.getRelatedProject())) {
            return;
        }

        activeViewFlavor = event.getNewViewFlavor();
        getService().restart();
    }

    @Override
    protected int getOrderPriority() {
        return 10;
    }

    /**
     * Gets the project
     *
     * @return the project
     */
    public Project getProject() {
        return project;
    }

    public ProjectViewFlavor getActiveViewFlavor() {
        return activeViewFlavor;
    }

    public void setActiveViewFlavor(ProjectViewFlavor activeViewFlavor) {
        this.activeViewFlavor = activeViewFlavor;
    }

    /**
     * Deletes all time series documents for the given project from the repository.
     * This must be called before removing the project metadata from H2 to avoid orphaned data.
     */
    private void deleteTimeSeriesData(Repository repository, String projectName) {
        try {
            repository.deleteTimeSeriesOf(projectName);
        } catch (Exception e) {
            LOGGER.error("Failed to delete time series data for project '{}' from search index.", projectName, e);
        }
    }
}