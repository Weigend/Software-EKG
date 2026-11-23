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
package io.softwareecg.awb.runtime.events;

import java.util.EventObject;

/**
 * Event to trigger an update of the explorer (TreeView on the left),
 * e.g. after the import is finished.
 * <p>
 * Listened by ExplorerController.
 * <p>
 *
 * @since 21.08.13
 */
public class ExplorerUpdateEvent extends EventObject {

    private Object treeItemValue;


    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred. This can be the itemin the explorer
     * @throws IllegalArgumentException if source is null.
     */
    public ExplorerUpdateEvent(Object source) {
        super(source);
    }

    /**
     * Constructs a new Event instance with with both key parameters
     * the Node that will be referenced by the Event and the information
     * object that the node represents.
     *
     * @param source the concrete referred tree node
     * @param treeItemValue the underlying information object
     */
    public ExplorerUpdateEvent(Object source, Object treeItemValue) {
        super(source);

        this.treeItemValue = treeItemValue;
    }

    /**
     * Return the value that is assigned to the referred tree node.
     * This will be one of the EKG data model objects like
     * Project, Metric, the Host or something similar.
     *
     * @return the
     */
    public Object getTreeItemValue() {
        return treeItemValue;
    }
}
