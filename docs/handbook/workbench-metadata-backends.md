# Replace the Workbench metadata backend

[Handbook home](README.md) · [Extensions](extensions.md)

Metadata is everything the Workbench remembers besides the time series: projects, repository
configurations, the data sources of an analysis, dimension aliases and bookmarks. It is reached
only through five store interfaces in `io.softwareecg.awb.metadata.port`:

| Store | Holds |
| --- | --- |
| `ProjectStore` | projects |
| `RepositoryConfigStore` | configured repositories |
| `CollectionSourceStore` | data sources of an analysis and their history |
| `DimensionAliasStore` | alias names of dimensions per project |
| `BookmarkStore` | bookmark groups and bookmarks |

H2 is the one implementation today, in `metadata.adapter.h2`. `LayerArchitectureTest` keeps every
other package from importing it.

## Steps

1. **Implement all five stores** in `metadata.adapter.<database>` and map the rows of the database
   to the records in `metadata.model`. A backend that implements only some of them is not
   usable: the features expect one place for all metadata.
2. **Own the schema.** The H2 adapter creates its tables from `schema.sql` on the classpath when
   the first connection opens, in `~/.ekg/metadata`. A new adapter brings its own schema,
   migrations, and backup and restore.
3. **Bind the new stores.** Each H2 store is bound by its `@Singleton` annotation, and there is no
   setting that chooses a backend. Every store interface must end up with exactly one bound
   implementation: annotate the new stores with `@Singleton` and take the annotation off the H2
   ones, or mark the new ones `@Primary`. Do not add a backend choice to a feature service.
4. **Carry existing data over.** Users have projects and bookmarks in the H2 file. Persisted
   identities, such as project names, repository IDs and data source IDs, must survive the move.
5. **Test it.** Add tests for migration, for access from several threads and for a restart of the
   Workbench against the real database.

## What to know

- **No transaction spans several stores.** Each store call is atomic by itself, as in the H2
  adapter. No feature needs more today.
- **Secrets are not metadata.** Stores hold a reference to a password or token, never the value.
  The value is in the secret store; see [Replace the secret store](workbench-secret-stores.md).
- **The H2 browser is tied to H2.** `tools.h2browser.H2BrowserApp` is a stand-alone tool that
  opens the H2 file directly. It is of no use with another database.
