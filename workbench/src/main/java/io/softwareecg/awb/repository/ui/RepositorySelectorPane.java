//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.repository.ui;

import io.softwareecg.awb.repository.connection.EkgRepository;
import io.softwareecg.awb.repository.connection.RepositoryConfiguration;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositorySelector;
import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import io.avaje.inject.Prototype;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Control to select a specific types.
 *
 */
@Priority(100)
@Prototype
public class RepositorySelectorPane extends GridPane implements Initializable, RepositorySelector {

    private final ListProperty<EkgRepository> repositories = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final DoubleProperty labelWidth = new SimpleDoubleProperty();

    private final StringProperty label = new SimpleStringProperty( "Repository:");

    private final ObjectProperty<Predicate<Repository>> filter = new SimpleObjectProperty<>();

    private final ObjectProperty<Repository> selectedRepository = new SimpleObjectProperty<>();

    private final RepositoryConfiguration configuration;

    @FXML
    private ComboBox<EkgRepository> cbRepositorySelection;

    @FXML
    private Label repoLabel;

    @FXML
    private RepositorySelectorPane repositorySelector;

    /**
     * Initialize a new EkgRepository selector. The service has to be there before the view is
     * loaded, since loading the view calls {@link #initialize(URL, ResourceBundle)}, which lists
     * the repositories.
     */
    @Inject
    public RepositorySelectorPane(RepositoryConfiguration configuration) {
        this.configuration = configuration;
        CdiFxmlLoader.loadView("RepositorySelector.fxml", this, this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbRepositorySelection.setConverter(new RepositoryStringConverter());
        cbRepositorySelection.itemsProperty().bind(repositories);

        labelWidth.addListener((javafx.beans.value.ChangeListener<? super Number>) (observable, oldValue, newValue) -> {
            repoLabel.setPrefWidth(newValue.doubleValue());
            repoLabel.setMaxWidth(newValue.doubleValue());
            repoLabel.setMinWidth(newValue.doubleValue());
        });


        selectedRepository.bind(cbRepositorySelection.getSelectionModel().selectedItemProperty());
        filter.addListener((o, ov, nv) -> repositoriesChanged());
        repositoriesChanged();
    }

    @Override
    public Node asJfxComponent() {
        return this;
    }

    private void repositoriesChanged() {
        repositories.clear();

        Predicate<Repository> predicate = getFilter();
        List<EkgRepository> repositoryList = configuration.listEkgRepositories();

        repositoryList.stream().filter(predicate).forEach(repositories::add);

        if (repositories.isEmpty()) {
            cbRepositorySelection.setValue(null);
            cbRepositorySelection.setDisable(true);
        } else {
            cbRepositorySelection.setValue(getRepositories().get(0));
            cbRepositorySelection.setDisable(false);
        }

    }

    /**
     * Lists the repositories again: a dialog that is shown anew calls it. The control does not
     * listen to changes of the repositories, so that nothing keeps it when its dialog is gone.
     */
    @Override
    public void updateState() {
        repositoriesChanged();
    }

    /**
     * Get the current used filter. In case of the filter is set to null it will always
     * return a predicate that accepts all types.
     *
     * @return The currently used filter predicate.
     */
    public Predicate<Repository> getFilter() {
        return filter.get() != null ? filter.get() : r -> true;
    }

    @Override
    public void setFilter(Predicate<Repository> filter) {
        this.filter.set(filter);
    }

    /**
     * Get the filter predicate property.
     *
     * @return The filter property.
     */
    public ObjectProperty<Predicate<Repository>> filterProperty() {
        return filter;
    }

    /**
     * Get the currently selected types.
     *
     * @return the selected types.
     */
    public Repository getSelectedRepository() {
        return selectedRepository.get();
    }

    /**
     * Get the selected types property.
     *
     * @return the selected types property.
     */
    public ObjectProperty<Repository> selectedRepositoryProperty() {
        return selectedRepository;
    }

    /**
     * Get the types list.
     *
     * @return the types list
     */
    public ObservableList<EkgRepository> getRepositories() {
        return repositories.get();
    }

    /**
     * The property list for the types
     *
     * @return the list property.
     */
    public ReadOnlyListProperty<EkgRepository> repositoriesProperty() {
        return repositories;
    }

    /**
     * Get the labels text.
     *
     * @return The labels text.
     */
    public String getLabel() {
        return label.get();
    }

    /**
     * Set the text for label.
     *
     * @param label The label text.
     */
    public void setLabel(String label) {
        this.label.set(label);
    }

    /**
     * Get the property for the labels text.
     *
     * @return The property for the labels text.
     */
    public StringProperty labelProperty() {
        return label;
    }

    /**
     * Get the width of the label.
     *
     * @return the label width.
     */
    public double getLabelWidth() {
        return labelWidth.get();
    }

    /**
     * Set the width of the label.
     *
     * @param labelWidth the width of the label.
     */
    public void setLabelWidth(double labelWidth) {
        this.labelWidth.set(labelWidth);
    }

    /**
     * Get the the width property of the label.
     *
     * @return the width property of the label.
     */
    public DoubleProperty labelWidthProperty() {
        return labelWidth;
    }


}
