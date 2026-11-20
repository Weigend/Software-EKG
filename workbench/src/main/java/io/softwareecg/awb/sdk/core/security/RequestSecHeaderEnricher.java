package io.softwareecg.awb.sdk.core.security;

import io.softwareecg.awb.sdk.awbapi.repository.ResourceAuthType;
import io.softwareecg.awb.sdk.core.security.oauth.OpenIdConnectClientCredentialFlow;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;
import org.apache.http.message.BasicHeader;

import java.util.Base64;

/**
 *
 *
 * @since 12.12.2018
 */
public class RequestSecHeaderEnricher {

    private final SecurityContext securityContext;

    private OpenIdConnectClientCredentialFlow clientCredentialFlow;

    public RequestSecHeaderEnricher(SecurityContext context) {
        this.securityContext = context;
        if (securityContext.getResourceAuthType() == ResourceAuthType.OAUTH2_CLIENT_CREDENTIAL) {
            clientCredentialFlow = new OpenIdConnectClientCredentialFlow(securityContext.getSsoOAuthServer(),
                    securityContext.getUserName(), securityContext.getPassword());
        }
    }

    public void configureRequest(HttpMessage request) {

        if (securityContext.getResourceAuthType() == ResourceAuthType.JWT_BEARER_TOKEN) {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + securityContext.getOAuthToken().getAccessToken());

        } else if (securityContext.getResourceAuthType() == ResourceAuthType.USERNAME_PASSWORD) {

            String basicAuthUsername = securityContext.getUserName();
            String basicAuthPassword = securityContext.getPassword();

            String authToken = basicAuthUsername + ":" + (basicAuthPassword != null ? basicAuthPassword : "");

            request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic "
                    +  Base64.getEncoder().encodeToString(authToken.getBytes())));

        } else if (securityContext.getResourceAuthType() == ResourceAuthType.OAUTH2_CLIENT_CREDENTIAL) {
            if (!clientCredentialFlow.isAuthenticated()) {
                clientCredentialFlow.authenticate();
            }

            clientCredentialFlow.decorate(request);
        }
     }
}
