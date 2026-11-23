package io.softwareecg.awb.importer.ui.dialogs;

import io.softwareecg.awb.projectcontext.api.ProjectSelector;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import io.softwareecg.awb.importer.framework.SplitSourceProjectImportStrategy;
import io.softwareecg.awb.importer.ui.dialogs.fileimport.FileImportDialog;
import io.softwareecg.awb.importer.ui.dialogs.queryimport.QueryImportDialog;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;

/**
 * Generic ImportDialog ui component that will/should be the base class of all
 * EKG import dialog implementations. This class is abstract and has to derive.
 *
 * Importer which based on files or queries and don't have a special demand to
 * derive can use the API sub-classes {@link  FileImportDialog} and {@link  QueryImportDialog }
 *
 * @since 14.07.2018
 */
public abstract class ImportDialog extends BorderPane {

    /**
     * the header description text that will displayed at
     * the top area (header) in all default ImportDialog implementations.
     */
    private final SimpleStringProperty headerDescription = new SimpleStringProperty("Import Dialog");

    /**
     * the window title of the dialog window and can changed
     * or overwrite by Importers
     */
    private String windowsTitle = "Import Dialog";

    /**
     * The icon that will show in the title bar
     * for the DialogImporter modal window.
     */
    private Image windowIcon = null;

    /**
     * Sets the header description text that will displayed in
     * dialog header if default dialog implementation's are in use
     *
     * @param description the description text
     */
    public void setHeaderDescription(String description) {
        this.headerDescription.set(description);
    }

    /**
     * Returns the header description text that will displayed in
     * dialog header if default dialog implementation's are in use
     *
     * @return the description text
     */
    public StringProperty getHeaderDescriptionProperty() {
        return headerDescription;
    }

    /**
     * Returns the ProjectSelector component that is used in the ImportDialog
     *
     * @return a ProjectSelector instance
     */
    public abstract ProjectSelector getProjectSelector();

    /**
     * Sets the LocalProjectImportStrategy import strategy that will called by the ImportDialog
     * than the user submits a new import procedure for local project data
     *
     * @param localProjectImportStrategy the LocalProjectImportStrategy that will provide the according import task
     */
    public abstract void setLocalImportStrategy(LocalProjectImportStrategy localProjectImportStrategy);

    /**
     * Sets the LocalProjectImportStrategy import strategy that will called by the ImportDialog
     * than the user submits a new import procedure for local project data
     *
     * @param splitSourceProjectImportStrategy the SplitSourceProjectImportStrategy that will provide the according import task
     */
    public abstract void setRemoteImportStrategy(SplitSourceProjectImportStrategy splitSourceProjectImportStrategy);

    /**
     * Returns the window title of the dialog window and can changed
     * or overwrite by Importers
     *
     * @return the title of the ImportDialog modal window
     */
    public String getDialogTitle() {
        return windowsTitle;
    }

    /**
     * Set the window title of the dialog window and can changed
     * or overwrite by Importers
     *
     * @param windowsTitle the title of the ImportDialog modal window
     */
    public void setWindowsTitle(String windowsTitle) {
        this.windowsTitle = windowsTitle;
    }

    /**
     * Returns the icon that will show in the title bar
     * of the DialogImporter modal window.
     *
     * @return the icon as {@link Image} instance
     */
    public Image getDialogIcon() {
        return windowIcon;
    }

    /**
     * Set the icon that will show in the title bar
     * of the DialogImporter modal window.
     *
     * @param windowIcon the icon as {@link Image} instance
     */
    @SuppressWarnings("unused")
    public void setWindowIcon(Image windowIcon) {
        this.windowIcon = windowIcon;
    }


    public void updateState() {
        // no op
    }

    /**
     * Loads the dialog view that it specified by the given URL instance
     * and inject all dependencies into the controller that have to specified
     * in the .fxml file.
     *
     * @param fxmlView the url that points to the fxml view to load for this ImportDialog instance
     * @return the controller that belongs to the view
     */
    protected <T> T loadView(URL fxmlView) {
        try {
            FXMLLoader fxmlLoader = Lookup.lookup(FXMLLoader.class);
            fxmlLoader.setRoot(this);
            fxmlLoader.setLocation(fxmlView);
            fxmlLoader.load();
            return fxmlLoader.getController();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
