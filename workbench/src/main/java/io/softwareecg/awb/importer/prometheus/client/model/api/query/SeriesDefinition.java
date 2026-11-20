package io.softwareecg.awb.importer.prometheus.client.model.api.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public class SeriesDefinition extends HashMap<String, String> {

    public String getMetricName() {
        return get("__name__");
    }

    public String getLabelValue(String labelName) {
        return get(labelName);
    }

    public Collection<String> getLabels() {
        HashSet<String> labels = new HashSet<>(keySet());
        labels.remove("__name__");
        return labels;
    }
}
