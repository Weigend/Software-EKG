# Consolidation into a single repository

**English** | [Deutsch](single-repository.de.md)

## Goal

The 7x structure is optimised for how Software-EKG is actually used: for a specific diagnostic case a data source is connected, the collector gathers the required measurements and the workbench displays them as time series. An extension should be possible with little context and without coordinated releases of several EKG repositories.

The consolidation was not a functional rewrite. Persistence formats and the ServiceLoader contracts of the collector were kept. What changed first is the build and ownership structure; after that the workbench was built anew from [components](workbench-components.md), with package names that say what they do. Since the consolidation, the collector configuration is YAML instead of XML (see below); existing XML files are still read.

## Architecture decision

A module boundary remains only if at least one of these reasons applies:

1. The result is an application that can be started or deployed separately.
2. Code is discovered at runtime as a collector plugin.
3. An optional adapter brings a clearly separate dependency stack.

The parts of the workbench do not meet these criteria. They are components in packages and compiled together; [rules](#what-ensures-the-structure-today) keep them apart. In the [collector](collector-components.md), by contrast, source and repository adapters are real extension boundaries and remain separate JARs.

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
| AWB API, BL, UI, Chart, Importer, Application and Distribution | `workbench` | one desktop application; components are packages, not releases |
| Runtime classes of the AWB-SDK | `workbench` | internal contracts of the only application |
| SDK test support | `workbench/src/test` | test fixture instead of a shipped product artifact |

The reactor thus comprises 17 modules, or 18 Maven projects including the root project, instead of 83 projects before. Each product has a POM of its own, `collector/pom.xml` and `workbench/pom.xml`; the root POM lists these two.

## Deliberately not carried over

- The SDK archetype was removed. It generated external importer projects for a form of extension that is no longer offered.
- The empty PostgreSQL source module was removed, and later the JSON repository of the collector and the WebSocket connector, which nothing used.
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

## What ensures the structure today

With the Maven modules of the workbench, what they ensured as a side effect disappeared as well. It
has been replaced by rules, and the rules ensure more than the modules did. A module only said
which other module it may use. Every public class of it could then be used, and inside a module
everything was possible.

| | 83 Maven projects | Today |
| --- | --- | --- |
| Direction of dependencies | between modules | between components, libraries and all packages, on every level |
| Cycles | impossible between modules, unnoticed inside of one | none on any level, neither between components nor between the parts of a package |
| Outer edge | every public class | only `api` and `spi` of a component; everything else is inside |
| Contracts | by convention | `api` is implemented by its own component, `spi` only holds contracts for others |
| Technologies | separated if they had modules of their own | Solr, Elasticsearch and ClickHouse do not know each other, nor do the kinds of data sources |
| Business logic | free of JavaFX by the choice of dependencies | free of JavaFX by rule, as the storage is |
| FXML and CSS | not checked | use other components through `api` and `spi` only, and find their files and controllers |

Three tests check this with every build:

- **`LayerArchitectureTest`** (ArchUnit) checks the code: outer edges, layers, cycles, technologies.
  The generated modules of the dependency injection are exempt because they wire all components
  together.
- **`ViewArchitectureTest`** checks FXML and CSS, where classes and files are written as text.
- **`ResourceReferencesTest`** checks that views, stylesheets and icons are where they are looked for.

**S202** shows the structure and measures it independently of these tests, from the bytecode:
accesses past an `api`, APIs that give the inside away, cycles and dependencies against the layers.
Its pictures and its current report are in [the components of the Workbench](workbench-components.md):
no access past an `api`, no package cycle, and four class cycles inside one package each, which are
known and stay. S202 finds what nobody wrote a rule for, for example a package in the wrong place;
what it finds becomes a rule if it must not come back.

In the collector the modules are still the boundaries. Its packages are described in
[the components of the Collector](collector-components.md), with what S202 reports about them; there
are no rules for them yet.

Beyond the structure:

- **Dependencies.** The workbench includes `collector-core` without its JavaScript engine
  (GraalJS). Only the standalone collector needs it, for calculated metrics (source `calc`),
  and even there the engine is only started when such a metric is queried.
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

Importers are part of the workbench. New importers are added in a package of their own below `io.softwareecg.awb.importer.adapter`, implement the contracts in `importer.spi` and are tested in the same reactor. This removes SDK versioning, the archetype, separate deployment and binary compatibility promises between tightly coupled components.

## Verification of the migration

The migration is safeguarded on four levels:

- the whole reactor compiles and packages from one root POM;
- the tests of all source, repository, collector and workbench components run in the new structure;
- CLI integration tests check the runtime resolution of the collector plugins, workbench tests check the embedded Prometheus collection, storage, importers and JavaFX components;
- end-to-end tests start the whole workbench and operate it through its user interface. They run with every build.

During the migration the old Java package names were deliberately kept, which reduced its risk. The clean-up came afterwards and is done for the workbench: the packages `sdk`, `common`, `da`, `metricanalyzer` and `project` no longer exist, and every top package is a component or a library. The boundaries are functional ones; the former Maven modules were not simulated.
