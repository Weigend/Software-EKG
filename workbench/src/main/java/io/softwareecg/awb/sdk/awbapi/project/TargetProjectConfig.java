package io.softwareecg.awb.sdk.awbapi.project;

/**
 * This POJO provides the user defined project settings that are
 * required by the importers to fulfill the metadata and control/configure the import flow.
 *
 * The class is designed as base class that will be overwritten by the concrete implementation.
 * So some of the getters return static values in this base implementation. The deriving
 * implementations can serve other values based on the underlying business logic.
 *
 * @since 30.01.2020
 */
public class TargetProjectConfig {

    public String projectName;

    public boolean overwriteExistingData;

    public boolean deltaImport;

    /**
     * The import time range selected by the user in the dialog.
     * Values: "1d", "2d", "3d", "5d", "10d", "Alle"
     */
    public String importRange = "Alle";

    public boolean useSplitSource() {
        return false;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean doOverwriteExistingData() {
        return overwriteExistingData;
    }

    public void setOverwriteExistingData(boolean overwriteExistingData) {
        this.overwriteExistingData = overwriteExistingData;
    }

    public boolean isNewProject() {
        return false;
    }

    public boolean isDeltaImport() {
        return deltaImport;
    }

    public void setDeltaImport(boolean deltaImport) {
        this.deltaImport = deltaImport;
    }

    public String getImportRange() {
        return importRange;
    }

    public void setImportRange(String importRange) {
        this.importRange = importRange;
    }

}
