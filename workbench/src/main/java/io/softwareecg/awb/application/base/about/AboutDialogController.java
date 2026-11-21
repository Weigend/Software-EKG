//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.application.base.about;

import io.softwareecg.awb.common.about.VersionInfo;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import jakarta.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ResourceBundle;

/**
 */
public class AboutDialogController implements Initializable {

    /**
     * The class logger
     */
    private static final Logger LOGGER = EkgLogger.get();

    @FXML
    private Label lblBuildDate;

    @FXML
    private Label lblRevision;

    @FXML
    private Label lblCopyrightYear;

    @FXML
    private Label lblProductVersion;

    @FXML
    private Label lblJavaVersion;

    @FXML
    private Label lblRuntimeVersion;

    @FXML
    private Label lblSystemVersion;

    @FXML
    private Label lbFounderCredit;

    @FXML
    private Hyperlink linkHomepage;

    @FXML
    private ImageView ivAboutBanner;

    @FXML
    private ImageView ivFounderBanner;

    @FXML
    private Pane ivFounderBannerWrapper;

    @Inject
    private VersionInfo versionInfo;

    /**
     * Initialize the controller
     *
     * @param url            - the url
     * @param resourceBundle - the resource bundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initHandlers();

        setSystemInformation();

        initBehaviour();
    }

    private void initBehaviour() {
        Image image = new Image(getClass().getResource("giphy.gif").toExternalForm());


        Rectangle rect = new Rectangle(0, 0, 800, 440);
        ivFounderBanner.setImage(image);
        ivFounderBanner.setFitWidth(800);
        ivFounderBanner.setClip(rect);


        //ivFounderBanner.setClip(rect);
        ivFounderBannerWrapper.setVisible(false);
        ivFounderBannerWrapper.setMaxHeight(400);

    }


    /**
     * Sets the system information labels
     */
    private void setSystemInformation() {

        lblProductVersion.setText(versionInfo == null ? "Software EKG" : "Software EKG Version " + versionInfo.getVersionString());
        lblRuntimeVersion.setText(ManagementFactory.getRuntimeMXBean().getVmName());
        lblJavaVersion.setText(System.getProperty("java.version"));
        lblSystemVersion.setText(ManagementFactory.getOperatingSystemMXBean().getName());
        if (versionInfo != null) {
            //lblVersion.setText(versionInfo.getVersionString());
            lblCopyrightYear.setText(String.valueOf(LocalDateTime.ofInstant(versionInfo.getBuildTime(), ZoneId.systemDefault()).getYear()));
            lblBuildDate.setText(versionInfo.getBuildTime().toString());
            lblRevision.setText(versionInfo.getBuildRevision());
        }

        linkHomepage.setOnAction(event -> {
            Thread thread = new Thread(() -> {
                try {
                    new ProcessBuilder("xdg-open", "https://www.software-ekg.de").start();
                } catch (Exception e) {
                    LOGGER.warn("Could not open the url in the default system browser.", e);
                }
            });
            thread.setDaemon(true);
            thread.start();
        });
    }

    /**
     * Inits the button handlers
     */
    private void initHandlers() {
        lbFounderCredit.setOnMouseClicked(event -> {
            founderClickHandler();
            lbFounderCredit.setText(ivFounderBannerWrapper.isVisible() ? "Got it!" : "Who invented it?!");
        });
    }

    private void founderClickHandler() {
        ivFounderBannerWrapper.setVisible(!ivFounderBannerWrapper.isVisible());
    }

    /**
     * Closes the about dialog
     *
     * @param event - the action event
     */
    private void closeWindow(ActionEvent event) {
        Node button = (Node) event.getSource();
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
