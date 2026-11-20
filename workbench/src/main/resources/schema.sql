-- Software-EKG H2 Metadata Schema
-- Metadaten fuer Projekte, Repository-Konfigurationen und Bookmarks.
-- Zeitreihendaten werden separat in Solr/SolrCloud gespeichert.

CREATE TABLE IF NOT EXISTS projects (
    id VARCHAR(36) DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1024),
    project_flavor VARCHAR(50),
    cloud_platform_type VARCHAR(50),
    is_split_source BOOLEAN DEFAULT FALSE,
    importer_id VARCHAR(255),
    target_repository_id VARCHAR(36)
);

-- Separate Tabelle fuer Dimensions-Alias-Konfiguration pro Projekt
CREATE TABLE IF NOT EXISTS project_dimension_aliases (
    id VARCHAR(36) DEFAULT RANDOM_UUID() PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL UNIQUE,
    alias_host_group VARCHAR(255),
    alias_host VARCHAR(255),
    alias_namespace VARCHAR(255),
    alias_service VARCHAR(255),
    alias_pod VARCHAR(255),
    alias_container VARCHAR(255),
    alias_measurement VARCHAR(255),
    alias_process VARCHAR(255),
    alias_metric_group VARCHAR(255),
    alias_metric_name VARCHAR(255),
    CONSTRAINT fk_alias_project FOREIGN KEY (project_name)
        REFERENCES projects(name) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_alias_project ON project_dimension_aliases(project_name);

CREATE TABLE IF NOT EXISTS repositories (
    id VARCHAR(36) DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(255),
    url VARCHAR(1024),
    db_index_name VARCHAR(255),
    db_type VARCHAR(50),
    auth_type VARCHAR(50),
    username VARCHAR(255),
    password VARCHAR(512),
    oauth_server_url VARCHAR(1024),
    sso_refresh_token VARCHAR(2048),
    is_import_source BOOLEAN DEFAULT TRUE,
    according_project_name VARCHAR(255),
    CONSTRAINT fk_repo_project FOREIGN KEY (according_project_name)
        REFERENCES projects(name) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_repo_project ON repositories(according_project_name);
CREATE INDEX IF NOT EXISTS idx_repo_is_import ON repositories(is_import_source);

CREATE TABLE IF NOT EXISTS bookmark_groups (
    id VARCHAR(36) DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    project_name VARCHAR(255),
    CONSTRAINT fk_bg_project FOREIGN KEY (project_name)
        REFERENCES projects(name) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_bg_project ON bookmark_groups(project_name);

-- Default-Gruppe fuer Bookmarks ohne explizite Gruppenzuordnung
MERGE INTO bookmark_groups (id, name) KEY(id) VALUES ('_BOOKMARK_GLOBAL_', 'Global');

CREATE TABLE IF NOT EXISTS metric_bookmarks (
    id VARCHAR(36) DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(255),
    bookmark_group_id VARCHAR(36),
    command_protocol CLOB,
    project_name VARCHAR(255),
    CONSTRAINT fk_mb_group FOREIGN KEY (bookmark_group_id)
        REFERENCES bookmark_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_mb_project FOREIGN KEY (project_name)
        REFERENCES projects(name) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_mb_group ON metric_bookmarks(bookmark_group_id);
CREATE INDEX IF NOT EXISTS idx_mb_project ON metric_bookmarks(project_name);

CREATE TABLE IF NOT EXISTS metric_query_bookmarks (
    id VARCHAR(36) DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(255),
    bookmark_group_id VARCHAR(36),
    project_name VARCHAR(255),
    query_data CLOB,
    CONSTRAINT fk_mqb_group FOREIGN KEY (bookmark_group_id)
        REFERENCES bookmark_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_mqb_project FOREIGN KEY (project_name)
        REFERENCES projects(name) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_mqb_group ON metric_query_bookmarks(bookmark_group_id);
CREATE INDEX IF NOT EXISTS idx_mqb_project ON metric_query_bookmarks(project_name);

-- Datenquellen eines Projekts: woraus die AWB die Daten des Projekts laufend holt.
-- Ein Projekt kann mehrere Quellen haben. Jede Quelle hat eine eigene Identitaet, einen Typ
-- (z.B. prometheus), einen Ausfuehrungsort (z.B. EMBEDDED = Collector in der AWB) und laesst
-- sich einzeln an- und abschalten. Zugangsdaten stehen NICHT hier: "config" enthaelt nur einen
-- Platzhalter, "secret_ref" verweist auf den Eintrag im Secret Store.
CREATE TABLE IF NOT EXISTS collection_sources (
    id VARCHAR(36) PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    execution_mode VARCHAR(50) NOT NULL,
    endpoint VARCHAR(1024),
    config CLOB NOT NULL,
    secret_ref VARCHAR(255),
    series_namespace VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_project FOREIGN KEY (project_name)
        REFERENCES projects(name) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_source_project ON collection_sources(project_name);
-- Migration: Namensraum der Serien einer Quelle (Messung), fuer bestehende Datenbanken
ALTER TABLE collection_sources ADD COLUMN IF NOT EXISTS series_namespace VARCHAR(255);

-- Verlauf einer Datenquelle: Zustandswechsel wie gestartet, Fehler, wieder in Ordnung, pausiert.
CREATE TABLE IF NOT EXISTS collection_source_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(36) NOT NULL,
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    state VARCHAR(50) NOT NULL,
    message VARCHAR(2048),
    CONSTRAINT fk_event_source FOREIGN KEY (source_id)
        REFERENCES collection_sources(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_event_source ON collection_source_events(source_id);

-- Migration: Spalte fuer bestehende Datenbanken hinzufuegen
ALTER TABLE projects ADD COLUMN IF NOT EXISTS target_repository_id VARCHAR(36);

-- Migration: Zeitstempel fuer Projekte
ALTER TABLE projects ADD COLUMN IF NOT EXISTS created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Migration: Bestehende Projekte ohne Repository-Zuordnung dem ersten
-- konfigurierten Target-Repository zuweisen (embedded repo ist nicht in der Tabelle)
UPDATE projects SET target_repository_id = (
    SELECT id FROM repositories WHERE is_import_source = FALSE LIMIT 1
) WHERE target_repository_id IS NULL
AND (SELECT COUNT(*) FROM repositories WHERE is_import_source = FALSE) > 0;
