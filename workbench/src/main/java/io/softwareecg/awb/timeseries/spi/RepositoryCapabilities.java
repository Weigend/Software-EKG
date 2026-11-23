package io.softwareecg.awb.timeseries.spi;

/** Operations advertised by a repository technology. */
public record RepositoryCapabilities(boolean embedded, boolean search, boolean facets,
                                     boolean streaming, boolean administration, boolean write) {
    public static RepositoryCapabilities remoteSearchRepository() {
        return new RepositoryCapabilities(false, true, true, true, true, true);
    }

    public static RepositoryCapabilities embeddedSearchRepository() {
        return new RepositoryCapabilities(true, true, true, true, true, true);
    }
}
