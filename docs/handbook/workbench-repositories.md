# Add a Workbench time-series repository

[Handbook home](README.md) · [Extensions](extensions.md)

Implement `TimeSeriesRepositoryProvider`. Its `repositoryTypes()` method is the complete extension
descriptor used by repository administration: stable ID, labels and prompts, icon, validation,
selectability and `RepositoryCapabilities`. `RepositoryTypeRegistry` rejects duplicate IDs. The UI
therefore contains no Solr/Elasticsearch/ClickHouse switch and must not import adapter classes.

1. Put the protocol client below `timeseries.adapter.<technology>` and implement the neutral
   `TimeSeriesRepository` port.
2. Implement `TimeSeriesRepositoryProvider#createRepositoryClient` for every advertised form.
3. Return one or more `RepositoryTypeDescriptor` values from `repositoryTypes()`.
4. Register the provider with the existing lookup/injection mechanism.
5. Test descriptor discovery, connection validation, capabilities and a read/write lifecycle.

`EkgRepositoryDbType` is an open persisted identity, not an enum. Calling
`descriptor.repositoryType()` defines the provider's ID for existing repository services and
factories, so adding a technology does not require changing a core type list.

Descriptor validation is for immediate form feedback. The provider remains responsible for
authoritative connection errors, authentication, timeout and cleanup. Repository IDs are persisted
and require migration if changed.
