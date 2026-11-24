package io.softwareecg.awb.metadata.api;

import java.util.List;

/**
 * Persistenz-Schnittstelle fuer Repository-Konfigurationen (EKG-Targets und
 * Importer-Quellen). Wird von DA-Adaptern (z.B. awb-da-h2) implementiert und
 * vom Repository-Layer konsumiert.
 *
 * Der Repository-Layer kennt damit nur diese API, nicht die konkrete
 * Persistenz-Technologie.
 */
public interface RepositoryConfigStore {

    /**
     * Listet alle Repository-Konfigurationen (EKG-Targets und Importer-Quellen).
     */
    List<RepositoryConfigRow> listAll();

    /**
     * Liefert die Importer-Source-Repository-Zeile fuer ein Projekt, oder null.
     *
     * @throws IllegalStateException wenn mehr als eine Quelle gefunden wird
     */
    RepositoryConfigRow findImportSourceByProject(String projectName);

    /**
     * Fuegt eine neue Konfigurationszeile ein. Setzt eine ID falls noch keine gesetzt ist.
     */
    RepositoryConfigRow insert(RepositoryConfigRow row);

    /**
     * Updates the connection data of the target repository with the row's id in place. The id
     * stays the same, so the projects stored for the repository keep pointing at it.
     *
     * @param row the new connection data; {@code row.id} names the repository to update
     * @return {@code false} if no repository with that id exists
     */
    boolean update(RepositoryConfigRow row);

    /**
     * Deletes a target repository together with the metadata of all projects that are
     * stored for it - and with the projects their dimension aliases, bookmarks and import
     * sources. Either everything is deleted or nothing.
     *
     * @param id the repository id
     */
    void deleteWithProjects(String id);

    /**
     * Loescht alle Importer-Source-Zeilen fuer ein Projekt.
     */
    void deleteImportSourceByProject(String projectName);
}
