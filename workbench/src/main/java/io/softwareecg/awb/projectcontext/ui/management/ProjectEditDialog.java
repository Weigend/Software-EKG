package io.softwareecg.awb.projectcontext.ui.management;

import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.model.TimeSeriesImportSource;
import io.softwareecg.awb.runtime.resources.CachingSvgLoader;
import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import io.softwareecg.awb.runtime.resources.ViewLoadResult;
import javafx.scene.Group;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The ProjectEditDialog UI component is a JavaFX Dialog that will provide the all
 * editable project properties including the optional ImportSourceRepository data to
 * the AWB user.
 * The dialog can used to change project settings after it's initial creation.
 * Especially change the access data for the {@link TimeSeriesImportSource}
 * is the most imported use case of this dialog.
 *
 * @since 24.11.2018
 */
public class ProjectEditDialog extends Dialog<Project> {

    /**
     * Creates a new CreateProjectDialog instance
     *
     * @param repository the EKG repository that stores the project that should be edit in the current dialog scope
     * @param project the project that should be edit in the current dialog scope
     */
    public ProjectEditDialog(Repository repository, Project project) {

        setTitle("EKG Projekt verwalten");

        ViewLoadResult viewResult = CdiFxmlLoader.loadView("ProjectEditDialogView.fxml");
        setDialogPane(viewResult.getComponent());

        ProjectEditDialogController controller = viewResult.getController();
        controller.setParent(this);
        controller.setRepository(repository);
        controller.setProject(project);

        // bind controller to Dialog closed with apply
        setResultConverter(controller::persistProject);

        setHeaderText("This dialog allows you to modify the project properties and, when using" +
                " remote time series (split-source projects), the connection details of the source system.");

        Group iconImage = new CachingSvgLoader(false)
                .setDefaultScale(0.14)
                .getSvgImage("icons/ekg-repository-icon2.svg");

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResource("/ekg-app-icon2.png").toExternalForm()));


        setGraphic(iconImage);
    }
}
