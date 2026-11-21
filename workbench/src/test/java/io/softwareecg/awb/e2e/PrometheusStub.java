package io.softwareecg.awb.e2e;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Prometheus server small enough to know by heart: it answers the three requests of the HTTP
 * API the importer sends - the names of the metrics, the series of a metric, and a query for
 * the values of one series over a range - with series a test can count.
 * <p>
 * It has {@link #METRICS} for each of {@link #INSTANCES}, and {@link #POINTS} values of every
 * series: one a minute, the last one now.
 */
public final class PrometheusStub implements AutoCloseable {

    public static final List<String> METRICS = List.of("node_load1", "node_memory_MemFree_bytes");
    public static final List<String> INSTANCES = List.of("server-01", "server-02");
    public static final int POINTS = 10;

    private static final Pattern INSTANCE = Pattern.compile("instance=\"([^\"]+)\"");

    private final HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();

    public PrometheusStub() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/api/v1/label/__name__/values", exchange -> answer(exchange,
                "{\"status\":\"success\",\"data\":[" + quoted(METRICS) + "]}"));
        server.createContext("/api/v1/series", exchange -> answer(exchange, series(parameter(exchange, "match[]"))));
        server.createContext("/api/v1/query", exchange -> answer(exchange, range(parameter(exchange, "query"))));
        server.createContext("/", exchange -> {
            requests.add("UNEXPECTED " + exchange.getRequestURI());
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
    }

    /** @return the address to enter in the dialog of the importer */
    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** @return the requests the server got, in their order */
    public List<String> requests() {
        return List.copyOf(requests);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private String series(String metric) {
        String name = metric == null ? "" : metric.replaceAll("\\{.*", "");
        if (!METRICS.contains(name)) {
            return "{\"status\":\"success\",\"data\":[]}";
        }
        return "{\"status\":\"success\",\"data\":["
                + INSTANCES.stream().map(instance -> labels(name, instance)).collect(Collectors.joining(","))
                + "]}";
    }

    private String range(String query) {
        String name = query == null ? "" : query.replaceAll("\\{.*", "");
        Matcher instance = INSTANCE.matcher(query == null ? "" : query);
        if (!METRICS.contains(name) || !instance.find()) {
            return "{\"status\":\"success\",\"data\":{\"resultType\":\"matrix\",\"result\":[]}}";
        }
        long now = System.currentTimeMillis() / 1000;
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < POINTS; i++) {
            long time = now - 60L * (POINTS - 1 - i);
            values.append(i == 0 ? "" : ",")
                    .append(String.format(Locale.ROOT, "[%d,\"%.2f\"]", time, 1.0 + i * 0.25));
        }
        return "{\"status\":\"success\",\"data\":{\"resultType\":\"matrix\",\"result\":[{\"metric\":"
                + labels(name, instance.group(1)) + ",\"values\":[" + values + "]}]}}";
    }

    private static String labels(String metric, String instance) {
        return "{\"__name__\":\"" + metric + "\",\"instance\":\"" + instance + "\",\"job\":\"node\"}";
    }

    private static String quoted(List<String> names) {
        return names.stream().map(name -> "\"" + name + "\"").collect(Collectors.joining(","));
    }

    private void answer(HttpExchange exchange, String json) throws IOException {
        requests.add(exchange.getRequestURI().toString());
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String parameter(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            String key = URLDecoder.decode(equals < 0 ? pair : pair.substring(0, equals), StandardCharsets.UTF_8);
            if (name.equals(key)) {
                return equals < 0 ? "" : URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
