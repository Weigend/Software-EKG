# Feature and Configuration Reference

[Handbook home](README.md) · [Collector](collector.md) · [Workbench](workbench.md) ·
[Extensions](extensions.md)

This page is the compact inventory of deliberately reachable CLI, UI and extension surfaces in the
current repository. Follow the linked guides for procedures and limitations.

## Collector CLI

| Surface | Syntax / effect |
| --- | --- |
| Default foreground run | `ekg-collector.sh` → `config/default/config.yaml` |
| Named configuration | `ekg-collector.sh <configfile>` |
| With the display | `ekg-collector.sh --ui <configfile>`; background: `EKG_COLLECTOR_UI=1` |
| Override log directory | `ekg-collector.sh <configfile> <logdir>` |
| JVM options | `EKG_COLLECTOR_JAVA_OPTS='-Xmx1g' ekg-collector.sh <configfile>` |
| Dependency check | `ekg-collector.sh --check` |
| Help / version | `ekg-collector.sh --help`; `ekg-collector.sh --version` |
| XML conversion | `ekg-collector.sh --convert old.xml [new.yaml]` |
| Background operation | `ekg-collector start\|stop\|restart\|status\|version` |
| Windows foreground run | `ekg-collector.cmd [<configfile> [<logdir>]]`, no options |
| Windows service entry point | `ekg-collector-svc.cmd start <configfile> [<logdir>]` |
| Source-tree launcher | `run-collector.sh [--no-build] [config [logdir]]` |

The JVM property `-Dconfig=...` does not select the configuration through the launchers; they
always pass one as an argument.

## Collector YAML keys

| Path | Meaning |
| --- | --- |
| `project` | Workbench project assigned to rows |
| `metricGroups.<id>` | metrics of one kind; any number of groups. The id becomes the dimension *metric group* |
| `metricGroups.<id>.type` | source ID, exactly one per group |
| `metricGroups.<id>.metrics[]` | string or metric object |
| `metrics[].metric` | source-specific counter/expression/regex |
| `metrics[].label` | displayed series name |
| `metrics[].regexPostFilter` | filter after wildcard expansion |
| `metrics[].wildcard` | `true` expands a `*`; omitted or `false` takes the metric literally |
| `processGroups.<id>[]` | the processes or endpoints read on every host; any number of groups. The id becomes the dimension *process group* |
| `processGroups.<id>[].name` | source-visible process/endpoint name |
| `.label`, `.source`, `.args` | display name, endpoint/file, source arguments |
| `hostGroups.<id>` | servers that are alike; any number of groups. The id becomes the dimension *host group* |
| `hostGroups.<id>.username/password` | group credential defaults |
| `hostGroups.<id>.hosts[]` | string host or name/address/credential object |
| `platform` | cluster name/API credentials/refresh/pod groups |
| `collectors[]` | one metric group × one process group × one host group; one job per host and process |
| `collectors[].type` | `remote` (default) or `sidecar` |
| `.metricGroup`, `.processGroup` | required group references |
| `.hostGroup` | required for remote |
| `.nodeGroup` | optional host/pod group or pod for sidecar |
| `.interval` | repeat duration |
| `.expandInterval` | wildcard/dynamic target refresh |
| `.at` | legacy fixed-time scheduling alternative |

Durations are `ms`, `s`, `m`, `h`, `d`. Variables are `${ENV:...}`, `${JVMOPT:...}`,
`${REF:hostName|hostAddress|podName|podAddress|procName|procLabel|procSource}` and `${FILTER:n}`.

## Registered Collector plugins

Sources: `calc`, `mock`, `unix`, `unixio`, `unixdf`, `win`, `jmx`, `jolokia`, `ekgjolokia`, `prometheus`,
`log`. `ekgjolokia` is legacy; see [Source modules](collector-sources.md).

The source `log` reads the `args` of a process as arguments separated by semicolons: `lookback`,
`timestamp`, `zone`, `charset` and `port`. A metric of it is a regular expression, optionally with
a group named `sum`, `avg`, `min`, `max` or `last`. See
[Log files and events](collector-sources.md#log-files-and-events).

Repository factories: CSV, archive, Solr and Elasticsearch. The line
`io.softwareecg.collector.repository.RepositoryFactory=<class>` in `collector.properties` selects
one; remote RDB stores read their connection from `remote-repository-config.properties`. See
[Collector repositories](collector-repositories.md#selecting-a-repository).

Collector connectors are a second ServiceLoader/configuration surface. The default NOP connector
exposes nothing. The WebSocket connector binds `localhost:8025` below `/collector` and exposes the
in-memory values; it is a legacy local integration surface rather than a secured remote API. Select
an `ICollectorConnector` implementation through its fully qualified interface key in
`collector.properties` only when a local client requires it.

## Workbench menus and actions

| Location | Actions |
| --- | --- |
| Analysis | New Analysis; Data Sources; Exit |
| Import | EKG Collector Logs; Prometheus Data; OWID COVID-19 Data; Binance Market Data; Yahoo Finance Market Data; Create Collector Project |
| Repository | Add repository |
| Window | New Chart; Restore Default Layout; activate module windows |
| Help | About Software-EKG |
| Project selector/bar | create, select, edit; repository/collection/view flavour; importer shortcuts |
| Explorer | traverse physical/cloud dimensions; open/query series and bookmarks; context actions |
| Chart header | project/dimension/time filters; smoothing; combine; refresh/follow |
| Chart/legend | zoom; visibility; colours/types; foreground/background; y-axis alignment; absolute/relative combination; clear/delete |
| Export | bitmap screenshot; Excel data |
| Status bar | background progress and cancellation |

## Chart compute values

Smoothing types: Inactive, Average, Min, Max, Sum, Median, Value Count and Value Difference.

Smoothing granularities: exact/off, seconds, minutes, hours, days, weeks, 30-day months, calendar
months, quarter, half-year and auto.

Combine modes: Inactive; Concatenate; Sum or Average at exact timestamps, per second, per minute,
per hour, per day or per month.

## Workbench repositories and importers

Query backends present in the current data-access layer are embedded/remote Solr, Elasticsearch and
ClickHouse; H2 stores Workbench metadata. Importers are Collector CSV, Prometheus local/remote,
OWID, Binance and Yahoo Finance. See [Workbench importers](workbench-importers.md) for accepted data
and options.

## Files and environment

| Setting/path | Effect |
| --- | --- |
| `EKG_JAVA_OPTS` | extra Workbench JVM options |
| `EKG_COLLECTOR_JAVA_OPTS` | Collector JVM options; replaces launcher memory defaults |
| `EKG_COLLECTOR_CONFIG`, `EKG_COLLECTOR_LOGDIR` | configuration and log directory of `ekg-collector start` |
| `EKG_COLLECTOR_UI` | `1` lets `ekg-collector start` show the display of the Collector; needs `DISPLAY` as well |
| `/etc/default/ekg-collector` | environment of the background Collector, read if it exists |
| `collectorPropertiesPath` | JVM property or environment variable: where `collector.properties` is |
| `collectorRepositoryPropertiesPath` | the same for `remote-repository-config.properties` |
| `OPENROUTER_API_KEY` | API key of the Workbench AI Assistant |
| `io.softwareecg.collector.ui.enabled`, `.fullscreen`, `.size` | the display of the Collector: on or off (off), whole screen (no), size of its window (800x480) |
| `EKG_SKIP_CHECKS=1` | let the Workbench start despite display/library preflight failures |
| `EKG_SOLR_HOME` | move embedded Solr base (JVM property: `ekg.solr.baseDir`) |
| `~/.ekg/metadata.mv.db` | project/repository/bookmark/source metadata |
| `~/.ekg/solr` | default embedded series index |
| `~/.ekg/secrets` | Workbench file secret store |
| `~/.ekg/logs` | Workbench launcher logs |
| Collector `config/collector.properties` | adapter selection and general settings |
| Collector `config/logback.xml` | logging configuration |
| Collector `logs/ekg/<start time>/` | files of the CSV repository, `<host>_<process>_<group>_<period>.csv` |
| `io.softwareecg.collector.source.rollingPolicyPattern` | period of a CSV file, default `%d{yyyy-MM-dd}`; `none` for no files by time |
| `Csv4jRepository.MaxFileSize`, `Csv4jRepository.MaxBackupIndex` | size of a CSV file (30MB) and files kept per job (0 = all) |

## Deliberate extension points

| Contract | Discovery | Use |
| --- | --- | --- |
| `CounterSource` | Java `ServiceLoader` | new metric/event source |
| `CounterWildcardSupport` | implemented by source | expand patterns/dynamic series |
| `CounterHistorySupport` | implemented by source | timestamped backfill |
| `RepositoryFactory` | Java `ServiceLoader` | new Collector destination |
| `ICollectorConnector` | Java `ServiceLoader` / registry property | external access to Collector memory |
| `ImportModule` + capability interfaces | Workbench injection scan | new UI importer |
| `EkgApplicationController` menu methods | injected module | new Analysis/Import/Repository/Window entry |
| smoother/combiner factories | code registration/enums | new chart computation |

The Workbench's former external SDK is not a stable separately versioned plugin promise. Add
Workbench extensions inside the consolidated module and validate its architecture tests.

## Known compatibility and incomplete surfaces

- Legacy Collector XML is readable/convertible; YAML is the current format.
- The display of the Collector has not been run on a Raspberry Pi, with a touch screen or on
  Windows yet. The standalone Collector UI of earlier versions, with its login and its settings
  dialogs, is replaced by it.
- `log` builds one series per expression, not one per captured value, and does not store the text
  of the lines. The source `rlog` and the module `logevents` of earlier versions are removed.
- `ekgjolokia` targets the historical filtered endpoint; prefer `jolokia`.
- Windows counters can be written with their English names on a Windows of every language. The
  source `win` needs a Collector on Windows with the native library; `%1` is not a placeholder.
- Unix parser output varies by tool implementation. `unix` expects the batch mode of the Linux
  `top`.
- The JSON repository of earlier versions, which was never part of the distribution, is removed.
- The AI Assistant is bound to OpenRouter and `OPENROUTER_API_KEY`; its SSH tool runs a command
  only after explicit approval. Its conclusions are not a substitute for saved evidence.

[Back to handbook home →](README.md)
