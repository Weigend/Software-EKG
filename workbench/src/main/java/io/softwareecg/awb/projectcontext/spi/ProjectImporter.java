package io.softwareecg.awb.projectcontext.spi;

/**
 * Imports data into a project. The project bar offers the importers of the application through
 * this interface; what an importer is made of is none of its business.
 */
public interface ProjectImporter {

    /**
     * @return the id a project keeps of the importer its data came from
     */
    String getImporterID();

    /**
     * @return the name of the importer as the user reads it
     */
    String getMenuText();

    /**
     * @return where the importer stands among the others; the lowest comes first
     */
    int getMenuPosition();

    /**
     * Shows the import dialog with a project selected.
     *
     * @param preselectedProjectName the name of the project to import into
     */
    void showImportDialog(String preselectedProjectName);
}
