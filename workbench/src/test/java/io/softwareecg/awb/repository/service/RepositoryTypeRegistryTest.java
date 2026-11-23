package io.softwareecg.awb.repository.service;

import io.softwareecg.awb.repository.spi.RepositoryTypeDescriptor;
import io.softwareecg.awb.repository.spi.TimeSeriesRepositoryProvider;

import io.softwareecg.awb.repository.port.TimeSeriesRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryTypeRegistryTest {

    @Test
    void exposes_the_complete_descriptor_contributed_by_a_provider() {
        RepositoryTypeDescriptor descriptor = descriptor("victoria-metrics");

        var registry = new RepositoryTypeRegistry(List.of(provider(descriptor)));

        assertEquals(descriptor, registry.require("victoria-metrics"));
        assertEquals(List.of(descriptor), registry.all());
        assertEquals("victoria-metrics", descriptor.repositoryType().name());
        assertEquals("VictoriaMetrics", descriptor.repositoryType().getName());
    }

    @Test
    void rejects_duplicate_repository_type_ids() {
        assertThrows(IllegalArgumentException.class, () -> new RepositoryTypeRegistry(List.of(
                provider(descriptor("duplicate")), provider(descriptor("duplicate")))));
    }

    @Test
    void preserves_provider_order_for_the_administration_ui() {
        RepositoryTypeDescriptor first = descriptor("first");
        RepositoryTypeDescriptor second = descriptor("second");

        var registry = new RepositoryTypeRegistry(List.of(provider(first), provider(second)));

        assertEquals(List.of(first, second), registry.all());
    }

    private static RepositoryTypeDescriptor descriptor(String id) {
        return new RepositoryTypeDescriptor(id, "VictoriaMetrics", "repository-victoria",
                true, "URL", "https://host", "Tenant", "optional", false,
                value -> value.startsWith("http"));
    }

    private static TimeSeriesRepositoryProvider provider(RepositoryTypeDescriptor descriptor) {
        return new TimeSeriesRepositoryProvider() {
            @Override public List<RepositoryTypeDescriptor> repositoryTypes() { return List.of(descriptor); }
            @Override public TimeSeriesRepository createRepositoryClient(String url, String user, String password) {
                return null;
            }
            @Override public TimeSeriesRepository createRepositoryClient(
                    String url, String repository, String user, String password) {
                return null;
            }
        };
    }
}
