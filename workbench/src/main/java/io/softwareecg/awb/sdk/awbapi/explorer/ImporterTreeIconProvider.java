package io.softwareecg.awb.sdk.awbapi.explorer;

import io.softwareecg.awb.sdk.awbapi.project.Project;
import io.softwareecg.awb.sdk.awbapi.project.ProjectViewFlavor;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

/**
 * An interface the represents importer specific icon factories for the AWB explorer.
 * It can and used by optionally by importers to serve domain specific icons for the different tree node (items) types.
 *
 * Caution:
 * The implementation of this interface have not to use caching for the returned nodes. Each call has to provide
 * new instance of the requested icon node or null!
 *
 * @since 04.02.2020
 */
@SuppressWarnings("unused") // the default implementation is designed not to use the parameter, because it return null
public interface ImporterTreeIconProvider {

    /**
     * Returns the tree icon for a tree item that represents a single metric (KPI).
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getMetricItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a group of metrics of the same domain.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getMetricGroupItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a wrapper element
     * that contains multiple metric groups.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getCounterItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a measurement.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getMeasurementItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a group of measurements.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getMeasurementGroupItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a group of host (=cluster).
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getHostGroupItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a single host / physical node.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getHostItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a group of processes.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getProcessGroupItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a single process.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getProcessItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a (Docker) container.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getContainerItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a wrapper element that
     * contains multiple groups of Cloud Platform namespaces (e.g. OpenShift projects).
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getNamespaceGroupItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a single CloudPlatform namespace.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getNamespaceItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a Pod that runs in Kupernetes/OpenShift.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getPodItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a group of services as type
     * of load balancer for multiple pods in a CloudPlatform.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getServiceGroupItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }

    /**
     * Returns the tree icon for a tree item that represents a single services as type
     * of load balancer for multiple pods in a CloudPlatform.
     * The returned icon is (svg or bitmap based) wrapped in a Node object.
     *
     * The method can return <code>null</code> that means no specific icon for this item type is available.
     *
     * @param project the project context of the tree branch the icon is requested for
     * @param activeFlavor the active view flavor the is currently selected (PHYSICAL_VIEW will be the default)
     * @param item the specific node of the explorer tree the icon is requested for
     *
     * @return the icon wrapped into a (prototype) Node instance
     */
    default Node getServiceItemIcon(TreeItem<String> item, Project project, ProjectViewFlavor activeFlavor) {
        return null;
    }
}
