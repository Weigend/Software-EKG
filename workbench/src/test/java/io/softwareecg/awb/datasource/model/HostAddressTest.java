package io.softwareecg.awb.datasource.model;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HostAddressTest {

    @Test
    public void hostsAreReadAsTheUserTypesThem() {
        List<HostAddress> hosts = HostAddress.parseAll("db-01, db-02:2222\n web=10.0.0.7 ; db-01  localhost");

        assertEquals(List.of("db-01", "db-02", "web", "localhost"),
                hosts.stream().map(HostAddress::name).collect(Collectors.toList()));
        assertEquals("the port stays with the address", "db-02:2222", hosts.get(1).address());
        assertEquals("10.0.0.7", hosts.get(2).address());
        assertTrue(hosts.get(3).isLocal());
        assertFalse(hosts.get(0).isLocal());
        assertTrue(HostAddress.parseAll("  ").isEmpty());
    }

    @Test
    public void aRangeIsWrittenOut() {
        assertEquals(List.of("db-01", "db-02", "db-03"),
                HostAddress.parseAll("db-[01-03]").stream().map(HostAddress::name).collect(Collectors.toList()));
        assertEquals(List.of("node8", "node9", "node10"),
                HostAddress.parseAll("node[8-10]").stream().map(HostAddress::name).collect(Collectors.toList()));
        assertEquals("a range the wrong way round is taken as it is", List.of("x[5-1]"),
                HostAddress.parseAll("x[5-1]").stream().map(HostAddress::name).collect(Collectors.toList()));
    }
}
