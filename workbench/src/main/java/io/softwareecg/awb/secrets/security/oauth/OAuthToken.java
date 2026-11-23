package io.softwareecg.awb.secrets.security.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * todo insert comment here
 *
 * @since 12.12.2018
 */
public class OAuthToken {

    /**
     * Access token.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Expiration of the access token in seconds.
     */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * Refresh token.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * Access token.
     *
     * @return Access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Access token.
     *
     * @param accessToken Access token.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Expiration of the access token in seconds.
     *
     * @return Expiration of the access token in seconds.
     */
    public int getExpiresIn() {
        return expiresIn;
    }

    /**
     * Expiration of the access token in seconds.
     *
     * @param expiresIn Expiration of the access token in seconds.
     */
    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Refresh token.
     *
     * @return Refresh token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Refresh token.
     *
     * @param refreshToken Refresh token.
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
