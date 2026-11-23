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
import jakarta.inject.Singleton;

/**
 * Base root item.
 *
 */
@Singleton
public class RootItem extends AbstractItem<String> {

    /**
     * Init the root item.
     */
    public RootItem() {
        super("Workbench");
    }
}
