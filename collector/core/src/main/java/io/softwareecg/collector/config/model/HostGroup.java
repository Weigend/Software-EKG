package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of machines that are collected alike.
 *
 * @param username the login for all hosts of the group that name none of their own, or {@code null}
 * @param password its password - as a reference to the environment, <code>${ENV:NAME}</code>,
 *                 not in clear text; or {@code null}
 * @param hosts    the machines
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"username", "password", "hosts"})
public record HostGroup(String username, String password, List<Host> hosts) {

    public HostGroup {
        hosts = hosts == null ? List.of() : new ArrayList<>(hosts);
    }

    /**
     * @return the login of the host: its own, or else the one of the group
     */
    public String usernameOf(Host host) {
        return CollectorConfig.isBlank(host.username()) ? username : host.username();
    }

    /**
     * @return the password of the host: its own, or else the one of the group
     */
    public String passwordOf(Host host) {
        return CollectorConfig.isBlank(host.password()) ? password : host.password();
    }
}
