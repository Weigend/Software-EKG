package io.softwareecg.awb.explorer;

import io.softwareecg.awb.uitoolkit.tree.ExplorerController;
import io.softwareecg.awb.uitoolkit.tree.RootItem;
import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.explorer.items.ProjectItem;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.projectcontext.api.ProjectViewFlavorChangedEvent;
import io.softwareecg.awb.projectcontext.events.ProjectSelectedEvent;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import io.softwareecg.wfx.platform.core.eventbus.AnnotationProcessor;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import jakarta.inject.Singleton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeView;
import org.slf4j.Logger;
import java.util.EventObject;

/**
 * A controller extension that will add some additional functionality to the common tree explorer controller.
 * In contrast to the plain controller of the common explorer tree this implementation will know and implement
 * all the EKG specific domain logic like domain specific TreeItems, extended event handling and custom context menus.
 *
 * @since 06.07.2018
 */
@Singleton
public class ExplorerExtendedController {

    private static final Logger LOGGER = EkgLogger.get();

    private ExplorerController commonTreeController;

    private TreeView explorerTree;

    private EventBus<EventObject> eventBus;

    /**
     *
     * @param commonController an ExplorerController instance that controls the ExplorerTree view
     */
    public void initWithCommonController(ExplorerController commonController) {
        this.commonTreeController = commonController;
        this.explorerTree = commonTreeController.getExplorerTree();
        this.eventBus = Lookup.lookup(EventBus.class);

        initHandlerFunctions();
        AnnotationProcessor.process(this);
    }

    /**
     * Init the extended functionality
     */
    public void initHandlerFunctions() {
        //noinspection unchecked
        explorerTree.getSelectionModel().selectedItemProperty().addListener(new ProjectSelectionListener());
    }

    @EventSubscriber(eventClass = ExplorerUpdateEvent.class)
    public void handleExplorerUpdateEvent(ExplorerUpdateEvent event) {

    }


    @SuppressWarnings("unchecked")
    @EventSubscriber(eventClass = ProjectViewFlavorChangedEvent.class)
    public void changeProject(ProjectViewFlavorChangedEvent event) {

        AbstractItem treeItem = commonTreeController.getRootItem();
        Project relatedProject = event.getRelatedProject();
        ProjectItem relatedProjectItem = findProjectItemByProject(relatedProject, treeItem);

        // check if project exits in the tree - this should always be the case
        if (relatedProjectItem == null) {
            LOGGER.error("unable to find project item in explorer tree for project " + relatedProject.getName());
            return;
        }

        // we only continue if the view flavor has really changed
        if (relatedProjectItem.getActiveViewFlavor() == event.getNewViewFlavor()) {
            return;
        }

        int selectionIndexForProject = commonTreeController.getExplorerTree().getRow(relatedProjectItem);
        commonTreeController.getExplorerTree().getSelectionModel().select(selectionIndexForProject);
        relatedProjectItem.changeProjectViewFlavor(event);

        commonTreeController.getExplorerTree().getSelectionModel().getSelectedItem();
    }

    /**
     * An ChangeListener implementation that will bind to tree selection and will
     * resolve the selected project that belong to a node (TreeItem) clicked by the user
     */
    public class ProjectSelectionListener implements ChangeListener {

        @Override
        public void changed(ObservableValue observable, Object previousSelection, Object selectedItem) {

            // we will only get RootItems above the project tree items - in this case there is no project change
            if (selectedItem instanceof RootItem) {
                return;
            }

            RepositoryBaseItem previousItem = null;
            if (!(previousSelection instanceof RootItem)) {
                previousItem = (RepositoryBaseItem) previousSelection;
            }

            ProjectItem previousProjectItem = resolveProjectItem(previousItem);
            Project oldProject = previousProjectItem != null ? previousProjectItem.getProject() : null;

            ProjectItem selectedProjectItem = resolveProjectItem((RepositoryBaseItem) selectedItem);
            Project newProject = selectedProjectItem != null ? selectedProjectItem.getProject() : null;

            if (newProject == null || newProject.equals(oldProject)) {
                return;
            }

            LOGGER.info("new project selected " + newProject.getId());
            eventBus.publish(new ProjectSelectedEvent(selectedItem, oldProject, newProject,
                    selectedProjectItem.getActiveViewFlavor(), ((RepositoryBaseItem) selectedItem).getRepository()));
        }

        /**
         * Traverse the tree path to the root to resolve the ProjectItem
         * node that contains the selected tree-node (or is itself the selected one).
         *
         * @param selectedItem the selected TreeItem of the explorer view
         * @return null if a selected item is null or a types/root node or the resolved ProjectItem
         */
        private ProjectItem resolveProjectItem(RepositoryBaseItem selectedItem) {
            AbstractItem parent = selectedItem;

            while (parent != null && !(parent instanceof ProjectItem)) {
                parent = (AbstractItem) parent.getParent();
            }

            return (ProjectItem) parent;
        }
    }

    private ProjectItem findProjectItemByProject(Project lookupProject, AbstractItem treeItem) {

        if (lookupProject == null) {
            LOGGER.error("no project was given, abort and return null as ProjectItem");
            return null;
        }

        if (treeItem instanceof ProjectItem && lookupProject.equals(((ProjectItem)treeItem).getProject())) {
            return (ProjectItem) treeItem;
        }

        for(Object child : treeItem.getChildren()) {
            if (child instanceof ProjectItem && lookupProject.equals(((ProjectItem)child).getProject())) {
                return (ProjectItem)child;
            }

            ProjectItem projectItem = findProjectItemByProject(lookupProject, (AbstractItem) child);
            if (projectItem != null) {
                return projectItem;
            }
        }

        return null;
    }
}
