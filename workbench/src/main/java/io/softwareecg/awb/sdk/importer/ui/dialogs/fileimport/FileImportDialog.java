package io.softwareecg.awb.sdk.importer.ui.dialogs.fileimport;

import io.softwareecg.awb.sdk.awbapi.project.ProjectSelector;
import io.softwareecg.awb.sdk.core.resourceloader.CdiFxmlLoader;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.tasks.SplitSourceProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.ui.dialogs.ImportDialog;

import java.util.List;


/**
 * Generic FileImportDialog ui component that can be used directly by
 * file based importers to provide user-interface for selection of types, project and files to import.
 *
 * Importer that have a more special demand can derive from this calls to extend the functionality with
 * specific stuff.
 *
 * @since 14.07.2018
 */
public class FileImportDialog extends ImportDialog {

    private static final String DEFAULT_VIEW = "FileImportDialogView.fxml";

    protected FileImportDialogController controller;

    /**
     * Constructs a new instance of FileImportDialog
     * and triggers the load of the view.
     */
    public FileImportDialog() {
        loadView();
    }

    /**
     * Resolves the URL of the fxml view to load for this ImportDialog instance
     * and trigger the load itself.
     *
     * This method should be overwritten by deriving classes which need to load it's own views.
     */
    protected void loadView() {
        if (controller == null) {
            controller = CdiFxmlLoader.loadView(DEFAULT_VIEW, this, new FileImportDialogController()).getController();
        } else {
            controller = CdiFxmlLoader.loadView(DEFAULT_VIEW, this, controller).getController();
        }
    }

    /**
     * Return a list of selected file paths. If no files
     * chosen an empty list will returned.
     *
     * @return the list property with file names
     */
    public List<String> getSelectedFilesPaths() {
        return controller.getSelectedFilesPaths();
    }

    @Override
    public void updateState() {
        controller.getProjectSelector().updateState();
        controller.getRepositorySelector().updateState();
    }

    @Override
    public ProjectSelector getProjectSelector() {
        return controller.getProjectSelector();
    }

    @Override
    public void setLocalImportStrategy(LocalProjectImportStrategy localProjectImportStrategy) {
        controller.setLocalImportStrategy(localProjectImportStrategy);
    }

    @Override
    public void setRemoteImportStrategy(SplitSourceProjectImportStrategy splitSourceProjectImportStrategy) {
        controller.setRemoteImportStrategy(splitSourceProjectImportStrategy);
    }

    protected void setController(FileImportDialogController importDialogController) {
        controller = importDialogController;
    }
}
