package io.softwareecg.awb.repository.api.events;

import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.sdk.awbapi.repository.RepositoryDataEvent;

/**
 * An event class that will used by AWB components to request the creation
 * or update/modification of an EKG types.
 *
 * @since 09.07.2018
 */
public class RepositoryModifyEvent extends RepositoryDataEvent {

    private final boolean modificationRequested;

    /**
     * Owner-Window-Referenz. In der API als Object typisiert, damit das api-Modul
     * frei von JavaFX bleibt; UI-Consumer casten zu javafx.stage.Window.
     */
    private final Object ownerWindow;

    /**
     * Constructs a new RepositoryModifyEvent with
     * modificationRequested = true as fixed setting.
     *
     * @param repository the types a modification is requested for
     * @param ownerWindow the owner window that will be the parent of an modification dialog/modal
     */
    public RepositoryModifyEvent(EkgRepository repository, Object ownerWindow) {
        this(repository, true, ownerWindow);
    }

    public RepositoryModifyEvent(EkgRepository repository, boolean modificationRequested, Object ownerWindow) {
        super(repository);
        this.modificationRequested = modificationRequested;
        this.ownerWindow = ownerWindow;
    }

    public Object getOwnerWindow() {
        return ownerWindow;
    }

    public boolean isModificationRequested() {
        return modificationRequested;
    }

    public EkgRepository getRepository() {
        return (EkgRepository) getSource();
    }
}
