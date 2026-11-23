# Replace the Workbench metadata backend

[Handbook home](README.md) · [Extensions](extensions.md)

Workbench metadata is exposed only through `io.softwareecg.awb.metadata.port`. A backend implements
`MetadataBackend` and supplies project, repository-configuration, collection-source,
dimension-alias and bookmark stores as one coherent unit. H2 is the adapter in
`metadata.adapter.h2`; feature code must not import it.

1. Implement every store interface in `metadata.port` and map backend rows to the records in
   `metadata.model`.
2. Compose them in one `MetadataBackend` implementation and define transaction, concurrency and
   close semantics. `inTransaction` is the atomic unit; H2 implements it with one thread-bound
   JDBC connection shared by all stores, including commit and rollback.
3. Bind the new implementations at the composition root; do not add backend selection to feature
   services.
4. Run the backend contract tests, then add migration, rollback, concurrent access and restart
   tests for the concrete database.

Schema ownership, migrations and backup/restore belong to the adapter. A backend is complete only
when all five stores and existing persisted identities are supported.
