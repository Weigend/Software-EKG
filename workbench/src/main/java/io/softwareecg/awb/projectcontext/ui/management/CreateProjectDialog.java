package io.softwareecg.awb.projectcontext.ui.management;

import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.runtime.resources.CachingSvgLoader;
import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import io.softwareecg.awb.runtime.resources.ViewLoadResult;
import javafx.scene.Group;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Dialog that adds a project of a remote repository to this workbench: typically the project
 * an external Collector writes its series under, straight into that repository.
 * <p>
 * Nothing is created in the repository. The project is a record in the metadata of the
 * workbench (H2) that names the repository it belongs to; the workbench shows the projects it
 * has such a record of, and finds their series in the repository by the name of the project.
 *
 * @since 24.06.2018
 */
public class CreateProjectDialog extends Dialog<Project> {

    private static final String DEFAULT_TITLE = "Add Project to Repository";

    /**
     * Creates a new CreateProjectDialog instance
     */
    public CreateProjectDialog() {

        setTitle(DEFAULT_TITLE);

        ViewLoadResult viewResult = CdiFxmlLoader.loadView("CreateProjectDialogView.fxml");
        setDialogPane(viewResult.getComponent());

        CreateProjectDialogController controller = viewResult.getController();
        controller.setParent(this);

        // bind controller to Dialog closed with apply
        setResultConverter(controller::persistProject);

        setHeaderText("Adds a project of a remote repository to this workbench, for example the\n"
                + "project a Collector writes into. Use the name the series are stored under.\n"
                + "Nothing is created in the repository itself.");

        Group iconImage = new CachingSvgLoader(false)
                .setDefaultScale(0.14)
                .getSvgImage("icons/ekg-repository-icon2.svg");

        setGraphic(iconImage);

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResource("/ekg-app-icon2.png").toExternalForm()));
    }
}
