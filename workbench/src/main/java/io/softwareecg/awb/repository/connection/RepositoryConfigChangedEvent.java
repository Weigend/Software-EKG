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
package io.softwareecg.awb.repository.connection;


import io.softwareecg.awb.runtime.events.ChangedEvent;

/**
 * The configuration of a repository was added, changed or removed.
 */
public class RepositoryConfigChangedEvent extends ChangedEvent<EkgRepository> {

    /**
     * Create a new changed event
     *
     * @param obj    The changed object.
     * @param change the type of the change
     * @param source the sender object.
     */
    public RepositoryConfigChangedEvent(EkgRepository obj, Change change, Object source) {
        super(obj, change, source);
    }
}
