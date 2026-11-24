package io.softwareecg.awb.metadata.api;

/**
 * Data Transfer Object fuer eine Zeile in der Repository-Config-Tabelle.
 * Wird von DA-Adaptern befuellt und vom Repository-Layer konsumiert.
 *
 * Bewusst public fields ohne Getter/Setter - reines Datenobjekt.
 */
public class RepositoryConfigRow {
    public String id;
    public String name;
    public String url;
    public String dbIndexName;
    public String dbType;
    public String authType;
    public String username;
    public String password;
    public String oauthServerUrl;
    public String ssoRefreshToken;
    public boolean isImportSource;
    public String accordingProjectName;
}
