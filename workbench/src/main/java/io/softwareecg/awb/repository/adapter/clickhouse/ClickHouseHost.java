package io.softwareecg.awb.repository.adapter.clickhouse;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * The host of a ClickHouse server, as a JDBC URL names it.
 */
final class ClickHouseHost {

    private static final int DEFAULT_HTTP_PORT = 8123;
    /** Sehr kurzer Timeout: damit ein nicht erreichbarer Server den Ping-Thread nicht
     *  blockiert. ClickHouse antwortet auf TCP-SYN typisch innerhalb weniger ms. */
    private static final int REACHABILITY_PROBE_TIMEOUT_MS = 1_000;

    private ClickHouseHost() {
    }

    /**
     * Leichtgewichtiger TCP-Erreichbarkeits-Check: parst Host und Port aus der JDBC-URL
     * und versucht einen Socket-Connect mit kurzem Timeout. Loggt nichts, wirft nichts -
     * komplettes Schweigen ist hier wertvoll, weil das Ergebnis vom State-Tracking-Layer
     * weiterverarbeitet wird.
     *
     * Multi-Host-URLs (jdbc:ch://h1:8123,h2:8123/db) werden mit dem ersten Host gepruft -
     * fuer den UI-Heartbeat reicht das.
     */
    static boolean isReachable(String jdbcUrl) {
        HostPort hostPort = extractFirstHostPort(jdbcUrl);
        if (hostPort == null) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostPort.host, hostPort.port), REACHABILITY_PROBE_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static HostPort extractFirstHostPort(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        String url = jdbcUrl;
        String[] schemes = {"jdbc:ch://", "jdbc:clickhouse://"};
        for (String s : schemes) {
            if (url.startsWith(s)) {
                url = url.substring(s.length());
                break;
            }
        }
        // Cut off database/path and query string
        int slash = url.indexOf('/');
        if (slash >= 0) url = url.substring(0, slash);
        int qmark = url.indexOf('?');
        if (qmark >= 0) url = url.substring(0, qmark);
        // First host of a comma-separated multi-host list
        int comma = url.indexOf(',');
        String first = comma >= 0 ? url.substring(0, comma) : url;
        // Optional credentials prefix (user:pass@host:port) — strip
        int at = first.indexOf('@');
        if (at >= 0) first = first.substring(at + 1);
        int colon = first.lastIndexOf(':');
        String host;
        int port = DEFAULT_HTTP_PORT;
        if (colon > 0) {
            host = first.substring(0, colon);
            try {
                port = Integer.parseInt(first.substring(colon + 1));
            } catch (NumberFormatException ignore) {
                // fall back to default port
            }
        } else {
            host = first;
        }
        if (host.isEmpty()) {
            return null;
        }
        return new HostPort(host, port);
    }

    private static final class HostPort {
        final String host;
        final int port;
        HostPort(String host, int port) { this.host = host; this.port = port; }
    }
}
