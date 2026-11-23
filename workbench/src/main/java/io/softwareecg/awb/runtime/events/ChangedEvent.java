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
package io.softwareecg.awb.runtime.events;

import java.util.EventObject;

/**
 * An event that informs the subscriber that a specific object of type <code>T</code> has been added, updated
 * or deleted.
 *
 * @param <T> the type of this event.
 */
public abstract class ChangedEvent<T> extends EventObject {
    private final T obj;
    private final Change change;

    /**
     * Create a new changed event
     *
     * @param obj    The changed object.
     * @param change the type of the change
     * @param source the sender object.
     */
    public ChangedEvent(T obj, Change change, Object source) {
        super(source);
        this.obj = obj;
        this.change = change;
    }

    public T getObj() {
        return obj;
    }

    public Change getChange() {
        return change;
    }

    /**
     * The type of the change.
     */
    public enum Change {
        ADD, UPDATE, DELETE
    }
}
