package io.softwareecg.awb.repository.api;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.Node;

/**
 * A control in which the user names the server the values of a project come from, and how to log
 * in there. Others put it into their dialogs: each {@code Lookup} or injection gives a new one.
 */
public interface QuerySourceSelector {

    /** @return the repository the project is in */
    Property<Repository> getRepositoryProperty();

    /**
     * Shows the server of a project.
     *
     * @param projectName  the name of the project, {@code null} if there is none yet: nothing can be entered
     * @param isNewProject whether the project is yet to be created and so has no server
     */
    void setProjectName(String projectName, boolean isNewProject);

    /** Offers only these ways to log in. */
    void setSupportedAuthTypes(ResourceAuthType... supportedAuthTypes);

    /** @return the address of the server as it stands in its field */
    ReadOnlyStringProperty repositoryUrlProperty();

    /** @return what was entered, {@code null} as long as there is no project */
    ReadOnlyProperty<TimeSeriesImportSource> getImportSourceRepositoryProperty();

    /** @param labelWidth the width of the column with the labels */
    void setLabelWidth(double labelWidth);

    /** @param rowHeight the height of a row */
    void setRowHeight(double rowHeight);

    /** Empties the fields, as when a dialog opens again. */
    void updateState();

    /** Stores what was entered for the project, if it differs from what is stored. */
    void submitChanges();

    /** @return the control, to put it into a dialog */
    Node asJfxComponent();
}
