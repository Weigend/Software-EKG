package io.softwareecg.awb.metadata.port;

import io.softwareecg.awb.metadata.model.DimensionAliasRow;

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
