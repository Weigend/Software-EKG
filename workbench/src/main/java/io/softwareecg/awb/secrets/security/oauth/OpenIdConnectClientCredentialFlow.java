package io.softwareecg.awb.secrets.security.oauth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * todo insert comment here
 *
 * @since 12.12.2018
 */
public class OpenIdConnectClientCredentialFlow {

    private static final Logger LOGGER = EkgLogger.get();

    private final ObjectMapper objectMapper;

    private final String oauthServerUrl;

    /**
     * Client id.
     */
    private String clientId;

    /**
     * Client secret.
     */
    private String clientSecret;

    private final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    /**
     * The current oauth token.
     */
    private OAuthToken accessToken;

    /**
     * The time stamp until the {@link #accessToken} is valid.
     */
    private Instant tokenExpirationTime;

    /**
     * Initializes a new decorator instance.
     *
     * @param clientId     The client id to get the access token.
     * @param clientSecret The client secret to get the access token.
     */
    public OpenIdConnectClientCredentialFlow(String oauthServerUrl, String clientId, String clientSecret) {
        requireNonNull(oauthServerUrl, "oauthServerUrl must not be null.");
        requireNonNull(clientId, "clientId must not be null.");
        requireNonNull(clientSecret, "clientSecret must not be null.");
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.oauthServerUrl = oauthServerUrl;

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Decorate a given HTTP request with additional information. Used on every request performed by the webservice.
     *
     * @param request The request to be modified.
     */
    public void decorate(HttpMessage request) {

        if (!checkAndRefreshIfNeeded()) {
            return;
        }
        request.addHeader("Authorization", "Bearer " + accessToken.getAccessToken());
    }

    /**
     *
     */
    public void authenticate() {
        useAccessToken(getToken());
    }

    public void deauthenticate() {
        accessToken = null;
        tokenExpirationTime = null;
        clientId = null;
        clientSecret = null;
    }

    /**
     * Check whether this decorator, and by extension its webservice, is authenticated against the server.
     *
     * @return true if the decorator considers itself authenticated.
     */
    public boolean isAuthenticated() {
        return accessToken != null && isNotExpired();
    }

    /**
     * Uses the given access token.
     *
     * @param token the token to use.
     */
    private void useAccessToken(OAuthToken token) {
        requireNonNull(token, "token must not be null.");

        tokenExpirationTime = Instant.now().plusSeconds(token.getExpiresIn());
        accessToken = token;
        LOGGER.info("Uses new access token {} valid until {}", accessToken.getAccessToken(), tokenExpirationTime);
    }

    /**
     * Get a new access token from user and passwords.
     *
     * @return The oauth token
     */
    private OAuthToken getToken() {

       LOGGER.info("Perform authenticate and retrieve auth token request.");

       return performTokenRequest(
                List.of(
                        new BasicNameValuePair("grant_type", "client_credentials"),
                        new BasicNameValuePair("client_id", clientId),
                        new BasicNameValuePair("client_secret", clientSecret)
                )
        );
    }

    /**
     * Check if the access token exists and is not expired. If it is expired, it will be refreshed.
     *
     * @return true if the token valid or successfully refreshed. otherwise false.
     */
    private boolean checkAndRefreshIfNeeded() {
        if (accessToken == null) {
            return false;
        }

        if (isNotExpired()) {
            return true;
        }

        LOGGER.info("Token expired. Attempt to refresh the token.");
        useAccessToken(refreshOAuthToken(accessToken));

        return true;
    }

    private boolean isNotExpired() {
        Instant now = Instant.now();
        return tokenExpirationTime == null || tokenExpirationTime.isAfter(now);
    }

    /**
     * Refreshes the access token based on a previous requested access token.
     *
     * @param oldToken the old access token
     * @return the new oauth token.
     */
    private OAuthToken refreshOAuthToken(OAuthToken oldToken) {

        LOGGER.info("Perform refresh token request.");

        return performTokenRequest(
                List.of(
                        new BasicNameValuePair("grant_type", "refresh_token"),
                        new BasicNameValuePair("refresh_token", oldToken.getRefreshToken())
                )
        );
    }

    /**
     * Performs the actual request to get the access token.
     *
     * @param formRequestFields
     * @return The parsed oauth access token.
     */
    private OAuthToken performTokenRequest(List<NameValuePair> formRequestFields) {
        try {
            HttpPost tokenRequest = new HttpPost(oauthServerUrl);
            tokenRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");
            tokenRequest.setEntity(new UrlEncodedFormEntity(formRequestFields));

            CloseableHttpResponse response = httpClient.execute(tokenRequest);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                throw new IllegalStateException("Request an new OAuth access token result in the following error: "
                        + response.getEntity().getContent());

            } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IllegalStateException("Unable to authenticate client. Server response with "
                        + response.getStatusLine().getReasonPhrase());
            }

            return objectMapper.readerFor(OAuthToken.class).readValue(response.getEntity().getContent()) ;

        } catch (Exception e) {
            throw new IllegalStateException("Unable to request access token.", e);
        }
    }
}