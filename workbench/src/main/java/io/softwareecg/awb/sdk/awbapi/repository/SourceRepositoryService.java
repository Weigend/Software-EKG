package io.softwareecg.awb.sdk.awbapi.repository;

import java.util.List;

/**
 * This interface represents service implementations that are
 * responsible for read and write {@link ImporterSourceRepository} object
 * from/to the EKG repository.
 *
 * ImporterSourceRepository's are used to read TimeSeries data form remote data sources
 * like webservice or databases.
 *
 * @since 03.02.2020
 */
public interface SourceRepositoryService {

    /**
     * Return a list of remote repositories that represents
     * all configured importer query sources
     *
     * @return a list of remote repositories
     */
    List<ImporterSourceRepository> listImportSourceRepositories();

    /**
     * Searches for an import source types that belongs to the project
     * with the given name. If no types exists <code>null</code> will
     * returned.
     *
     * @param projectName the name of the project the types belongs to
     * @return an ImporterSourceRepository instance or null
     */
    ImporterSourceRepository queryImportSourceRepository(String projectName);

    /**
     * Persists the data of ImporterSourceRepository in the EKG repository.
     *
     * For more details use the documentation of {@link ImporterSourceRepository}
     * that provides further information about that special repository type.
     *
     * @param sourceRepository an instance of ImporterSourceRepository to persist. If null nothing will happen.
     */
    void persistImporterSourceRepository(ImporterSourceRepository sourceRepository);

    /**
     * Deletes an ImportSourceRepository by filtering with the name of the project
     * that references the repository.
     * The id of the repository will resolved from the project data.
     *
     * For more details use the documentation of {@link ImporterSourceRepository}
     * that provides further information about that special repository type.
     *
     * @param projectName the name of the project that references the ImportSourceRepository that should deleted.
     */
    void deleteImportSourceRepositoryByProjectName(String projectName);

}
