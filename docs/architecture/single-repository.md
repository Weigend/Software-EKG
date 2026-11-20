# Consolidation into a single repository

**English** | [Deutsch](single-repository.de.md)

## Goal

The 7x structure is optimised for how Software-EKG is actually used: for a specific diagnostic case a data source is connected, the collector gathers the required measurements and the workbench displays them as time series. An extension should be possible with little context and without coordinated releases of several EKG repositories.

This is not a functional rewrite. Package names, persistence formats, ServiceLoader contracts and application entry points are kept as far as possible. What changed is the build and ownership structure. Since the consolidation, the collector configuration is YAML instead of XML (see below); existing XML files are still read.

## Architecture decision

A module boundary remains only if at least one of these reasons applies:

1. The result is an application that can be started or deployed separately.
2. Code is discovered at runtime as a collector plugin.
3. An optional adapter brings a clearly separate dependency stack.

The UI, business and API layers inside the workbench do not meet these criteria. They are separated by packages and compiled together. In the collector, by contrast, source and repository adapters are real extension boundaries and remain separate JARs.

## Mapping old to new

| Before | Now | Reason |
| --- | --- | --- |
| Aggregator with four gitlinks | one Git repository and one Maven reactor | one change, one review, one version |
| `ssh` | `collector/core` | exclusively infrastructure of the collector and the Unix connectors |
| Collector Common, Config, Memory, Connector, Repository API, Source Common and Control | `collector/core` | tightly coupled runtime base without independent deployment value |
| Collector source implementations | `collector/sources/*` | real plugins loaded via ServiceLoader |
| CSV, JSON, RDB, Solr and Elasticsearch | `collector/repositories/*` | optional storage adapters with different dependencies |
| Collector Delivery | `collector/apps/cli` | product that can be started on its own |
| Collector UI and Jolokia WAR | `collector/apps/ui`, `collector/apps/jolokia-agent` | independent deployments |
| AWB API, BL, UI, Chart, Importer, Application and Distribution | `workbench` | one desktop application; layers are packages, not releases |
| Runtime classes of the AWB-SDK | `workbench` | internal contracts of the only application |
| SDK test support | `workbench/src/test` | test fixture instead of a shipped product artifact |

The reactor thus comprises 19 product modules, or 20 Maven projects including the root project, instead of 83 projects before.

## Deliberately not carried over

- The SDK archetype was removed. It generated external importer projects for a form of extension that is no longer offered.
- The empty PostgreSQL source module was removed.
- The AWB sandbox programs that were not integrated into the application (one-off
  converters, repository analysis and experimental Prometheus clients) were not
  carried over as product code.
- The old operating-system-specific AWB ZIP wrappers were not carried over.
  The distribution of the workbench is created directly from the single `workbench` module
  (directory with start scripts; with the `dist` profile additionally a Java runtime
  and a ZIP), instead of introducing three application modules again.
- Clone scripts, gitlinks and the CI logic for gathering separate EKG repositories were removed.
- Historical manual root test data was not duplicated. Test data needed by automated tests still lives with the respective module.
- Generated build output, old logs and local IDE/agent settings do not belong in the new repository.

WFX deliberately stays external. It is a general JavaFX framework with its own life cycle and is used as a versioned Maven dependency. `install-wfx.sh` provides this dependency on a new development machine.

## What modules no longer enforce

With the Maven modules of the workbench, what they ensured as a side effect disappeared as well.
This has been replaced explicitly:

- **Layers.** `LayerArchitectureTest` (ArchUnit) checks the package boundaries: business logic
  without JavaFX, no access from the UI to the data layer that bypasses the business logic, data
  layer without UI. Generated Avaje modules are exempt because they wire all layers together.
- **Dependencies.** The workbench includes `collector-core` without its JavaScript engine
  (GraalJS) and WebSocket server. Only the standalone collector needs both: GraalJS for
  calculated metrics (source `calc`), and even there the engine is only started when such a
  metric is queried.
- **Data outside the build.** The Solr index is located under `~/.ekg/solr`, not under
  `target/`. The Solr configuration is copied there from the application on every start.
- **One version.** `revision` in the root POM; programs and scripts read it from the build.

## Collector configuration as YAML

The configuration of the collector is a format-neutral model
(`io.softwareecg.collector.config.model`) that is read and written as YAML. The XSD, the JAXB
classes generated from it and the dependency on JAXB have been dropped. Validation happens when
reading: unknown and duplicate keys, missing values and references to groups that do not exist,
each with its location. XML files from Collector 6.1 on are converted into the same model by a
small reader without JAXB; the older format (`CounterGroups`) is no longer read. For its data
sources, the workbench builds the model instead of assembling text.

## Extension model

### Collector source

A source implements the existing collector contracts and registers its implementation via `META-INF/services`. It can then be used both in the headless distribution and – once added as a dependency – in the workbench. This boundary makes sense for customer-specific APIs, devices, robots, switches or proprietary platforms.

### Workbench importer

Importers are part of the workbench. New importers are added in the appropriate `io.softwareecg.awb.importer` package and tested in the same reactor. This removes SDK versioning, the archetype, separate deployment and binary compatibility promises between tightly coupled components.

## Verification of the migration

The migration is safeguarded on three levels:

- the whole reactor compiles and packages from one root POM;
- the tests of all source, repository, collector and workbench components run in the new structure;
- CLI integration tests check the runtime resolution of the collector plugins, workbench tests check the embedded Prometheus collection, storage, importers and JavaFX components.

The old Java package names were deliberately not renamed across the board. This reduces the migration risk and keeps configurations and reflection access compatible. A later package clean-up can happen independently and should only improve functional boundaries, not simulate Maven modules again.
