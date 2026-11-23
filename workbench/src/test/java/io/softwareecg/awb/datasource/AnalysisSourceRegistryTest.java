package io.softwareecg.awb.datasource;

import io.softwareecg.awb.datasource.spi.AnalysisSourceDefinition;
import io.softwareecg.awb.datasource.spi.AnalysisSourceOption;
import io.softwareecg.awb.datasource.spi.AnalysisSourceProvider;
import io.softwareecg.awb.datasource.spi.AnalysisTopic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisSourceRegistryTest {

    @Test
    void exposes_a_contributed_source_without_changing_the_topic_or_dialog() {
        var source = new AnalysisSourceDefinition("oracle-awr", "Oracle AWR", true, true);
        AnalysisSourceProvider provider = provider(source, AnalysisTopic.DATABASES, "Database snapshots");

        var options = new AnalysisSourceRegistry(List.of(provider)).options(AnalysisTopic.DATABASES);

        assertEquals(List.of(new AnalysisSourceOption(source, "Database snapshots")), options);
    }

    @Test
    void rejects_duplicate_stable_source_ids() {
        var source = new AnalysisSourceDefinition("duplicate", "First", false, true);

        assertThrows(IllegalArgumentException.class, () -> new AnalysisSourceRegistry(List.of(
                provider(source, AnalysisTopic.LINUX, "one"),
                provider(new AnalysisSourceDefinition("duplicate", "Second", false, true),
                        AnalysisTopic.LINUX, "two"))));
    }

    @Test
    void preserves_provider_order_for_default_selection_and_wizard_steps() {
        var prometheus = new AnalysisSourceDefinition("prometheus-test", "Prometheus", true, true);
        var direct = new AnalysisSourceDefinition("direct-test", "Direct", false, true);

        var registry = new AnalysisSourceRegistry(List.of(
                provider(prometheus, AnalysisTopic.LINUX, "usual"),
                provider(direct, AnalysisTopic.LINUX, "alternative")));
        var options = registry.options(AnalysisTopic.LINUX);

        assertEquals(List.of(prometheus, direct), options.stream().map(AnalysisSourceOption::source).toList());
        assertEquals(List.of(prometheus, direct), registry.definitions());
    }

    private static AnalysisSourceProvider provider(AnalysisSourceDefinition source,
                                                   AnalysisTopic topic,
                                                   String description) {
        return new AnalysisSourceProvider() {
            @Override public AnalysisSourceDefinition definition() { return source; }
            @Override public Map<AnalysisTopic, AnalysisSourceOption> topics() {
                return Map.of(topic, new AnalysisSourceOption(source, description));
            }
        };
    }
}
