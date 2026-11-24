package io.softwareecg.awb.bookmark.ui;

import io.softwareecg.awb.bookmark.store.MetricsBookmarkServiceImpl;
import io.softwareecg.awb.bookmark.api.BookmarkDialogs;
import io.softwareecg.awb.bookmark.api.BookmarkGroup;
import io.softwareecg.awb.bookmark.store.MetricQueryBookmark;
import io.softwareecg.awb.query.model.QuerySpec;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;
import java.util.UUID;

/**
 * The dialogs of the bookmarks.
 */
@Singleton
public class BookmarkDialogsImpl implements BookmarkDialogs {

    private final MetricsBookmarkServiceImpl bookmarks;

    @Inject
    public BookmarkDialogsImpl(MetricsBookmarkServiceImpl bookmarks) {
        this.bookmarks = bookmarks;
    }

    @Override
    public boolean createBookmark(Window owner, QuerySpec query, String chartType) {
        MetricQueryBookmark.Builder builder = new MetricQueryBookmark.Builder(query).withChartType(chartType);
        CreateBookmarkDialog<MetricQueryBookmark> dialog = new CreateBookmarkDialog<>(builder, query.getProjectName());
        dialog.initOwner(owner);

        Optional<BookmarkCreationResult<MetricQueryBookmark>> created = dialog.showAndWait();
        created.ifPresent(result -> bookmarks.persistNewBookmark(result.getBookmark(), result.getBookmarkGroup()));
        return created.isPresent();
    }

    @Override
    public boolean createGroup(Window owner, String projectName) {
        CreateBookmarkGroupDialog dialog = new CreateBookmarkGroupDialog();
        dialog.initOwner(owner);

        Optional<BookmarkGroup> created = dialog.showAndWait();
        created.ifPresent(group -> {
            group.setId(UUID.randomUUID().toString());
            group.setProjectName(projectName);
            bookmarks.persistNewBookmarkGroup(group);
        });
        return created.isPresent();
    }

    @Override
    public boolean deleteGroup(Window owner, BookmarkGroup group) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(owner);

        Image warning = new Image(getClass().getResource("warning.png").toExternalForm());
        ImageView graphic = new ImageView(warning);
        graphic.setFitWidth(36);
        graphic.setFitHeight(33);

        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("CreateBookmarkDialogStyle.css").toExternalForm());
        pane.setGraphic(graphic);
        ((Stage) pane.getScene().getWindow()).getIcons().add(warning);

        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Bookmarks at Risk!");
        alert.setContentText("Deleting this group will also permanently remove all associated bookmarks. " +
                "This affects all users of the repository.\n\nDo you really want to delete this group?\n ");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);

        boolean confirmed = alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
        if (confirmed) {
            bookmarks.deleteBookmarkGroup(group.getId());
        }
        return confirmed;
    }
}
