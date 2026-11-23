package io.softwareecg.awb.metadata.adapter.h2;

import io.softwareecg.awb.metadata.port.BookmarkStore;
import io.softwareecg.awb.metadata.port.CollectionSourceStore;
import io.softwareecg.awb.metadata.port.DimensionAliasStore;
import io.softwareecg.awb.metadata.port.MetadataBackend;
import io.softwareecg.awb.metadata.port.ProjectStore;
import io.softwareecg.awb.metadata.port.RepositoryConfigStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** H2 composition of all metadata stores. */
@Singleton
public final class H2MetadataBackend implements MetadataBackend {
    private final H2ProjectStore projects;
    private final H2RepositoryConfigStore repositoryConfigurations;
    private final H2CollectionSourceStore collectionSources;
    private final H2DimensionAliasStore dimensionAliases;
    private final H2BookmarkStore bookmarks;
    private final H2MetadataDataSource dataSource;

    @Inject
    public H2MetadataBackend(H2MetadataDataSource dataSource,
                             H2ProjectStore projects,
                             H2RepositoryConfigStore repositoryConfigurations,
                             H2CollectionSourceStore collectionSources,
                             H2DimensionAliasStore dimensionAliases,
                             H2BookmarkStore bookmarks) {
        this.dataSource = dataSource;
        this.projects = projects;
        this.repositoryConfigurations = repositoryConfigurations;
        this.collectionSources = collectionSources;
        this.dimensionAliases = dimensionAliases;
        this.bookmarks = bookmarks;
    }

    @Override public ProjectStore projects() { return projects; }
    @Override public RepositoryConfigStore repositoryConfigurations() { return repositoryConfigurations; }
    @Override public CollectionSourceStore collectionSources() { return collectionSources; }
    @Override public DimensionAliasStore dimensionAliases() { return dimensionAliases; }
    @Override public BookmarkStore bookmarks() { return bookmarks; }

    @Override
    public <T> T inTransaction(MetadataWork<T> work) {
        return dataSource.inTransaction(() -> work.execute(this));
    }
}
