package io.softwareecg.awb.datasource.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.softwareecg.wfx.lookup.api.Lookup;

/** Validated registry of the Analysis Source providers used by New Analysis. */
public final class AnalysisSourceRegistry {
    private final Map<String, AnalysisSourceProvider> providers;

    public AnalysisSourceRegistry(Collection<? extends AnalysisSourceProvider> providers) {
        Map<String, AnalysisSourceProvider> indexed = new LinkedHashMap<>();
        for (AnalysisSourceProvider provider : providers) {
            String id = provider.definition().id();
            if (indexed.putIfAbsent(id, provider) != null) {
                throw new IllegalArgumentException("Duplicate analysis source id: " + id);
            }
        }
        this.providers = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }

    public List<AnalysisSourceOption> options(AnalysisTopic topic) {
        List<AnalysisSourceOption> result = new ArrayList<>();
        for (AnalysisSourceProvider provider : providers.values()) {
            AnalysisSourceOption option = provider.topics().get(topic);
            if (option != null) {
                result.add(option);
            }
        }
        return List.copyOf(result);
    }

    public AnalysisSourceProvider require(String id) {
        AnalysisSourceProvider provider = providers.get(id);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown analysis source id: " + id);
        }
        return provider;
    }

    public List<AnalysisSourceDefinition> definitions() {
        return providers.values().stream().map(AnalysisSourceProvider::definition).toList();
    }

    public static AnalysisSourceRegistry standard() {
        List<AnalysisSourceProvider> providers = new ArrayList<>(List.of(
                BuiltinAnalysisSourceProviders.prometheus(),
                BuiltinAnalysisSourceProviders.sshTop(),
                BuiltinAnalysisSourceProviders.windowsCounters(),
                BuiltinAnalysisSourceProviders.jolokia(),
                BuiltinAnalysisSourceProviders.jmx(),
                BuiltinAnalysisSourceProviders.postgres()));
        providers.addAll(Lookup.lookupAll(AnalysisSourceProvider.class));
        return new AnalysisSourceRegistry(providers);
    }
}
