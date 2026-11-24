package io.softwareecg.awb.repository.api;


import io.softwareecg.awb.runtime.NamedEnum;

/**
 * A enumeration that contains all authentication types that
 * are supported by Software-EKG AWB.
 * The enumeration also provides the alias names that will shown in the UI as labels.
 *
 * @since 28.07.2018
 */
public enum ResourceAuthType implements NamedEnum {

    /**
     * No authentication in use
     */
    NONE ("No Authentication"),

    /**
     * Authentication with username and password
     * via basic auth.
     */
    USERNAME_PASSWORD ("Username & Password"),

    /**
     * Static API-Key the target system uses to
     * identify the user or application.
     */
    API_KEY ("Static API Key"),

    /**
     * A temporary JsonWebToken that can stored by
     * the user to get access for a limited time.
     * It will send via HTTP headers.
     */
    JWT_BEARER_TOKEN("JSON Web Token via Header"),

    /**
     * Authentication via OAuth 2 mechanism
     */
    OAUTH2_CLIENT_CREDENTIAL("OAuth2 via 'Client Credentials' Flow"),

    /**
     * Certificate based authentication for example via
     * SSL certificates.
     */
    CERTIFICATE("Certificate-Based Authentication");

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * the alias name that represents the
     * enumeration value
     */
    private final String name;

    /**
     * Constructs a new instance of the
     * RessourceAuthType enum
     */
    ResourceAuthType(String name) {
        this.name = name;
    }

    /**
     * Return an user friendly to read alias name
     * that describes the enum value.
     *
     * @return the alias name of the enum value
     */
    @Override
    public String getName() {
        return name;
    }
}
