package io.softwareecg.awb.project.ui.mgrdialog;

import io.softwareecg.awb.project.api.model.Project;
import io.softwareecg.awb.sdk.core.resourceloader.CachingSvgLoader;
import io.softwareecg.awb.sdk.core.resourceloader.CdiFxmlLoader;
import io.softwareecg.awb.sdk.core.resourceloader.ViewLoadResult;
import javafx.scene.Group;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Dialog component that is used to create new empty projects in a remote repository that
 * can used by EKG-Collector to fill it with time series data directly from the collectors.
 *
 * @since 24.06.2018
 */
public class CreateProjectDialog extends Dialog<Project> {

    private static final String DEFAULT_TITLE = "Add EKG Collector Project";

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

        setHeaderText("To create a new project for the EKG Collector, please specify" +
                " all required project properties. Creating projects is only\npossible for remote EKG repositories.");

        Group iconImage = new CachingSvgLoader(false)
                .setDefaultScale(0.14)
                .getSvgImage("icons/ekg-repository-icon2.svg");

        setGraphic(iconImage);

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResource("/ekg-app-icon2.png").toExternalForm()));
    }
}
