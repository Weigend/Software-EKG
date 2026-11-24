package io.softwareecg.awb.repository.api;

import java.util.EventObject;

/**
 * Asks for the dialog in which a repository is added.
 */
public class AddRepositoryRequest extends EventObject {

    private final Object ownerWindow;

    /**
     * @param source      who asks
     * @param ownerWindow the window the dialog belongs to, a {@code javafx.stage.Window}
     */
    public AddRepositoryRequest(Object source, Object ownerWindow) {
        super(source);
        this.ownerWindow = ownerWindow;
    }

    public Object getOwnerWindow() {
        return ownerWindow;
    }
}
