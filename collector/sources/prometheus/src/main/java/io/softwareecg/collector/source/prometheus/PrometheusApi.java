package io.softwareecg.collector.source.prometheus;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The part of the Prometheus HTTP API the collector needs: evaluate an expression now
 * (instant query) or over a time range. Certificates and host names of HTTPS servers are
 * verified with the JDK defaults.
 */
public final class PrometheusApi {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

    private final String baseUrl;

    private final String authorization;

    /**
     * @param address  the address of the Prometheus server, with or without scheme
     *                 ({@code http://host:9090} or {@code host:9090})
     * @param username the user for basic authentication, or blank
     * @param password the password for basic authentication; if no user is given it is sent
     *                 as bearer token. Blank for a server without authentication.
     */
    public PrometheusApi(String address, String username, String password) {
        this.baseUrl = normalize(address);
        this.authorization = authorization(username, password);
    }

    /**
     * One time series of a query result.
     *
     * @param labels  the labels of the series, sorted by name; includes {@code __name__} if the
     *                expression keeps the metric name
     * @param samples the values, oldest first
     */
    public record Series(Map<String, String> labels, List<Sample> samples) {
    }

    /**
     * @param timestampMillis the time of the value
     * @param value           the value; {@code null} for NaN and infinite values
     */
    public record Sample(long timestampMillis, Double value) {
    }

    /**
     * Evaluates the expression at the current time.
     *
     * @return one entry per resulting series, each with exactly one sample
     * @throws PrometheusException if the server cannot be reached or rejects the query
     */
    public List<Series> query(String expression) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("query", expression);
        return toSeries(post("/api/v1/query", form));
    }

    /**
     * Evaluates the expression at every step between start and end (both inclusive).
     *
     * @throws PrometheusException if the server cannot be reached or rejects the query
     */
    public List<Series> queryRange(String expression, long startMillis, long endMillis, long stepMillis) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("query", expression);
        form.put("start", seconds(startMillis));
        form.put("end", seconds(endMillis));
        form.put("step", seconds(stepMillis));
        return toSeries(post("/api/v1/query_range", form));
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, String> form) {
        StringBuilder body = new StringBuilder();
        form.forEach((name, value) -> body.append(body.isEmpty() ? "" : "&")
                .append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8)));

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PrometheusException("Prometheus at " + baseUrl + " is not reachable: " + describe(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrometheusException("The query to Prometheus at " + baseUrl + " was interrupted.", e);
        }

        Map<String, Object> answer = null;
        try {
            if (JsonReader.parse(response.body()) instanceof Map<?, ?> map) {
                answer = (Map<String, Object>) map;
            }
        } catch (IllegalArgumentException e) {
            // not JSON: a proxy or login page answered; reported with the HTTP status below
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new PrometheusException("Prometheus at " + baseUrl + " rejected the credentials (HTTP "
                    + response.statusCode() + ").");
        }
        if (answer == null || !"success".equals(answer.get("status"))) {
            String reason = answer != null && answer.get("error") != null
                    ? String.valueOf(answer.get("error"))
                    : "HTTP " + response.statusCode();
            throw new PrometheusException("Prometheus at " + baseUrl + " did not answer the query: " + reason);
        }
        return answer;
    }

    @SuppressWarnings("unchecked")
    private static List<Series> toSeries(Map<String, Object> answer) {
        List<Series> result = new ArrayList<>();
        if (!(answer.get("data") instanceof Map<?, ?> data) || !(data.get("result") instanceof List<?> entries)) {
            return result;
        }
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> series)) {
                continue;
            }
            Map<String, String> labels = new TreeMap<>();
            if (series.get("metric") instanceof Map<?, ?> metric) {
                metric.forEach((name, value) -> labels.put(String.valueOf(name), String.valueOf(value)));
            }
            List<Sample> samples = new ArrayList<>();
            if (series.get("value") instanceof List<?> single) {          // instant query
                addSample(samples, (List<Object>) single);
            } else if (series.get("values") instanceof List<?> many) {    // range query
                for (Object sample : many) {
                    if (sample instanceof List<?> pair) {
                        addSample(samples, (List<Object>) pair);
                    }
                }
            }
            result.add(new Series(labels, samples));
        }
        return result;
    }

    private static void addSample(List<Sample> samples, List<Object> pair) {
        if (pair.size() != 2 || !(pair.get(0) instanceof Double seconds)) {
            return;
        }
        samples.add(new Sample(Math.round(seconds * 1000d), toValue(String.valueOf(pair.get(1)))));
    }

    private static Double toValue(String text) {
        try {
            double value = Double.parseDouble(text.replace("Inf", "Infinity"));
            return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String seconds(long millis) {
        return String.format(java.util.Locale.ROOT, "%.3f", millis / 1000d);
    }

    private static String normalize(String address) {
        if (address == null || address.isBlank()) {
            throw new PrometheusException("No Prometheus address configured.");
        }
        String url = address.trim();
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String authorization(String username, String password) {
        boolean hasUser = username != null && !username.isBlank();
        boolean hasPassword = password != null && !password.isBlank();
        if (hasUser) {
            String credentials = username + ":" + (hasPassword ? password : "");
            return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
        return hasPassword ? "Bearer " + password : null;
    }

    private static String describe(Throwable e) {
        return e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
    }
}
