package io.softwareecg.awb.e2e;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>Help &rarr; About Software-EKG</b>: the dialog with the logo and the version of the build.
 */
class AboutDialogE2ETest {

    private static Workbench workbench;

    @BeforeAll
    static void startTheWorkbench() {
        workbench = Workbench.start();
    }

    /**
     * The logo is a picture that is named in the FXML of the dialog. A name that fits no file
     * shows only when the dialog is opened.
     */
    @Test
    void theAboutDialogShowsItsLogo() {
        Stage about = workbench.openDialog("Help", "About Software-EKG");
        try {
            workbench.screenshot(about, "about-dialog");
            List<ImageView> pictures = workbench.onFx(() -> about.getScene().getRoot().lookupAll(".image-view").stream()
                    .filter(ImageView.class::isInstance).map(ImageView.class::cast)
                    .filter(Node::isVisible)
                    .toList());
            assertThat(pictures).as("pictures of the About dialog").isNotEmpty();
            assertThat(pictures)
                    .as("pictures of the About dialog that were loaded")
                    .anySatisfy(picture -> {
                        assertThat(picture.getImage()).isNotNull();
                        assertThat(picture.getImage().isError()).isFalse();
                        assertThat(picture.getImage().getWidth()).isGreaterThan(500);
                    });
        } finally {
            workbench.close(about);
        }
    }
}
