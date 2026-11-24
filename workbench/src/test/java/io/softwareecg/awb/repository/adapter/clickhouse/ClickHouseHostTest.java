package io.softwareecg.awb.repository.adapter.clickhouse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

class ClickHouseHostTest {

    @Test
    void aHostThatListensIsReachable() throws IOException {
        int port;
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            port = server.getLocalPort();
            assertThat(ClickHouseHost.isReachable("jdbc:ch://127.0.0.1:" + port)).isTrue();
            assertThat(ClickHouseHost.isReachable("jdbc:clickhouse://127.0.0.1:" + port + "/ekg?ssl=false")).isTrue();
            assertThat(ClickHouseHost.isReachable("jdbc:ch://user:pass@127.0.0.1:" + port + "/ekg")).as("with credentials").isTrue();
            assertThat(ClickHouseHost.isReachable("jdbc:ch://127.0.0.1:" + port + ",127.0.0.1:1/ekg")).as("the first of two").isTrue();
        }
        assertThat(ClickHouseHost.isReachable("jdbc:ch://127.0.0.1:" + port)).as("after it stopped").isFalse();
    }

    @Test
    void withoutAHostNothingIsReachable() {
        assertThat(ClickHouseHost.isReachable(null)).isFalse();
        assertThat(ClickHouseHost.isReachable("jdbc:ch:///ekg")).isFalse();
    }
}
