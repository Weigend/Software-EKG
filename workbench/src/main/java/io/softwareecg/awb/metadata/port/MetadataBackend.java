package io.softwareecg.awb.metadata.port;

/**
 * Complete metadata persistence boundary. An alternative backend implements these stores as one
 * coherent unit and owns their transaction and lifecycle semantics.
 */
public interface MetadataBackend extends AutoCloseable {
    ProjectStore projects();
    RepositoryConfigStore repositoryConfigurations();
    CollectionSourceStore collectionSources();
    DimensionAliasStore dimensionAliases();
    BookmarkStore bookmarks();

    default <T> T inTransaction(MetadataWork<T> work) {
        return work.execute(this);
    }

    @Override
    default void close() {
        // Most embedded backends are closed by their DI lifecycle.
    }

    @FunctionalInterface
    interface MetadataWork<T> {
        T execute(MetadataBackend backend);
    }
}
