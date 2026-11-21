# Workbench Guide

[Handbook home](README.md) · [Getting started](getting-started.md) ·
[Importers](workbench-importers.md) · [Reference](reference.md)

The Workbench is the JavaFX investigation application. It manages projects and repositories,
imports or remotely queries data, explores dimensions, computes chart series, preserves queries as
bookmarks and exports evidence. Its normal mode is local/offline analysis with embedded Solr.

## Workspace and menus

The main window is dockable. **Window → Restore Default Layout** recovers the standard arrangement;
**Window → New Chart** opens another independent chart. The project bar selects the active project,
view flavour and data collection. The explorer shows series dimensions and bookmark groups.

| Menu | Current functions |
| --- | --- |
| Analysis | New Analysis, Data Sources, Exit |
| Import | Collector CSV, Prometheus, OWID, Binance, Yahoo Finance, Create Collector Project |
| Repository | Add repository |
| Window | New Chart, restore layout, dockable windows |
| Help | About/version/runtime information |

## Projects

A project is the boundary for series, dimension aliases, bookmarks and collection configuration.
Create a project from the project selector or **Import → Create Collector Project**. Projects may
use local copied data or a split/remote source. View flavours cover classic infrastructure and
cloud-native dimensions; specialised importers can supply domain aliases such as Country or Market.

The project bar exposes project metadata, repository/data collection, the view flavour and
importer shortcuts. The view flavour is Classic (bare metal or a single system), Hybrid or Cloud
Native; a project with cloud parts additionally names its platform: OpenShift, Kubernetes or
another cloud platform. Project management supports
create, select and edit. Use separate projects when retention, credentials or dimension meanings
differ; use one project when the point is to overlay the series.

## Repositories

**Repository → Add...** registers a query repository. The Workbench has clients for:

- embedded Solr (the offline default);
- remote Solr, standalone or SolrCloud;
- Elasticsearch;
- ClickHouse;
- H2 for Workbench metadata, not measurement-series analysis.

Repository configuration supports URLs and authentication fields exposed by the selected backend.
The embedded Solr base can be moved with the environment variable `EKG_SOLR_HOME` or the JVM
property `ekg.solr.baseDir`; its default is `~/.ekg/solr`. Repository and project records live in `~/.ekg/metadata.mv.db`.

## Data Sources and New Analysis

The Workbench contains a Collector of its own. **Analysis → New Analysis...** sets up an analysis
with current data in one dialog and starts collecting. No standalone Collector and no import are
needed.

The dialog asks three things:

1. **What** the analysis is about. The topics are tiles, and several can be chosen.
2. **How** the data comes, for every topic.
3. **Where** it is: the address of a Prometheus server, or the hosts to read directly.

| Topic | Ways to collect it |
| --- | --- |
| Linux / Unix | a Prometheus server (node exporter); or each host by SSH and `top` |
| Windows Server | each host through its performance counters |
| Java / JVM | a Prometheus server (Micrometer or the JMX exporter) |
| Kubernetes, Databases, Services | shown, but not available yet |

Jolokia and JMX for JVMs and a Prometheus server for Windows hosts are listed in the dialog as
planned ways and cannot be chosen yet.

The ways differ in what they give. A Prometheus server knows its systems, so they are found
rather than listed, and their history comes along. Hosts read directly have to be named, and
there are only the values collected from then on. Both direct ways also show single processes.

Windows Server needs a Workbench running on Windows, because the counters are read through a
native library. The Workbench reads its own machine and other Windows hosts that allow it, as
the user it runs as. See
[Windows Performance Counters](collector-sources.md#windows-performance-counters).

When the dialog is finished, every source becomes a data source of the analysis with a Collector
engine of its own, and the first chart opens as soon as the first values are there. The engines
run while the Workbench runs. After a restart they continue where the stored data ends; for a
Prometheus source the time in between is filled from the history the server keeps.

**Analysis → Data Sources...** manages the sources afterwards:

- pause a source and collect from it again;
- reconnect it;
- edit its connection;
- add a source to an analysis;
- remove a source;
- **Export Collector Config...** writes the source as a configuration for a standalone Collector.

The Workbench compiles a source to the same Collector YAML model described in the
[Collector guide](collector.md) and stores its secrets separately under `~/.ekg/secrets`. An
exported configuration contains no credentials. Older source definitions stored as XML remain
readable.

## Explorer and query filters

The explorer can represent physical hierarchy (host groups, hosts, process groups/processes,
measurements and metrics) and logical/cloud hierarchy (namespaces, services, pods and containers).
Expand nodes to narrow the context or use the chart filter header. Filter controls accept multiple
values and wildcards where the backing query supports them. A warning item reports incomplete or
inconsistent hierarchy data rather than silently hiding it.

The chart query includes project, source, time interval, selected dimensions, sort and compute
parameters. Changing a parent filter refreshes valid child choices. Use aliases shown by the
project; the stored dimensions remain stable underneath.

## Chart operations

Each chart supports:

- overlaying many series on a shared time axis;
- zooming and navigating the visible interval;
- a configurable chart time zone and date/time correction;
- following newly arriving data;
- foreground/background chart layers and legend visibility;
- combining chart series and calculating percentage deviation from a comparison;
- point reduction for display (Douglas–Peucker and value-change simplification);
- cancellation of background fetches from the application status bar.

### Smoothing and aggregation

Smoothing functions are `average`, `sum`, `minimum`, `maximum`, `median`, `difference` and
`value count`, plus no smoothing. Buckets can be automatic, off, or fixed calendar/time
granularities exposed in the header. Series combination supports concatenation and sum/average at
exact timestamp, second, minute, hour, day or month granularity. Choose sum for counts per
interval, as the Collector source `log` delivers them, value count for series in which every
sample is one event, difference for monotonically increasing counters, and averages only when
averaging has a defensible meaning.

### Bookmarks

A query bookmark preserves filters, time range and compute settings so the same chart can be
reproduced. Create bookmark groups, create bookmarks inside them, edit group names and reopen a
bookmark from the explorer. Bookmarks belong to the project metadata database; exporting a picture
does not create one automatically.

### Export

Chart controls export the visible result as a bitmap image or an Excel workbook. Image export lets
the user choose file path/dimensions and is suitable for incident reports. Excel export carries the
series data for independent checking. Exports reflect the computed/visible chart, so record the
query as a bookmark when reproducibility matters.

## AI Assistant

The dockable AI Assistant uses an OpenAI-compatible chat endpoint. Its tool set can:

- read the current chart context;
- list metric dimensions;
- query time series;
- propose and run an SSH command only through an approval handler.

The model is not configurable in the application yet. The Workbench talks to
[OpenRouter](https://openrouter.ai) with the model `anthropic/claude-sonnet-5.5` and takes the
API key from the environment variable `OPENROUTER_API_KEY`, which must be set before the
Workbench starts. Without it the assistant cannot reach a model. Another endpoint (OpenAI,
Ollama or another OpenAI-compatible service) needs a change in `AiChatModule` and a new build.

Chart context, metric names and queried values are sent to that service. Review what data
leaves the workstation and never approve an SSH command you would not run yourself. AI output is
a hypothesis aid, not collected evidence.

## Local files and recovery

| Path | Purpose |
| --- | --- |
| `~/.ekg/metadata.mv.db` | projects, repositories, bookmarks and source definitions |
| `~/.ekg/solr` | embedded measurement index |
| `~/.ekg/secrets` | file-backed source credentials |
| `~/.ekg/logs` | launcher/application logs |

Back up metadata and Solr together while the Workbench is stopped. If the layout becomes unusable,
restore it from the Window menu; do not delete the metadata database as a layout reset.

[Next: Workbench importers →](workbench-importers.md)
