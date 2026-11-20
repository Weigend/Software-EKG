package io.softwareecg.awb.repository.bl.repositories;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods to derive valid ClickHouse table names from repository metadata.
 */
public final class ClickHouseRepositoryNaming {

    private ClickHouseRepositoryNaming() {
    }

    public static String resolveTableName(String configuredIndexName, String repositoryName, String repositoryId) {
        if (StringUtils.isNotBlank(configuredIndexName)) {
            return configuredIndexName.trim();
        }

        String normalizedName = normalizeIdentifier(repositoryName);
        if (StringUtils.isNotBlank(normalizedName)) {
            return normalizedName;
        }

        String normalizedId = normalizeIdentifier(repositoryId);
        if (StringUtils.isNotBlank(normalizedId)) {
            return normalizedId;
        }

        return "ekg_clickhouse_repository";
    }

    static String normalizeIdentifier(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        String normalized = value.trim().replaceAll("[^a-zA-Z0-9_]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = StringUtils.strip(normalized, "_");

        if (StringUtils.isBlank(normalized)) {
            return null;
        }

        if (!Character.isLetter(normalized.charAt(0)) && normalized.charAt(0) != '_') {
            normalized = "ekg_" + normalized;
        }

        return normalized.toLowerCase();
    }
}
