package io.softwareecg.awb.sdk.awbapi.repository;

import java.util.EventObject;

/**
 * An event that will used to signal the repository data has changed
 * in any way.
 *
 * @since 01.06.2018
 */
public class RepositoryDataEvent extends EventObject {

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public RepositoryDataEvent(Object source) {
        super(source);
    }
}
