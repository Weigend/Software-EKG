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
package io.softwareecg.awb.explorer.metric.items.bookmarks;

import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.runtime.di.BeanProvider;
import io.softwareecg.awb.bookmark.model.BookmarkGroup;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.EventObject;
import java.util.Optional;

import static io.softwareecg.awb.explorer.items.TaskUtils.runAsTask;

/**
 * A tree node item that represents a domain specific/topic specific group
 * of bookmarks that are defined by a EKG user to group a specific set of bookmarks.
 *
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection"})
public class BookmarkGroupItem extends ContextAwareItem {

    private final BookmarkGroup bookmarkGroup;

    @Inject
    private EventBus<EventObject> eventBus;

    /**
     * Creates a new BookmarkGroupItem based on the given queryParams using 'Bookmarks' as name
     *
     * @param bookmarkGroup the {@link BookmarkGroup} instance this explorer node represents (and is bind to)
     * @param queryParams - the query context.
     */
    public BookmarkGroupItem(BookmarkGroup bookmarkGroup, FilterContext queryParams) {
        super(bookmarkGroup.getName(), queryParams);
        super.setGraphic(getIconProvider().getBookmarkGroupItemIcon(this));
        this.bookmarkGroup = bookmarkGroup;

        BeanProvider.injectFields(this);

        addContextMenuEntry("Delete Group",

                event -> {

                    Alert alert = new Alert(Alert.AlertType.WARNING);

                    DialogPane alertPane = alert.getDialogPane();
                    alertPane.getStylesheets().add("/io/softwareecg/awb/bookmark/ui/CreateBookmarkDialogStyle.css");
                    Stage stage = (Stage) alertPane.getScene().getWindow();
                    stage.getIcons().add(new Image("/io/softwareecg/awb/metricview/icons/warning.png"));
                    ImageView imageView = new ImageView("/io/softwareecg/awb/metricview/icons/warning.png");
                    imageView.setFitWidth(36);
                    imageView.setFitHeight(33);
                    alertPane.setGraphic(imageView);

                    alert.setTitle("Confirm Deletion");
                    alert.setHeaderText("Bookmarks at Risk!");
                    alert.setContentText("Deleting this group will also permanently remove all associated bookmarks. " +
                            "This affects all users of the repository.\n\nDo you really want to delete this group?\n ");
                    alert.getButtonTypes().add(ButtonType.YES);
                    alert.getButtonTypes().remove(ButtonType.OK);
                    alert.getButtonTypes().add(ButtonType.CANCEL);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.YES){
                        runAsTask(
                                "delete-bookmarkGroup-" + bookmarkGroup.getName() + "-task",
                                this::deleteBookmarkGroup,
                                finishedEvent -> eventBus.publish(new ExplorerUpdateEvent(getParent()))
                        );
                    }

                }
        );
    }

    /**
     * Returns the unique id of the underlying bookmark group
     * that is represented by this tree item.
     *
     * @return the unique id of th bookmark group
     */
    public String getBookmarkGroupId() {
        return bookmarkGroup.getId();
    }

    private void deleteBookmarkGroup() {
        getFilterContext().bookmarks().deleteBookmarkGroup(bookmarkGroup.getId());
        Platform.runLater(() -> ((AbstractItem<String>) getParent()).getService().restart());
    }
}
