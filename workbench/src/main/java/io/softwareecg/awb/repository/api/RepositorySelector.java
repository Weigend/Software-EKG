package io.softwareecg.awb.repository.api;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import java.util.function.Predicate;

/**
 * This interface is the minimum API of an JavaFX UI component that allows users
 * to select on the existing EKG repositories as target for the import process.
 * Each selector component will derive from JavaFX Node with all its
 * properties and methods but this interface provides the domain API of the component.
 *
 * @since 30.01.2020
 */
public interface RepositorySelector {

    /**
     * Offers only some of the repositories.
     *
     * @param filter accepts a repository the user may choose
     */
    void setFilter(Predicate<Repository> filter);

    void setLabelWidth(double labelWidth);

    /**
     * Returns the EKG target repository that
     * is selected by the user (or default)
     *
     * @return the repository that will uses as target for the time series import
     */
    Repository getSelectedRepository() ;

    /**
     * Returns a reference to the ObjectProperty that stores the current
     * selected EKG repository. The property can used to register listeners
     * or various kinds of validations.
     *
     * @return the ObjectProperty that stores the selected repository
     */
    ObjectProperty<Repository> selectedRepositoryProperty();

    /**
     * Triggers that component to update all it internal states and
     * the according view representation. This method can use to enforce
     * view updates to make changes visible that normally doesn't trigger
     * view state updates.
     */
    void updateState();

    /**
     * Returns a reference to the JavaFX Node instance that should be
     * the technical base of each RepositorySelector UI component.
     * The Node reference can used to embed the component in other JavaFX components.
     *
     * @return a JavaFX Node reference to the selector component
     */
    Node asJfxComponent();
}
