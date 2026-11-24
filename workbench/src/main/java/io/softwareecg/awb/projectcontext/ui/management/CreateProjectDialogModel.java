package io.softwareecg.awb.projectcontext.ui.management;

import io.softwareecg.awb.projectcontext.api.Project;
import io.softwareecg.awb.repository.api.Repository;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;

import java.util.Collection;

/**
 * Bean that represents the underlying model of the CreateProjectDialog.
 * It stores all data need to define a project and some additional validation features.
 *
 * @since 21.06.2018
 */
public class CreateProjectDialogModel extends ProjectDialogModel {

    /**
     * A boolean binding that will be only true if all project properties are well defined. This means not
     * null and in case of the project name not blank and unique for the chooses repository.
     */
    private final BooleanBinding isValidProjectConfig = getIsValidProjectDefinitionProperty();

    //=================================================================================================================
    // Accessor API of the CreateProjectDialogModel
    //=================================================================================================================

    public Project getConfiguredProject() {
        return new Project(projectName.get(), null, projectFlavor.get().toString(), cloudPlatformType.get().toString());
    }

    public BooleanBinding isValidProjectConfigProperty() {
        return isValidProjectConfig;
    }

    public void setProjectNames(Collection<String> newProjectNames) {
        projectNames.clear();

        //noinspection UseBulkOperation (addAll is unsupported)
        newProjectNames.forEach(projectNames::add);
    }

    public ObjectProperty<Repository> selectedRepositoryProperty() {
        return selectedRepository;
    }
}
