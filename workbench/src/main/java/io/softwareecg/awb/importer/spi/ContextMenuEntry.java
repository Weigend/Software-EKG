package io.softwareecg.awb.importer.spi;

import io.softwareecg.awb.projectcontext.model.ProjectTimeSeriesType;
import javafx.scene.Node;

/**
 * This interface represents an entry in the AWB explorer context menu that
 * provides context aware actions on nodes like repositories or metrics.
 * The entry has to provides an label and supported series type as minimum.
 *
 * @since 14.07.2018
 */
public interface ContextMenuEntry {

    /**
     * Returns the supported project data type.
     *
     * @return the project data type
     */
    default ProjectTimeSeriesType getProjectDataType() {
        return ProjectTimeSeriesType.DEFAULT;
    }

    /**
     * Returns the text/label of the context menu entry that
     * will shown to the user
     *
     * @return the menu entry label in the standard EKG AWB language
     */
    String getItemText();

    /**
     * Returns the optional that will shown in front of the menu
     * entry provides as JavaFX node instance
     *
     * @return the icon that will shown in front of the menu entry (can be null)
     */
    default Node getItemIcon() {
        return null;
    }
}
