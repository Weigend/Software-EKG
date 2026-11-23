package io.softwareecg.awb.metadata.port;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class MetadataBackendContractTest {

    @Test
    void backend_publishes_every_metadata_store_as_one_coherent_boundary() {
        ProjectStore projects = mock(ProjectStore.class);
        RepositoryConfigStore repositories = mock(RepositoryConfigStore.class);
        CollectionSourceStore sources = mock(CollectionSourceStore.class);
        DimensionAliasStore aliases = mock(DimensionAliasStore.class);
        BookmarkStore bookmarks = mock(BookmarkStore.class);

        MetadataBackend backend = new MetadataBackend() {
            @Override public ProjectStore projects() { return projects; }
            @Override public RepositoryConfigStore repositoryConfigurations() { return repositories; }
            @Override public CollectionSourceStore collectionSources() { return sources; }
            @Override public DimensionAliasStore dimensionAliases() { return aliases; }
            @Override public BookmarkStore bookmarks() { return bookmarks; }
        };

        assertSame(projects, backend.projects());
        assertSame(repositories, backend.repositoryConfigurations());
        assertSame(sources, backend.collectionSources());
        assertSame(aliases, backend.dimensionAliases());
        assertSame(bookmarks, backend.bookmarks());
        assertSame(backend, backend.inTransaction(value -> value));
    }
}
