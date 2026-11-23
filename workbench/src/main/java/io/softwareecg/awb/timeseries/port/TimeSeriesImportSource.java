package io.softwareecg.awb.timeseries.port;

import io.softwareecg.awb.sdk.awbapi.repository.ResourceAuthType;

import java.util.Objects;

/**
 * The importer source types definition that
 * contains all properties that used to access the
 * remote data.
 *
 * @since 29.07.2018
 */
public class TimeSeriesImportSource {

    private String id;

    private String repositoryUrl;

    private ResourceAuthType authType;

    private String oauthServerUrl;

    private String userName;

    private String password;

    private String accordingProjectName;

    private String ssoAccessToken;

    private String ssoRefreshToken;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccordingProjectName() {
        return accordingProjectName;
    }

    public void setAccordingProjectName(String accordingProjectName) {
        this.accordingProjectName = accordingProjectName;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public ResourceAuthType getAuthType() {
        return authType;
    }

    public void setAuthType(ResourceAuthType authType) {
        this.authType = authType;
    }

    public void setOAuthServerUrl(String oauthServerUrl) {
        this.oauthServerUrl = oauthServerUrl;
    }

    public String getOAuthServerUrl() {
        return oauthServerUrl;
    }

    public String getSsoAccessToken() {
        return ssoAccessToken;
    }

    public void setSsoAccessToken(String ssoAccessToken) {
        this.ssoAccessToken = ssoAccessToken;
    }

    public String getSsoRefreshToken() {
        return ssoRefreshToken;
    }

    public void setSsoRefreshToken(String ssoRefreshToken) {
        this.ssoRefreshToken = ssoRefreshToken;
    }

    public String getUsername() {
        return userName;
    }

    public void setUsername(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeSeriesImportSource that)) {
            return false;
        }
        return Objects.equals(id, that.id) &&
                Objects.equals(repositoryUrl, that.repositoryUrl) &&
                authType == that.authType &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password) &&
                Objects.equals(accordingProjectName, that.accordingProjectName) &&
                Objects.equals(oauthServerUrl, that.oauthServerUrl) &&
                Objects.equals(ssoAccessToken, that.ssoAccessToken) &&
                Objects.equals(ssoRefreshToken, that.ssoRefreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, repositoryUrl, authType, userName, password,
                accordingProjectName, oauthServerUrl, ssoRefreshToken, ssoAccessToken);
    }
}
