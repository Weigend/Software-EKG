package io.softwareecg.collector.source.prometheus;

/**
 * A Prometheus server could not be reached or did not answer a query. The message is written
 * for the person who configured the source.
 */
public class PrometheusException extends RuntimeException {

    public PrometheusException(String message) {
        super(message);
    }

    public PrometheusException(String message, Throwable cause) {
        super(message, cause);
    }
}
