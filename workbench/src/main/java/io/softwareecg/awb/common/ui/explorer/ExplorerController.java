//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.common.ui.explorer;


import io.softwareecg.awb.common.ui.explorer.api.AbstractItem;
import io.softwareecg.awb.sdk.awbapi.explorer.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller for the explorerView view.
 *
 */
@Singleton
public class ExplorerController implements Initializable {

    private static final Logger LOGGER = EkgLogger.get();

    /**
     * Defines the cell size for the metric tree.
     */
    private static final int DEFAULT_CELL_SIZE = 22;

    /**
     * The metric tree control
     */
    @FXML
    private TreeView explorerTree;


    private AbstractItem<?> rootItem;

    /**
     * Initialize the controller
     *
     * @param location  - the location url
     * @param resources - the resources
     */
    @Override
    @SuppressWarnings("unchecked")
    public void initialize(URL location, ResourceBundle resources) {
        explorerTree.setCellFactory(new TreeViewTreeCellFactory<>(new KeyNavigationHandler<>(this.explorerTree)));
        explorerTree.setShowRoot(true);
        explorerTree.setFixedCellSize(DEFAULT_CELL_SIZE);
        // ExplorerUpdateEvent subscription is wired via the @EventSubscriber annotation
        // on updateExplorer(...) by AnnotationProcessor (called from FXMLLoaderFactory).
    }

    /**
     * Set the root item.
     *
     * @param rootItem the new root item.
     */
    @SuppressWarnings("unchecked")
    public void setRootItem(AbstractItem<?> rootItem) {
        this.rootItem = rootItem;

        /*
        todo SKR create search field in explorerView view

Example:

        rootItem.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            if (filterField.getText() == null || filterField.getText().isEmpty())
                return null;
            return TreeItemPredicate.create(actor -> actor.toString().contains(filterField.getText()));
        }, filterField.textProperty()));
 */

        explorerTree.setRoot(this.rootItem);
    }

    public AbstractItem<?> getRootItem() {
        return rootItem;
    }

    public TreeView getExplorerTree() {
        return explorerTree;
    }

    /**
     * Subscribe this class to listen on the explorerView update event, to update the tree items
     *
     * @param updateEvent update event
     * @return true
     */
    @EventSubscriber(eventClass = ExplorerUpdateEvent.class)
    public boolean updateExplorer(ExplorerUpdateEvent updateEvent) {

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateExplorer(updateEvent));
            return true;
        }

        if (updateEvent.getSource() instanceof AbstractItem root) {
            root.getService().restart();
            return true;

       } else if (updateEvent.getSource() instanceof String) {

            String nodeId = ((String) updateEvent.getSource()).toLowerCase();

            AbstractItem root = (AbstractItem) explorerTree.getRoot();
            if (root == null) {
                return true;
            }

            var filteredChildren = root.getFilteredChildren(nodeId);

            for (Object childItem : filteredChildren) {
                ((AbstractItem<?>)childItem).getService().restart();
                ((AbstractItem<?>)childItem).setExpanded(true);
            }

            return true;
        }

        // something else was given an we will update the project node that was updated
        TreeItem project = explorerTree.getRoot();
        ((AbstractItem) project).getService().restart();
        project.setExpanded(true);

//        project.getChildren().forEach(treeItem -> {
//            AbstractItem repositoryItem = (AbstractItem) treeItem;
//            repositoryItem.getService().reset();
//            repositoryItem.setExpanded(true);
//        });

        return true;
    }
}
