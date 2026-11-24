package io.softwareecg.awb.importer.ui.controls;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositorySelector;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.util.function.Predicate;

/**
 * This is a JavaFX proxy component/placeholder that wraps the real RepositorySelector
 * provided by the ABW implementation.
 * It can used by importers to get the repository selection capability.
 *
 * @since 13.02.2020
 */
public class RepositorySelectorProxy extends BorderPane implements RepositorySelector {

    private RepositorySelector repositorySelector;

    public RepositorySelectorProxy() {
        initialize();
    }

    public void initialize() {
        repositorySelector = Lookup.lookup(RepositorySelector.class);

        if (repositorySelector == null) {
            throw new IllegalStateException("Expect a RepositorySelector instance in classpath.");
        }

        this.setCenter(repositorySelector.asJfxComponent());
        this.setMinHeight(34);
    }

    public RepositorySelector getAdaptedRepositorySelector() {

        if (repositorySelector == null) {
            repositorySelector = Lookup.lookup(RepositorySelector.class);

            if (repositorySelector == null) {
                throw new IllegalStateException("Expect a ProjectSelector instance in classpath.");
            }
        }

        return repositorySelector;
    }

    @Override
    public void setFilter(Predicate<Repository> filter) {
        repositorySelector.setFilter(filter);
    }

    @Override
    public void setLabelWidth(double labelWidth) {
        repositorySelector.setLabelWidth(labelWidth);
    }

    public double getLabelWidth() {
        return repositorySelector.getLabelWidth();
    }

    @Override
    public Repository getSelectedRepository() {
        return getAdaptedRepositorySelector().getSelectedRepository();
    }

    @Override
    public ObjectProperty<Repository> selectedRepositoryProperty() {
        return getAdaptedRepositorySelector().selectedRepositoryProperty();
    }

    @Override
    public void updateState() {
        getAdaptedRepositorySelector().updateState();
    }

    @Override
    public Node asJfxComponent() {
        return this;
    }
}