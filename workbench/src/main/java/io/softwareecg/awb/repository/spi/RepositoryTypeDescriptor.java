package io.softwareecg.awb.repository.spi;

import io.softwareecg.awb.runtime.NamedEnum;
import java.net.URI;
import java.util.Objects;
import java.util.function.Predicate;

/** UI-neutral description and validation rules of one repository type. */
public record RepositoryTypeDescriptor(
        String id,
        String displayName,
        String icon,
        boolean selectable,
        String connectionLabel,
        String connectionPrompt,
        String indexLabel,
        String indexPrompt,
        boolean indexDisabled,
        Predicate<String> connectionValidator) implements NamedEnum {

    public RepositoryTypeDescriptor {
        Objects.requireNonNull(id);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(icon);
        Objects.requireNonNull(connectionValidator);
    }

    /**
     * For technologies reached by a URL of their server.
     *
     * @return whether the connection is an absolute http or https URL
     */
    public static boolean isHttpUrl(String connection) {
        try {
            URI uri = URI.create(connection);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isConnectionValid(String connection) {
        return connection != null && connectionValidator.test(connection);
    }

    /** Open, persisted identity passed to repository factories and services. */
    public EkgRepositoryDbType repositoryType() {
        return EkgRepositoryDbType.define(id, displayName);
    }

    @Override public String getName() { return displayName; }
}
