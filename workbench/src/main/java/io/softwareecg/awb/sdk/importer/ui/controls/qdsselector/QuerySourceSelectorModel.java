package io.softwareecg.awb.sdk.importer.ui.controls.qdsselector;

import io.softwareecg.awb.sdk.awbapi.repository.ImporterSourceRepository;
import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.awbapi.repository.ResourceAuthType;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;

/**
 * The view model for the QuerySourceSelector component
 *
 * @since 28.07.2018
 */
@SuppressWarnings("unused")
public class QuerySourceSelectorModel {

    //-----------------------------------------------------------------------------------------------------------------
    // the properties hold by the model an shown in the view
    //-----------------------------------------------------------------------------------------------------------------

    /**
     * the currently selected types (will do outside in RepositorySelector UI component)
     */
    private final ObjectProperty<Repository> selectedRepository = new SimpleObjectProperty<>();

    private final StringProperty repositoryUrl = new SimpleStringProperty();

    private final ObjectProperty<ResourceAuthType> selectedAuthTyp = new SimpleObjectProperty<>();

    private final StringProperty username = new SimpleStringProperty();

    private final StringProperty password = new SimpleStringProperty();

    private final StringProperty oauthServerUrl = new SimpleStringProperty();

    private boolean isNewProject = true;

    //-----------------------------------------------------------------------------------------------------------------
    // the properties hold by the model that are only used to visualisation the state and output
    //-----------------------------------------------------------------------------------------------------------------

    private final StringProperty projectName = new SimpleStringProperty();

    private final BooleanProperty isValidProjectConfig = new SimpleBooleanProperty(false);

    private ImporterSourceRepository sourceRepository = null;

    //-----------------------------------------------------------------------------------------------------------------
    //  accessor for the properties
    //-----------------------------------------------------------------------------------------------------------------


    public ImporterSourceRepository getSourceRepository() {
        return sourceRepository;
    }

    public void setSourceRepository(ImporterSourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    public String getProjectName() {
        return projectName.get();
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName.set(projectName);
    }

    public ResourceAuthType getSelectedAuthTyp() {
        return selectedAuthTyp.get();
    }

    public ObjectProperty<ResourceAuthType> selectedAuthTypProperty() {
        return selectedAuthTyp;
    }

    public void setSelectedAuthTyp(ResourceAuthType selectedAuthTyp) {
        this.selectedAuthTyp.set(selectedAuthTyp);
    }

    public String getRepositoryUrl() {
        return repositoryUrl.get();
    }

    public StringProperty repositoryUrlProperty() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl.set(repositoryUrl);
    }

    public String getOAuthServerUrl() {
        return oauthServerUrl.get();
    }

    public StringProperty oauthServerUrlProperty() {
        return oauthServerUrl;
    }

    public void setOauthServerUrl(String oauthServerUrl) {
        this.oauthServerUrl.set(oauthServerUrl);
    }

    public void isNewProject(boolean isNewProject) {
        this.isNewProject = isNewProject;
    }

    public boolean isNewProject() {
        return isNewProject;
    }

    public String getUsername() {
        return username.get();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public String getPassword() {
        return password.get();
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValidProjectConfig() {
        return isValidProjectConfig.get();
    }

    public BooleanProperty isValidProjectConfigProperty() {
        return isValidProjectConfig;
    }

    public void setIsValidProjectName(boolean isValidProjectConfig) {
        this.isValidProjectConfig.set(isValidProjectConfig);
    }

    public ObjectProperty<Repository> selectedRepositoryProperty() {
        return selectedRepository;
    }

    public ReadOnlyProperty<ImporterSourceRepository> getImportSourceRepositoryProperty() {

        return new ReadOnlyObjectPropertyBase<>() {

            @Override
            public ImporterSourceRepository get() {

                if (!isValidProjectConfig()) {
                    return null;
                }

                ImporterSourceRepository sourceDefinition = getSourceRepository();

                if (sourceDefinition != null) {
                    return sourceDefinition;
                }

                sourceDefinition = new ImporterSourceRepository();
                sourceDefinition.setAccordingProjectName(getProjectName());
                sourceDefinition.setRepositoryUrl(getRepositoryUrl());
                sourceDefinition.setAuthType(getSelectedAuthTyp());
                sourceDefinition.setUsername(getUsername());
                sourceDefinition.setPassword(getPassword());
                sourceDefinition.setOAuthServerUrl(getOAuthServerUrl());

                setSourceRepository(sourceDefinition);

                return sourceDefinition;
            }

            @Override
            public Object getBean() {
                return QuerySourceSelectorModel.this;
            }

            @Override
            public String getName() {
                return "importSourceRepositoryProperty";
            }
        };
    }

    public BooleanBinding isValidImportSourceDefinitionProperty() {
        return new BooleanBinding() {

            {
                bind(isValidProjectConfig);
            }

            @Override
            protected boolean computeValue() {
                return getImportSourceRepositoryProperty().getValue() != null;
            }
        };
    }
}
