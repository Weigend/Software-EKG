# Add a Workbench time-series repository

[Handbook home](README.md) · [Extensions](extensions.md)

Everything a repository technology consists of is in one package,
`io.softwareecg.awb.repository.adapter.<technology>`. Solr, Elasticsearch and ClickHouse are each
built this way; Elasticsearch is the smallest one to read. There are five parts:

| Part | Contract |
| --- | --- |
| The client that talks to the database | `TimeSeriesRepository` in `repository.persistence` |
| The factory for clients, and the description of the type | `TimeSeriesRepositoryProvider` in `repository.connection` |
| A qualifier that names the technology for injection | an annotation like `@ElasticSearch` |
| The repository the rest of the Workbench works with | `RepositoryAdapterBase` in `repository.adapter` |
| The factory that turns a stored configuration into that repository | `EkgRepository.Factory` in `repository.connection` |

The provider alone makes the type appear in repository administration. Only with the repository
and its factory can a repository of that type be created and opened.

## Steps

1. **Write the client.** Implement `TimeSeriesRepository`: search, export, facets, adding
   and deleting entities, commit and close. It must not use JavaFX or any UI class.
2. **Add a qualifier.** Copy `ElasticSearch.java` under the name of the technology.
3. **Write the provider.** A `@Singleton` class with the qualifier that implements
   `TimeSeriesRepositoryProvider`:
   - `createRepositoryClient(url, user, password)` is the form `RepositoryAdapterBase` calls.
     The URL it receives is the connection URL with the index name appended after a slash.
   - `createRepositoryClient(clusterManagerUrl, repositoryName, user, password)` keeps address
     and collection or table apart. A repository that needs this form overrides
     `getRepositoryClient()` and calls it, as `SolrCloudRepository` and
     `ClickHouseCloudRepository` do.
   - `repositoryTypes()` returns one `RepositoryTypeDescriptor` per type; see below.
4. **Write the repository and its factory:**

   ```java
   public class ApplianceRepository extends RepositoryAdapterBase {

       private final TimeSeriesRepositoryProvider clients;

       // constructor that sets id, repositoryName, connectionUrl, dbIndexName,
       // ekgRepositoryDbType, repositoryAuthType, username, password and clients

       @Override
       protected TimeSeriesRepositoryProvider getRepositoryClientFactory() {
           return clients;
       }

       @Singleton
       public static class Factory implements EkgRepository.Factory {

           private final TimeSeriesRepositoryProvider clients;

           @Inject
           public Factory(@External @Appliance TimeSeriesRepositoryProvider clients) {
               this.clients = clients;
           }

           @Override
           public EkgRepository getInstance(String id, String name, String connectionUrl,
                   String dbIndexName, EkgRepositoryDbType type, ResourceAuthType authType,
                   String username, String password) {
               return new ApplianceRepository(id, name, connectionUrl, dbIndexName, type,
                       authType, username, password, clients);
           }

           @Override
           public boolean isTypeSupported(EkgRepositoryDbType type) {
               return type.name().equals("APPLIANCE");
           }
       }
   }
   ```

   `RepositoryMapper` asks every `EkgRepository.Factory` whether it supports the stored type and
   uses the first that does. A type no factory supports cannot be opened. A factory whose
   technology needs an index name where the user entered none overrides `storedIndexName`, as
   the one of ClickHouse does.
5. **Test it.** Cover that the descriptor is found, the connection validation, and one cycle of
   writing, reading and deleting against the real database. `RepositoryTypeRegistryTest` shows
   the registry part.

## The descriptor

`RepositoryTypeDescriptor` is everything repository administration shows and checks for a type:

- **`id`** is the stored identity. It is also what `isTypeSupported` compares. Never change it
  without a migration.
- **`displayName`** and **`icon`** appear in the type list. The icon is an SVG resource, named
  relative to `io/softwareecg/awb/repository/ui/admin`, for example `icons/repository-db-type-solr.svg`.
- **`selectable`** is false for a type the user cannot create, as the embedded Solr.
- **Labels and prompts** for the connection field and the index field.
- **`indexDisabled`** is true when the type has no index name of its own.
- **`connectionValidator`** gives immediate feedback in the form. The provider remains
  responsible for the real errors: connection, authentication, timeout and cleanup.

`EkgRepositoryDbType` in `repository.connection` is an open identity, not an enum. `descriptor.repositoryType()` defines the
ID, so no central list of types has to be extended. `RepositoryTypeRegistry` rejects two types with
the same ID.

## What still lives outside the adapters

One rule for existing technologies is in shared code. A new technology does not have to touch it,
but one with the same need has to be added there: `EkgRepositoryAdminController` keeps a trailing
slash of the URL for Elasticsearch only, and preselects Solr (Classic) in the form.

`LayerArchitectureTest` keeps the repository UI from importing an adapter class, the technologies
from depending on each other, and the other components from using anything but `repository.api`.
