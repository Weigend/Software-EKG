//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.repository.ui.admin;

import afester.javafx.svg.SvgLoader;
import io.softwareecg.awb.repository.connection.EkgRepository;
import io.softwareecg.awb.repository.api.AddRepositoryRequest;
import io.softwareecg.awb.repository.api.EditRepositoryRequest;
import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import io.softwareecg.awb.runtime.resources.ViewLoadResult;
import javafx.scene.Group;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.InputStream;

/**
 * The connection controller controls the Dialog where the user can
 * addAndSum Repositories from the network/from URLs.
 *
 * @since 06.08.2018
 */
public class EkgRepositoryAdmin extends Dialog<EkgRepository> {

    private final EkgRepositoryAdminController controller;

    /**
     * Creates a new EkgRepositoryAdmin
     */
    public EkgRepositoryAdmin() {

        setTitle("Add EKG Repository");

        ViewLoadResult viewResult = CdiFxmlLoader.loadView("EkgRepositoryAdminView.fxml");
        setDialogPane(viewResult.getComponent());

        controller = viewResult.getController();
        controller.setParent(this);

        // bind controller to Dialog closed with apply
        setResultConverter(controller::openConnection);

        setHeaderText("To add another EKG repository, please specify the type and " +
                "connection details.\nThe inputs and connection will be verified immediately.");

        InputStream iconStream = EkgRepositoryAdmin.class.getResourceAsStream("icons/ekg-repository-icon2.svg");
        Group iconImage = new SvgLoader().loadSvg(iconStream);
        iconImage.setScaleX(0.14);
        iconImage.setScaleY(0.14);
        setGraphic(new Group(iconImage));

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResource("/ekg-app-icon2.png").toExternalForm()));
    }

    public boolean addRepository(AddRepositoryRequest event) {
        initOwner((Window) event.getOwnerWindow());
        show();
        return true;
    }

    public boolean editRepository(EditRepositoryRequest event) {
        return controller.editRepository(event);
    }

    public void setOldRepository(EkgRepository repository) {
        controller.setOldRepository(repository);
    }
}
