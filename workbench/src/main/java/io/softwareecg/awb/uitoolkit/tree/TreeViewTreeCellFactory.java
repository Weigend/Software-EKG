//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.uitoolkit.tree;

import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * A factory to create {@link TreeCell TreeCells} from {@link AbstractItem AbstractItems}.
 *
 */
class TreeViewTreeCellFactory<V> implements Callback<TreeView<V>, TreeCell<V>> {

    private static final Logger LOGGER = EkgLogger.get();

    /**
     * Indicates a single mouse-click.
     */
    private static final int SINGLE_CLICK = 1;

    /**
     * Used for indicating a double-click.
     */
    private static final int DOUBLE_CLICK = 2;

    private final KeyNavigationHandler<V> keyNavigationHandler;

    /**
     * Init the factory.
     *
     * @param keyNavigationHandler The navigation handler for key strokes.
     */
    public TreeViewTreeCellFactory(KeyNavigationHandler<V> keyNavigationHandler) {
        this.keyNavigationHandler = keyNavigationHandler;
    }

    /**
     * The actual {@link TreeCell} factory.
     *
     * @param treeView The target view for the created cells.
     * @return The new {@link TreeCell}.
     */
    @Override
    public TreeCell<V> call(final TreeView<V> treeView) {
        treeView.setOnKeyReleased(keyNavigationHandler); //Get selected item on tree at key pressed

        final TreeCell<V> treeCell = makeTreeCell();
        treeCell.setOnMouseClicked(e -> onTreeCellClickedHandler(e, treeCell));

        // Cells are reused for other rows whenever the tree changes. The menu is therefore
        // picked at the moment it is requested, as a filter that runs before the cell
        // shows it. Assigning it on the mouse click instead would come too late: the request
        // precedes the click, so the first right-click on a reused cell would open the menu
        // of the item that was shown in this row before - and act on that item.
        treeCell.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                e -> treeCell.setContextMenu(contextMenuFor(treeView, treeCell)));

        return treeCell;
    }

    /**
     * Resolves the context menu of the item the given cell currently displays. The tree view,
     * not the cell, is asked for that item, so a stale cell can never hand out the menu of
     * an item that has already left the tree.
     *
     * @return the item's context menu, or {@code null} for an empty cell
     */
    static <T> ContextMenu contextMenuFor(TreeView<T> treeView, TreeCell<T> treeCell) {
        if (treeCell.isEmpty() || treeCell.getIndex() < 0) {
            return null;
        }
        TreeItem<T> item = treeView.getTreeItem(treeCell.getIndex());
        return item instanceof AbstractItem<?> abstractItem ? abstractItem.getContextMenu() : null;
    }

    private TextFieldTreeCell<V> makeTreeCell() {
        return new TextFieldTreeCell<>() {
            @Override
            public void updateItem(V item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(Objects.toString(getTreeItem().getValue(), "NO TEXT"));
                    setGraphic(getTreeItem().getGraphic());
                }
            }
        };
    }

    /**
     * MouseEvent-Handler reacting to clicks on the TreeCells.
     *
     * @param mouseEvent The caught mouse event.
     * @param treeCell   the TreeCell which was clicked.
     */
    @SuppressWarnings("unchecked")
    protected void onTreeCellClickedHandler(final MouseEvent mouseEvent, final TreeCell treeCell) {
        if (!(treeCell.getTreeItem() instanceof AbstractItem)) {
            return;
        }

        AbstractItem<V> item = (AbstractItem<V>) treeCell.getTreeItem();

        if (mouseEvent.getClickCount() == DOUBLE_CLICK) {
            item.getDoubleClickHandler().handle(mouseEvent);

        } else if (mouseEvent.getClickCount() == SINGLE_CLICK) {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                item.getSingleClickHandler().handle(mouseEvent);
            }
        }
    }
}
