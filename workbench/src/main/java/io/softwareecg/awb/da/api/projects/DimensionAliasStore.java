package io.softwareecg.awb.da.api.projects;

/**
 * Persistenz-Schnittstelle fuer Dimension-Alias-Mappings pro Projekt.
 */
public interface DimensionAliasStore {

    /**
     * Liefert die Alias-Konfiguration fuer ein Projekt, oder null falls keine
     * existiert.
     */
    DimensionAliasRow findByProject(String projectName);

    /**
     * Insert-or-update der Alias-Konfiguration fuer ein Projekt.
     */
    void insertOrUpdate(DimensionAliasRow row);
}
