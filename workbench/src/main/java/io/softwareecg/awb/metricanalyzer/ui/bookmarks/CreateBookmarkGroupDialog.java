package io.softwareecg.awb.metricanalyzer.ui.bookmarks;

import io.softwareecg.awb.runtime.di.BeanProvider;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.BookmarkGroup;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * A simple dialog that will used to create a new {@link BookmarkGroup} instance
 * that can used group bookmarks of a specific domain/topic.
 *
 * @since 26.02.2020
 */
public class CreateBookmarkGroupDialog extends Dialog<BookmarkGroup> {

    private final EkgRepository targetRepository;

    @FXML
    private TextField bookmarkGroupName;

    /**
     * Initialize a new create bookmark dialog.
     *
     * @param repository the repository used to store the bookmark
     */
    public CreateBookmarkGroupDialog(EkgRepository repository) {

        try {
            BeanProvider.injectFields(this);

            FXMLLoader fxmlLoader = Lookup.lookup(FXMLLoader.class);
            fxmlLoader.setLocation(CreateBookmarkDialog.class.getResource("CreateBookmarkGroupDialogView.fxml"));
            fxmlLoader.setController(this);

            setDialogPane(fxmlLoader.load());
            setTitle("Create Bookmark Group");

            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResource("bookmark-icon.png").toExternalForm()));
            setResultConverter(this::createBookmarkGroup);

            Button applyButton = (Button)getDialogPane().lookupButton(ButtonType.APPLY);
            applyButton.getStyleClass().add("applyButton");
            applyButton.setDefaultButton(true);
            applyButton.setDisable(true);
            applyButton.setOnKeyReleased(event -> {
                if (!isValid()) {
                    event.consume();
                }

                if(event.getCode() == KeyCode.ENTER) {
                    CreateBookmarkGroupDialog.this.setResult(createBookmarkGroup(ButtonType.APPLY));
                }
            });

            applyButton.addEventFilter(ActionEvent.ACTION, event -> {
                if (!isValid()) {
                    event.consume();
                }
            });

            bookmarkGroupName.textProperty().addListener(observable -> applyButton.setDisable(!isValid()));
            bookmarkGroupName.requestFocus();
            targetRepository = repository;
        } catch (IOException e) {
            throw new IllegalStateException("Can not load fxml file for CreateBookmarkDialog", e);
        }
    }

    /**
     * Returns the name of the bookmark group.
     *
     * @return the name of the bookmark group
     */
    public String getName() {
        return bookmarkGroupName.getText();
    }

    /**
     * Get the target types.
     *
     * @return the target types.
     */
    public EkgRepository getTargetRepository() {
        return targetRepository;
    }

    /**
     * Check if the current dialog state is valid.
     *
     * @return true if the dialog is valid. false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isValid() {
        return StringUtils.isNotBlank(bookmarkGroupName.getText()) && targetRepository != null;
    }

    private BookmarkGroup createBookmarkGroup(ButtonType buttonType) {

        if (buttonType != ButtonType.APPLY || getTargetRepository() == null || StringUtils.isBlank(bookmarkGroupName.getText())) {
            return null;
        }

        BookmarkGroup group = new BookmarkGroup();
        group.setName(bookmarkGroupName.getText());
        return group;
    }
}
