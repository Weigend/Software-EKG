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
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.Optional;

/**
 * The navigation handler for the key events
 */
class KeyNavigationHandler<T> implements EventHandler<KeyEvent> {

    private final TreeView<T> treeView;

    /**
     * The selection index, initialize with root
     */
    private int selectionIndex = 0;

    /**
     * The key code
     */
    private KeyCode keyCode;

    /**
     * Initializes the key handler for the given {@link TreeView}.
     *
     * @param treeView The tree view instance events should be handled.
     */
    public KeyNavigationHandler(TreeView<T> treeView) {
        this.treeView = treeView;
    }

    /**
     * Handles the key event
     *
     * @param event - the key event
     */
    @Override
    public void handle(KeyEvent event) {
        //Expand the tree
        if (arrowKeyExpandMove(event.getCode())) {
            keyCode = KeyCode.RIGHT;
            markCurrentSelectedItem();
        }
        
        //Collapse the item
        if (arrowKeyCollapseMove(event.getCode())) {
            keyCode = KeyCode.LEFT;
            markCurrentSelectedItem();
            selectCurrentItem();
        }

        // Enter shows the selected item, and so does moving to it with the arrow keys: going
        // down a list of metrics shows one after the other.
        if (enterKey(event.getCode()) || navigationKey(event.getCode())) {
            getSelectedItem().ifPresent(i -> i.getEnterPressedHandler().handle(event));
            keepTheKeyboard();
        }
        if (refreshKey(event)) {
            getSelectedItem().ifPresent(i -> i.getService().restart());
        }
    }

    /**
     * Gives the focus back to the tree. What an item shows opens in another pane, and that
     * pane takes the focus: the next arrow key went to it instead of the tree, so that the
     * keys worked for one step and then no more.
     */
    private void keepTheKeyboard() {
        treeView.requestFocus();
        Platform.runLater(treeView::requestFocus);
    }

    private Optional<AbstractItem<?>> getSelectedItem() {
        Object item = treeView.getSelectionModel().getSelectedItem();
        if (item instanceof AbstractItem) {
            return Optional.of((AbstractItem<?>) item);
        }
        return Optional.empty();
    }

    /**
     * Validate if the key code is right up or down.
     *
     * @param eventKeyCode - the key code
     * @return true if event should trigger a tree move (right,up and down), otherwise false
     */

    private boolean arrowKeyExpandMove(KeyCode eventKeyCode) {
        return eventKeyCode == KeyCode.RIGHT || eventKeyCode == KeyCode.UP || eventKeyCode == KeyCode.DOWN;
    }

    /**
     * Validate if the key code is left.
     *
     * @param eventKeyCode - the key code
     * @return true if event should trigger a tree collapse (left), otherwise false
     */
    private boolean arrowKeyCollapseMove(KeyCode eventKeyCode) {
        return eventKeyCode == KeyCode.LEFT;
    }

    /**
     * Validates if the eventKeyCode is the enter key
     *
     * @param eventKeyCode - the event key code
     * @return true if enter key, otherwise false
     */
    private boolean enterKey(KeyCode eventKeyCode) {
        return eventKeyCode == KeyCode.ENTER;
    }

    /**
     * Validates if the eventKeyCode is the navigation key
     *
     * @param eventKeyCode - the event key code
     * @return true if navigation key, otherwise false
     */
    private boolean navigationKey(KeyCode eventKeyCode) {
        return eventKeyCode == KeyCode.DOWN || eventKeyCode == KeyCode.UP;
    }


    private boolean refreshKey(KeyEvent event) {
        return event.getCode() == KeyCode.R && event.isControlDown() || event.getCode() == KeyCode.F5;
    }

    /**
     * Select the item at the stored selection index
     */
    private void selectCurrentItem() {
        treeView.getSelectionModel().select(selectionIndex);

    }

    /**
     * Sets the current selected item
     */
    private void markCurrentSelectedItem() {
        selectionIndex = treeView.getSelectionModel().getSelectedIndex();
    }

    /**
     * Restores the selected item if the key code is not KeyCode.LEFT
     */
    void restoreSelectedItemIfNecessary() {
        if (keyCode != KeyCode.LEFT) {
            treeView.getSelectionModel().select(selectionIndex);
        }
    }
}
