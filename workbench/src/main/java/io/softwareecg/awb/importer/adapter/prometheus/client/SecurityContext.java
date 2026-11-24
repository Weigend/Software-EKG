package io.softwareecg.awb.importer.adapter.prometheus.client;

import io.softwareecg.awb.repository.api.ResourceAuthType;

/**
 * A bean that contains security information for a specific resource
 * for example an remote database.
 *
 * @since 12.12.2018
 */
@SuppressWarnings("unused") // this class is part of the framework,
// not all of it's method need to be in use at the moment
public class SecurityContext {

    private ResourceAuthType resourceAuthType = ResourceAuthType.NONE;

    private String userName;

    private String password;

    private String ssoOAuthServer;

    private OAuthToken oAuthToken;

    public ResourceAuthType getResourceAuthType() {
        return resourceAuthType;
    }

    public void setResourceAuthType(ResourceAuthType resourceAuthType) {
        this.resourceAuthType = resourceAuthType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSsoOAuthServer() {
        return ssoOAuthServer;
    }

    public void setSsoOAuthServer(String ssoOAuthServer) {
        this.ssoOAuthServer = ssoOAuthServer;
    }

    public OAuthToken getOAuthToken() {
        return oAuthToken;
    }

    public void setOAuthToken(OAuthToken oAuthToken) {
        this.oAuthToken = oAuthToken;
    }
}
