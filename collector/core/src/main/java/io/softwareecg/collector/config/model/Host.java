package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A machine. In the file a host whose name is its address is just that text: {@code db-01}.
 *
 * @param name     the name the host is shown with
 * @param address  its address: a host name, an IP address or a URL
 * @param username the login, if it differs from the one of the group; or {@code null}
 * @param password its password, see {@link HostGroup#password()}; or {@code null}
 */
public record Host(String name, String address, String username, String password) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Host(@JsonProperty("name") String name,
                @JsonProperty("address") String address,
                @JsonProperty("username") String username,
                @JsonProperty("password") String password) {
        // one of the two is enough
        this.name = name == null ? address : name;
        this.address = address == null ? name : address;
        this.username = username;
        this.password = password;
    }

    /**
     * A host that is called by its address.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Host of(String address) {
        return new Host(address, address, null, null);
    }

    @JsonValue
    Object written() {
        if (username == null && password == null && name != null && name.equals(address)) {
            return name;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("address", address);
        if (username != null) {
            map.put("username", username);
        }
        if (password != null) {
            map.put("password", password);
        }
        return map;
    }
}
