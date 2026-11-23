package io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query;

import java.util.Map;

/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public class MatrixSeries {

    private Map<String, String> metric;

    private double[][] values;

    public Map<String, String> getMetric() {
        return metric;
    }

    public void setMetric(Map<String, String> metric) {
        this.metric = metric;
    }

    public double[][] getValues() {
        return values;
    }

    public void setValues(double[][] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "MatrixSeries{" +
                "metric=" + metric +
                ", values=" + (values.length > 0 ? "[.,.,.]" : "[]") + '}';
    }
}
