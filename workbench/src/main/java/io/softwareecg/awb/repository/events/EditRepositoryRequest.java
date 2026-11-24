package io.softwareecg.awb.repository.events;

import io.softwareecg.awb.repository.api.EkgRepository;

import java.util.EventObject;

/**
 * Asks for the dialog in which the configuration of a repository is edited. Nothing has changed
 * yet; when it has, {@link RepositoryConfigChangedEvent} says so.
 *
 * @since 09.07.2018
 */
public class EditRepositoryRequest extends EventObject {

    private final boolean modificationRequested;

    /**
     * Owner-Window-Referenz. In der API als Object typisiert, damit das api-Modul
     * frei von JavaFX bleibt; UI-Consumer casten zu javafx.stage.Window.
     */
    private final Object ownerWindow;

    /**
     * Constructs a new EditRepositoryRequest with
     * modificationRequested = true as fixed setting.
     *
     * @param repository the types a modification is requested for
     * @param ownerWindow the owner window that will be the parent of an modification dialog/modal
     */
    public EditRepositoryRequest(EkgRepository repository, Object ownerWindow) {
        this(repository, true, ownerWindow);
    }

    public EditRepositoryRequest(EkgRepository repository, boolean modificationRequested, Object ownerWindow) {
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
