package io.softwareecg.awb.metricview;

import io.softwareecg.awb.query.api.ComputationSpec;
import io.softwareecg.awb.query.api.QuerySpec;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Keeps the query identity of chart layers independently from their JavaFX representation. */
public final class InvestigationStore {

    private final Map<String, Entry> entries = new HashMap<>();

    public void put(String layerId, QuerySpec query, ComputationSpec computation) {
        entries.put(layerId, new Entry(query, computation));
    }

    public Entry get(String layerId) {
        return entries.get(layerId);
    }

    public Entry remove(String layerId) {
        return entries.remove(layerId);
    }

    public void retainOnly(Collection<String> layerIds) {
        entries.keySet().retainAll(layerIds);
    }

    public boolean contains(String layerId) {
        return entries.containsKey(layerId);
    }

    public record Entry(QuerySpec filter, ComputationSpec compute) {
    }
}
