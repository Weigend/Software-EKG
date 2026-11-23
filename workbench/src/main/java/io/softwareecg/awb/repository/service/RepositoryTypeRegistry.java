package io.softwareecg.awb.repository.service;

import io.softwareecg.awb.repository.spi.RepositoryTypeDescriptor;
import io.softwareecg.awb.repository.spi.TimeSeriesRepositoryProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validated view of all repository types contributed by repository providers. */
public final class RepositoryTypeRegistry {
    private final Map<String, RepositoryTypeDescriptor> descriptors;

    public RepositoryTypeRegistry(Collection<? extends TimeSeriesRepositoryProvider> providers) {
        Map<String, RepositoryTypeDescriptor> indexed = new LinkedHashMap<>();
        for (TimeSeriesRepositoryProvider provider : providers) {
            for (RepositoryTypeDescriptor descriptor : provider.repositoryTypes()) {
                if (indexed.putIfAbsent(descriptor.id(), descriptor) != null) {
                    throw new IllegalArgumentException("Duplicate repository type id: " + descriptor.id());
                }
            }
        }
        descriptors = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }

    public RepositoryTypeDescriptor require(String id) {
        RepositoryTypeDescriptor descriptor = descriptors.get(id);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown repository type id: " + id);
        }
        return descriptor;
    }

    public List<RepositoryTypeDescriptor> all() {
        return List.copyOf(descriptors.values());
    }
}
