package io.softwareecg.awb.timeseries.spi;

import io.softwareecg.awb.runtime.NamedEnum;
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

    public boolean isConnectionValid(String connection) {
        return connection != null && connectionValidator.test(connection);
    }

    /** Open, persisted identity passed to repository factories and services. */
    public EkgRepositoryDbType repositoryType() {
        return EkgRepositoryDbType.define(id, displayName);
    }

    @Override public String getName() { return displayName; }
}
