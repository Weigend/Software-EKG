# Extension Guide

[Handbook home](README.md) · [Source modules](collector-sources.md) ·
[Repositories](collector-repositories.md) · [Importers](workbench-importers.md)

Use the narrowest extension that fits: a Collector source for periodic acquisition, a repository
factory for a new destination, or a Workbench importer for an existing file/API dataset. The
Collector contracts are runtime plugin boundaries. Workbench importers are source packages in the
single `workbench` module; the former independently published SDK/archetype is no longer a promised
third-party binary API.

## Build a Collector source

Implement `io.softwareecg.collector.source.CounterSource`:

```java
public final class ApplianceCounterSource implements CounterSource {
    @Override
    public String getId() {
        return "appliance";
    }

    @Override
    public CounterSeries queryCounter(CounterDescriptor descriptor) {
        // Return values in exactly the order of descriptor.getMetricDefinitions().
        // Use null/-1 only according to a documented failure policy.
        return new CounterSeries(readValues(descriptor));
    }
}
```

Register the implementation in:

```text
src/main/resources/META-INF/services/io.softwareecg.collector.source.CounterSource
```

```text
com.example.ekg.ApplianceCounterSource
```

Most sources do not implement `queryCounter` themselves. They extend
`io.softwareecg.collector.source.common.AbstractCounterSource` and read one metric at a time:

```java
public final class ApplianceCounterSource extends AbstractCounterSource {
    @Override
    public String getId() {
        return "appliance";
    }

    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metric) {
        // -1 says that the value could not be read
        return readValue(descriptor.getNodeAddress(), metric.getDefinition());
    }
}
```

Add `CounterWildcardSupport` if a configured expression expands to concrete metrics, and
`CounterHistorySupport` if the source can deliver timestamps and all samples since a cursor. Study
`mock` for the smallest adapter, `prometheus` for wildcard/history behaviour and `unix` for remote
command parsing.

A source inside this repository is a module of its own with a focused Maven dependency set,
added as a dependency of `collector/apps/cli` so that the distribution contains it. A source
built elsewhere is a jar, together with its dependencies, in the `lib` directory of the
distribution: the launchers put `lib/*` on the classpath, and `ServiceLoader` finds the
registration. Its ID is then usable as `metricGroups.<id>.type`.

Tests should cover ID discovery, metric ordering, unavailable endpoints, authentication,
wildcard-empty and wildcard-refresh cases, timestamps, shutdown and interruption. Never share
mutable connection state between descriptors unless the cache key includes every security and
endpoint dimension.

### Completing a log-event source

A production log adapter should define these semantics explicitly:

1. pattern syntax and whether it matches a whole line (`String.matches`) or searches within it;
2. timestamp parser/time zone, with arrival time only as a documented fallback;
3. output: one `1` sample per match, bucketed count, captured number, or categorical code mapping;
4. rotation identity, last offset and reconnect deduplication;
5. bounded buffering/backpressure and batched repository writes;
6. encoding, multiline events and redaction;
7. a stable series label derived from regex capture groups;
8. local and SSH integration tests including rotation during collection.

Prefer emitting normal `CounterSeries` rows so existing repositories and Workbench value-count
aggregation can chart them. Add a separate raw-log store only when retaining log text is an explicit
security and product decision.

## Build a Collector repository

Implement `RepositoryFactory` and return a `StaticTimeSeriesRepository` configured for the current
descriptor. Register the factory in:

```text
META-INF/services/io.softwareecg.collector.repository.RepositoryFactory
```

A Collector uses one factory. Select yours by its class name in `collector.properties`:

```properties
io.softwareecg.collector.repository.RepositoryFactory=com.example.ekg.ApplianceRepositoryFactory
```

The interface has one method, `createStaticRepository(CounterDescriptor)`, called for the series
of a job. Follow CSV for a simple append-only implementation or Solr for schema, commit,
compaction and retention. Define concurrency, flushing, close semantics, null/error values,
timestamp precision and failure recovery. Test rotation/commit boundaries and process termination.

An `ICollectorConnector` is the rarer extension for serving the Collector's in-memory values to an
external client. It has `start()` and `stop()` lifecycle methods and is also discovered through
ServiceLoader. Bind only to an explicitly chosen interface, add authentication before exposing it
beyond localhost, and document its concurrency and shutdown behaviour.

## Build a Workbench importer

Create a package below `io.softwareecg.awb.importer`, subclass `ImportModule`, annotate it as a
singleton discovered by the application's injection scan, and implement the capabilities it really
supports:

```java
@Singleton
public final class ApplianceImportModule extends ImportModule
        implements LocalProjectSupport, ClassicViewFlavorSupport, ContextMenuAware {

    @Override public String getMenuText() { return "Appliance Export"; }
    @Override public String getImporterID() { return "ApplianceExportImporter"; }
    @Override public ImportDialog getImportDialog() { return dialog; }
    @Override public LocalProjectImportStrategy getLocalProjectImportStrategy() {
        return new ApplianceImportStrategy(dialog.selectedPaths());
    }
    @Override public ContextMenuEntry getImportContextMenuItem() {
        return () -> "Import Appliance Export";
    }
}
```

Implement `SplitSourceProjectSupport` only when a remote fetcher exists, and
`AliasMappingProvider` when the source has meaningful domain names for EKG dimensions. Reuse the
shared file/query selectors, authentication model, progress tasks and repository write pipeline.
Add parser unit tests and an import test that queries the stored rows through the normal data layer.

## Extend Workbench UI or analysis

Modules add items through `EkgApplicationController` to Analysis, Import, Repository or Window.
Keep JavaFX in UI packages; architecture tests enforce that business logic and data access do not
depend on UI classes or bypass their layer. New smoothing/combination algorithms belong behind the
existing factories and enums, with tests for time alignment, missing values and calendar boundaries.

## Contribution checklist

- Document the new ID, configuration, credentials, failure values and an executable example.
- Keep secrets out of fixtures and logs.
- Add ServiceLoader/injection discovery tests, not only direct constructor tests.
- Test on the source's real operating system/tool version where parsing external commands.
- Run `mvn test` (with `xvfb-run -a` on headless Linux) and package the affected distribution.
- Verify the new module is actually inside the packaged `lib` directory and starts from the ZIP.

[Next: Feature and configuration reference →](reference.md)
