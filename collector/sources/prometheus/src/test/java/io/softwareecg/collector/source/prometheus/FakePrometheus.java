package io.softwareecg.collector.source.prometheus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A stand-in for the Prometheus HTTP API. Every registered expression yields a fixed set of
 * series; the value of a series at a time is {@code seriesIndex * 1000 + seconds % 1000},
 * so tests can tell from a value which series and which time it belongs to.
 */
final class FakePrometheus implements AutoCloseable {

    /** One received request: the API path and its form parameters. */
    record Request(String path, Map<String, String> form, String authorization) {
    }

    final List<Request> requests = new CopyOnWriteArrayList<>();

    private final Map<String, List<Map<String, String>>> seriesByExpression = new HashMap<>();

    private final HttpServer server;

    /** Series (by index within their expression) that have no value before this time. */
    private final Map<String, Long> firstValueMillis = new HashMap<>();

    volatile int statusCode = 200;

    /** If set, every sample has this value instead of the one calculated from series and time. */
    volatile Double fixedValue;

    FakePrometheus() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/query", exchange -> answer(exchange, false));
        server.createContext("/api/v1/query_range", exchange -> answer(exchange, true));
        server.start();
    }

    String address() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Registers the series an expression yields; each series is given as name/value pairs of its labels. */
    FakePrometheus expression(String expression, List<Map<String, String>> series) {
        seriesByExpression.put(expression, series);
        return this;
    }

    /** Lets one series of an expression start at the given time, to simulate a late-coming system. */
    FakePrometheus startsAt(String expression, int seriesIndex, long millis) {
        firstValueMillis.put(expression + "#" + seriesIndex, millis);
        return this;
    }

    static double valueOf(int seriesIndex, long timestampMillis) {
        return seriesIndex * 1000d + (timestampMillis / 1000) % 1000;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void answer(HttpExchange exchange, boolean range) throws IOException {
        Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        requests.add(new Request(exchange.getRequestURI().getPath(), form,
                exchange.getRequestHeaders().getFirst("Authorization")));

        String body;
        int status = statusCode;
        if (status != 200) {
            body = "{\"status\":\"error\",\"errorType\":\"unavailable\",\"error\":\"simulated failure\"}";
        } else {
            body = range ? matrix(form) : vector(form);
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String vector(Map<String, String> form) {
        String expression = form.get("query");
        long now = System.currentTimeMillis();
        List<String> entries = new ArrayList<>();
        List<Map<String, String>> series = seriesByExpression.getOrDefault(expression, List.of());
        for (int i = 0; i < series.size(); i++) {
            entries.add("{\"metric\":" + labels(series.get(i)) + ",\"value\":" + sample(i, now) + "}");
        }
        return "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[" + String.join(",", entries) + "]}}";
    }

    private String matrix(Map<String, String> form) {
        String expression = form.get("query");
        long start = Math.round(Double.parseDouble(form.get("start")) * 1000);
        long end = Math.round(Double.parseDouble(form.get("end")) * 1000);
        long step = Math.round(Double.parseDouble(form.get("step")) * 1000);
        List<String> entries = new ArrayList<>();
        List<Map<String, String>> series = seriesByExpression.getOrDefault(expression, List.of());
        for (int i = 0; i < series.size(); i++) {
            long first = firstValueMillis.getOrDefault(expression + "#" + i, Long.MIN_VALUE);
            List<String> samples = new ArrayList<>();
            for (long t = start; t <= end; t += step) {
                if (t >= first) {
                    samples.add(sample(i, t));
                }
            }
            if (!samples.isEmpty()) {
                entries.add("{\"metric\":" + labels(series.get(i)) + ",\"values\":[" + String.join(",", samples) + "]}");
            }
        }
        return "{\"status\":\"success\",\"data\":{\"resultType\":\"matrix\",\"result\":[" + String.join(",", entries) + "]}}";
    }

    private String sample(int seriesIndex, long millis) {
        Double fixed = fixedValue;
        return String.format(Locale.ROOT, "[%.3f,\"%s\"]", millis / 1000d,
                fixed != null ? fixed : valueOf(seriesIndex, millis));
    }

    private static String labels(Map<String, String> labels) {
        List<String> members = new ArrayList<>();
        labels.forEach((name, value) -> members.add("\"" + name + "\":\"" + value.replace("\"", "\\\"") + "\""));
        return "{" + String.join(",", members) + "}";
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                form.put(pair.substring(0, equals), URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }
}
