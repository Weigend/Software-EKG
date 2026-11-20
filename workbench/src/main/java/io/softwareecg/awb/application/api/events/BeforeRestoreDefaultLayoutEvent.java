package io.softwareecg.awb.application.api.events;

import java.util.EventObject;

/**
 * Wird vor dem Wiederherstellen des Default-Layouts publiziert. Features koennen
 * darauf reagieren um eigene dynamische Komponenten aufzuraeumen.
 */
public class BeforeRestoreDefaultLayoutEvent extends EventObject {

    public BeforeRestoreDefaultLayoutEvent(Object source) {
        super(source);
    }
}
