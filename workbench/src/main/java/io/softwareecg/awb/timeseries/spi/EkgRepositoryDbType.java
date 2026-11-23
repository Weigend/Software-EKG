package io.softwareecg.awb.timeseries.spi;

import io.softwareecg.awb.runtime.NamedEnum;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stable repository technology identity.
 *
 * <p>This used to be an enum and made every repository extension a change to the Workbench core.
 * It deliberately retains the enum-like {@link #name()} and {@link #valueOf(String)} API for
 * persisted configurations, while allowing providers to contribute further identities.</p>
 */
public final class EkgRepositoryDbType implements NamedEnum {
    private static final Map<String, EkgRepositoryDbType> KNOWN = new LinkedHashMap<>();

    public static final EkgRepositoryDbType SOLR_EMBEDDED = define("SOLR_EMBEDDED", "Solr Embedded");
    public static final EkgRepositoryDbType SOLR_CLOUD = define("SOLR_CLOUD", "Solr Cloud");
    public static final EkgRepositoryDbType SOLR_STANDALONE = define("SOLR_STANDALONE", "Solr (Classic)");
    public static final EkgRepositoryDbType ELASTICSEARCH_STANDALONE =
            define("ELASTICSEARCH_STANDALONE", "ElasticSearch Standalone");
    public static final EkgRepositoryDbType CLICKHOUSE_CLOUD = define("CLICKHOUSE_CLOUD", "ClickHouse Cloud");

    private final String id;
    private String displayName;

    private EkgRepositoryDbType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static synchronized EkgRepositoryDbType define(String id, String displayName) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        if (id.isBlank()) throw new IllegalArgumentException("Repository type id must not be blank");
        EkgRepositoryDbType existing = KNOWN.get(id);
        if (existing != null) {
            if (existing.displayName.equals(existing.id)) existing.displayName = displayName;
            return existing;
        }
        EkgRepositoryDbType type = new EkgRepositoryDbType(id, displayName);
        KNOWN.put(id, type);
        return type;
    }

    /** Resolves persisted IDs; unknown IDs remain representable until their provider is loaded. */
    public static synchronized EkgRepositoryDbType valueOf(String id) {
        return KNOWN.computeIfAbsent(id, key -> new EkgRepositoryDbType(key, key));
    }

    public static synchronized EkgRepositoryDbType[] values() {
        return KNOWN.values().toArray(EkgRepositoryDbType[]::new);
    }

    public String name() { return id; }

    @Override public String getName() { return displayName; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof EkgRepositoryDbType type && id.equals(type.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    @Override public String toString() { return id; }
}
