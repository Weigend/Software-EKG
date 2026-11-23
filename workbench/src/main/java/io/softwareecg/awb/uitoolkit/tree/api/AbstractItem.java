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
package io.softwareecg.awb.uitoolkit.tree.api;

import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The abstract basic item for explorer view items.
 *
 * @param <T> The type of the {@link #getValue() value} property within TreeItem.
 *
 */
public abstract class AbstractItem<T> extends TreeItem<T> implements Comparable<AbstractItem<T>> {

    // an ObservableList list of TreeItem elements that will populate the tree view.
    // This member is redundant to the "children" member of super class that couldn't accessed by clients via API
    final private ObservableList<TreeItem<T>> sourceList = FXCollections.observableArrayList();

    private final ObjectProperty<TreeItemPredicate<T>> predicate = new SimpleObjectProperty<>();

    private ContextMenu contextMenu;

    private Service<ObservableList<AbstractItem>> service;

    private String id = super.toString();

    private boolean alreadyLoad = false;

    /**
     * Stores expanded state passed down from a parent node during tree refresh.
     * Used to recursively restore the expanded state of child nodes after service restart.
     * Structure: key = child value string, value = nested map for that child's children.
     */
    private Map<String, Object> pendingExpandedState;

    /**
     * Creates a AbstractItem with the value property set to the provided object and it initializes the context menu for
     * the current item.
     *
     * @param value The object to be stored as the value of this AbstractItem.
     */
    public AbstractItem(T value) {
        super(value);

//        sourceList

        /*
        todo SKR tree filter to support a search of the whole tree

        sourceList.predicateProperty().bind(Bindings.createObjectBinding(() -> child -> {

            // Set the predicate of child items to force filtering
            if (child instanceof AbstractItem) {
                AbstractItem<T> filterableChild = (AbstractItem<T>) child;
                filterableChild.setPredicate(predicate.get());
            }

            // If there is no predicate, keep this tree item
            if (predicate.get() == null) {
                return true;
            }

            // If there are children, keep this tree item
            if (child.getChildren().size() > 0) {
                return true;
            }

            // Otherwise ask the TreeItemPredicate
            return predicate.get().test(this, child.getValue());

        }, predicate));

        */

        //setHiddenFieldChildren(sourceList);

        //Ensure we are the ui thread.
        Platform.runLater(() -> contextMenu = new ContextMenu());
    }

    /**
     * Returns the list of children that is backing the filtered list.
     * This accessor is redundant to the getChildren of the super class. In contrast
     * to that one this is one doesn't perform additional (expensive logic)
     *
     * @return underlying list of children
     */
    public ObservableList<TreeItem<T>> getInternalChildren() {
        // Use the public API instead of reflection to avoid Java module system issues
        return getChildren();
    }

    /**
     * Returns a filtered set of {@link AbstractItem} child's of this node that have the same
     * item id like the given one. Normally only one item should returned.
     *
     * @param itemId the item id to look for
     * @return a set of AbstractItem instances
     */
    public Set<AbstractItem<T>> getFilteredChildren(String itemId) {

        Set<AbstractItem<T>> resultSet = new HashSet<>();

        Collection<TreeItem<T>> childs = this.getInternalChildren();

        if (childs == null) {
            return resultSet;
        }

        for (TreeItem<T> child : this.getInternalChildren()) {
            AbstractItem<T> abstractChildItem = (AbstractItem<T>) child;
            if (itemId.equals(abstractChildItem.getId())) {
                resultSet.add(abstractChildItem);
            }

            resultSet.addAll(abstractChildItem.getFilteredChildren(itemId));
        }

        return resultSet;
    }

    /**
     * @return the predicate
     */
    public final TreeItemPredicate<T> getPredicate() {
        return this.predicate.get();
    }

    public ObjectProperty<TreeItemPredicate<T>> predicateProperty() {
        return predicate;
    }

    /**
     * Set the predicate
     *
     * @param predicate the predicate
     */
    public final void setPredicate(TreeItemPredicate<T> predicate) {
        this.predicate.set(predicate);
    }

    /**
     * The children of this TreeItem. This method is called frequently, and
     * it is therefore recommended that the returned list be cached by
     * any TreeItem implementations.
     *
     * @return a list that contains the child TreeItems belonging to the TreeItem.
     */
    @Override
    public ObservableList<TreeItem<T>> getChildren() {
        if (!alreadyLoad) {
            alreadyLoad = true;
            getService().restart();
        }
        return super.getChildren();
    }

    /**
     * Returns the unique id of the tree item (node)
     *
     * @return the unique id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique id of the tree item
     *
     * @param id the unique id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Flag to check if the current item is a leaf
     *
     * @return false
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Get the enter pressed handler.
     *
     * @return the enter pressed handler.
     */
    public EventHandler<KeyEvent> getEnterPressedHandler() {
        return e -> {
            // Do nothing
        };
    }

    /**
     * Get the delete pressed handler.
     *
     * @return the delete pressed handler.
     */
    public EventHandler<KeyEvent> getDeletePressedHandler() {
        return e -> {
            // Do nothing
        };
    }

    /**
     * Get the single click handler.
     *
     * @return the single click handler.
     */
    public EventHandler<MouseEvent> getSingleClickHandler() {
        return e -> {
            // Do nothing
        };
    }

    /**
     * Get the double click handler.
     *
     * @return the double click handler.
     */
    public EventHandler<MouseEvent> getDoubleClickHandler() {
        return e -> {
            // Do nothing
        };
    }

    /**
     * Adds a context menu entry
     *
     * @param command - the name of the command
     * @param action  - the action for the command
     */
    public void addContextMenuEntry(final String command, final EventHandler<ActionEvent> action) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> addContextMenuEntry(command, action));
            return;
        }
        MenuItem item = new MenuItem(command);
        item.setOnAction(action);
        contextMenu.getItems().add(item);
    }

    /**
     * Gets the context menu of the item
     *
     * @return the context menu
     */
    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Get the service to resolve the children.
     *
     * @return the service to resolve the children.
     */
    @SuppressWarnings("unchecked")
    public Service getService() {
        if (service == null) {
            service = createService();
            service.progressProperty().addListener((o, ov, nv) -> Lookup.lookup(EventBus.class)
                    .publish(new ProgressEvent(service.getMessage(), service.getProgress(), this)));
            service.setOnSucceeded(e -> {
                Collection<? extends TreeItem<T>> newChildren = (Collection<? extends TreeItem<T>>) e.getSource().getValue();

                // Save expanded state from current (old) children before replacing them
                Map<String, Object> expandedState = collectExpandedState(getChildren());

                // Merge with any pending state inherited from a parent's refresh
                if (pendingExpandedState != null) {
                    expandedState.putAll(pendingExpandedState);
                    pendingExpandedState = null;
                }

                getChildren().setAll(newChildren);

                // Restore expanded state on the new child instances
                applyExpandedState(getChildren(), expandedState);
            });
            service.setOnFailed(e -> {
                // Reset the alreadyLoad flag so the user can retry by collapsing and expanding the node
                alreadyLoad = false;
            });
        }
        return service;
    }

    /**
     * Get the priority how to order different types of items.
     * <p>
     * The default value for the order priority is 100.
     *
     * @return the defined order priority.
     */
    protected int getOrderPriority() {
        return 100;
    }

    /**
     * Sets the given TreeItem list to the private field "children" of the super class
     * and bind the (already in super class instantiated) change listener for the tree item list
     * to the given one.
     *
     * This hack is necessary because of an bad inconsistent implementation of the TreeItem super class.
     * See https://bugs.openjdk.java.net/browse/JDK-8089158 for more details.
     *
     * @ p aram treeItemList an observable list of TreeItem elements
     */
//    protected void setHiddenFieldChildren(ObservableList<TreeItem<T>> treeItemList) {
//        try {
//            Field childrenField = TreeItem.class.getDeclaredField("children"); //$NON-NLS-1$
//            childrenField.setAccessible(true);
//            childrenField.set(this, treeItemList);
//
//            Field declaredField = TreeItem.class.getDeclaredField("childrenListener"); //$NON-NLS-1$
//            declaredField.setAccessible(true);
//            //noinspection unchecked
//            treeItemList.addListener((ListChangeListener<? super TreeItem<T>>) declaredField.get(this));
//        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
//            throw new IllegalStateException("Could not set TreeItem.children", e); //$NON-NLS-1$
//        }
//    }

    @Override
    public int compareTo(AbstractItem<T> o) {
        if (this.getOrderPriority() != o.getOrderPriority()) {
            return Integer.compare(this.getOrderPriority(), o.getOrderPriority());
        } else {
            return toCompareString(getValue()).compareToIgnoreCase(toCompareString(o.getValue()));
        }
    }

    private static String toCompareString(Object value) {
        return value != null ? value.toString() : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AbstractItem<?> that)) {
            return false;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getOrderPriority())
                .toHashCode();
    }

    /**
     * Recursively collects the expanded state of all children into a tree-structured map.
     * Keys are the string representation of each expanded child's value; values are nested maps
     * representing the expanded state of that child's own children.
     */
    private Map<String, Object> collectExpandedState(Collection<? extends TreeItem<T>> children) {
        Map<String, Object> state = new HashMap<>();
        for (TreeItem<T> child : children) {
            if (child.isExpanded()) {
                String key = String.valueOf(child.getValue());
                state.put(key, collectExpandedState(child.getChildren()));
            }
        }
        return state;
    }

    /**
     * Restores expanded state on new child items by matching their values against the saved state.
     * For each match, the nested state is passed down via {@link #pendingExpandedState} so that
     * when the child's service completes, its own children can be expanded in turn.
     */
    @SuppressWarnings("unchecked")
    private void applyExpandedState(Collection<? extends TreeItem<T>> children, Map<String, Object> state) {
        if (state == null || state.isEmpty()) {
            return;
        }
        for (TreeItem<T> child : children) {
            String key = String.valueOf(child.getValue());
            if (state.containsKey(key) && child instanceof AbstractItem<?> ai) {
                ai.pendingExpandedState = (Map<String, Object>) state.get(key);
                child.setExpanded(true);
            }
        }
    }

    private Service createService() {
        return Lookup.lookup(ItemBuilderRegistry.class).getServiceFor(this);
    }
}
