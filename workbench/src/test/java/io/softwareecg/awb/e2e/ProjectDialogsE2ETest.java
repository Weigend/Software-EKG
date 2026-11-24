package io.softwareecg.awb.e2e;

import io.softwareecg.awb.projectcontext.api.CloudPlatformType;
import io.softwareecg.awb.projectcontext.api.Project;
import io.softwareecg.awb.projectcontext.api.ProjectFlavor;
import io.softwareecg.awb.projectcontext.api.ProjectDataAccessService;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositoryService;
import io.softwareecg.awb.repository.api.ResourceAuthType;
import io.softwareecg.awb.repository.api.SourceRepositoryService;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.repository.connection.EkgRepositoryDbType;
import io.softwareecg.awb.repository.connection.RepositoryConfiguration;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two dialogs of a project: the one that adds a project of a remote repository, and the one
 * that edits a project.
 */
class ProjectDialogsE2ETest {

    private static Workbench workbench;

    @BeforeAll
    static void startTheWorkbench() {
        workbench = Workbench.start();
    }

    /** A test that failed half way leaves no repository behind: an import of the next could go into it. */
    @AfterEach
    void removeAddedRepositories() {
        RepositoryService service = Lookup.lookup(RepositoryService.class);
        workbench.onFx(() -> {
            service.listRepositories().stream().filter(repository -> !repository.isEmbedded())
                    .forEach(service::removeRepositoryWithProjects);
            return null;
        });
    }

    /** Only the remote repositories are offered: in the embedded one, a project comes with an import. */
    @Test
    void aProjectOfARemoteRepositoryIsAdded() {
        Repository plant = workbench.onFx(() -> Lookup.lookup(RepositoryConfiguration.class).addEkgRepository("Plant",
                "http://127.0.0.1:1/solr/ekgdata", "ekgdata", EkgRepositoryDbType.SOLR_STANDALONE, ResourceAuthType.NONE, null, null));

        Stage dialog = workbench.openDialog("Repository", "Add Project...");
        ComboBox<?> repositories = Workbench.control(dialog, "cbRepositorySelection", ComboBox.class);
        assertThat(workbench.onFx(() -> repositories.getItems().size())).as("repositories").isEqualTo(1);
        assertThat(workbench.onFx(() -> ((Repository) repositories.getValue()).getRepositoryName())).isEqualTo("Plant");
        fill(dialog, "projectName", "Measurements of the plant");
        choose(dialog, "cbProjectFlavor", ProjectFlavor.CLASSIC);
        choose(dialog, "cbCloudPlatformType", CloudPlatformType.NONE);
        workbench.screenshot(dialog, "project-add-dialog");
        apply(dialog);

        assertThat(ServiceDiscovery.lookup(ProjectDataAccessService.class, plant).listProjects())
                .extracting(Project::getName).containsExactly("Measurements of the plant");
    }

    /** The dialog comes up with the server the values of the project are read from, and stores another. */
    @Test
    void theServerOfAProjectIsEdited() {
        Repository embedded = workbench.embeddedRepository();
        Project remote = new Project("Remote values", null, "CLASSIC", "NONE", true);
        ServiceDiscovery.lookup(ProjectDataAccessService.class, embedded).persistProject(remote);
        TimeSeriesImportSource source = new TimeSeriesImportSource();
        source.setAccordingProjectName("Remote values");
        source.setRepositoryUrl("http://127.0.0.1:1/prometheus");
        source.setAuthType(ResourceAuthType.USERNAME_PASSWORD);
        source.setUsername("somebody");
        source.setPassword("secret");
        SourceRepositoryService sources = ServiceDiscovery.lookup(SourceRepositoryService.class, embedded);
        sources.persistImporterSourceRepository(source);
        workbench.project("Remote values").select();

        Node edit = workbench.find("btnEditProject").orElseThrow();
        Stage dialog = workbench.openDialog("edit the project", () -> edit.getOnMouseClicked().handle(null));
        workbench.screenshot(dialog, "project-edit-dialog");
        assertThat(contentOf(dialog, "projectName")).isEqualTo("Remote values");
        assertThat(contentOf(dialog, "txtRepositoryURL")).isEqualTo("http://127.0.0.1:1/prometheus");
        assertThat(contentOf(dialog, "txtRepositoryUserName")).isEqualTo("somebody");
        fill(dialog, "txtRepositoryURL", "http://127.0.0.1:1/another");
        apply(dialog);

        TimeSeriesImportSource stored = sources.queryImportSourceRepository("Remote values");
        assertThat(stored.getRepositoryUrl()).isEqualTo("http://127.0.0.1:1/another");
        assertThat(stored.getUsername()).isEqualTo("somebody");
        assertThat(stored.getPassword()).isEqualTo("secret");
        assertThat(stored.getAuthType()).isEqualTo(ResourceAuthType.USERNAME_PASSWORD);
    }

    private static void fill(Stage dialog, String id, String text) {
        workbench.onFx(() -> {
            Workbench.control(dialog, id, TextField.class).setText(text);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static void choose(Stage dialog, String id, Object value) {
        workbench.onFx(() -> {
            Workbench.control(dialog, id, ComboBox.class).setValue(value);
            return null;
        });
    }

    private static String contentOf(Stage dialog, String id) {
        return workbench.onFx(() -> Workbench.control(dialog, id, TextField.class).getText());
    }

    private static void apply(Stage dialog) {
        workbench.onFx(() -> {
            Button apply = (Button) ((DialogPane) dialog.getScene().getRoot()).lookupButton(ButtonType.APPLY);
            assertThat(apply.isDisabled()).as("'Apply' is disabled").isFalse();
            Platform.runLater(apply::fire);
            return null;
        });
        workbench.awaitTrue("the dialog to close", Duration.ofSeconds(20), () -> workbench.onFx(() -> !dialog.isShowing()));
    }
}
