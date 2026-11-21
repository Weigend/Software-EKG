# Collector Guide

[Handbook home](README.md) · [Source modules](collector-sources.md) ·
[Repositories](collector-repositories.md) · [Reference](reference.md)

The Collector is the headless acquisition process. It expands a declarative YAML configuration
into jobs, periodically reads source adapters and writes timestamped rows through one repository
adapter. It can run beside a workload (`sidecar`) or reach multiple systems (`remote`).

## Command line and service

```text
ekg-collector.sh [<config.yaml> [<log directory>]]
ekg-collector.sh --convert <config.xml> [<config.yaml>]
ekg-collector.sh --check | --version | --help
```

The packaged Unix launcher is `ekg-collector.sh`. Without a configuration it runs
`config/default/config.yaml`, which reads invented values. The second argument names the log
directory; the default is `logs` in the distribution.

```bash
./ekg-collector.sh config/unix/config.yaml
./ekg-collector.sh config/unix/config.yaml /var/log/software-ekg
EKG_COLLECTOR_JAVA_OPTS='-Xmx1g' ./ekg-collector.sh /opt/ekg/config.yaml
```

`EKG_COLLECTOR_JAVA_OPTS` replaces the launcher's default memory settings. Name the configuration
as an argument: the launchers always pass one to the Collector, so the JVM property
`-Dconfig=...` has no effect with them. It is read only when the class
`io.softwareecg.collector.StartCollector` is started directly without arguments.

`--check` reports the Java runtime, configuration and log directory the Collector would use and
then exits. `--help` (or `-h`) and `--version` print the usage and the version.

The Windows launcher `ekg-collector.cmd` takes the same two positional arguments and nothing
else. It has no `--help`, `--version`, `--check` or `--convert`; it would read such an option as
the name of the configuration.

### Background and service operation

On Linux and macOS the script `ekg-collector` runs the Collector in the background:

```bash
./ekg-collector start | stop | restart | status | version
```

It can be called where it lies or through a link, for example from `/etc/init.d`. It takes its
settings from the environment. For a service they are set in `/etc/default/ekg-collector`, which
the script reads if it exists.

| Variable | Meaning | Default |
| --- | --- | --- |
| `EKG_COLLECTOR_CONFIG` | the configuration | `config/default/config.yaml` |
| `EKG_COLLECTOR_LOGDIR` | where the Collector writes | `logs` |
| `EKG_COLLECTOR_JAVA_OPTS` | options for the JVM | `-Xmx386m` |
| `JAVA_HOME` | the Java 25 runtime, unless the distribution has one in `jre` | |

The script keeps the process id in `ekg-collector.pid` and the output of the JVM in `stderr.log`,
both in the distribution directory. `status` exits with code 3 if the Collector is not running.

On Windows `ekg-collector-svc.cmd` is the entry point for a service wrapper, not a service by
itself. `ekg-collector-svc.cmd start <config.yaml> [<log directory>]` runs the Collector, with a
Jolokia agent on port 8778 of localhost, until it is stopped. Its `stop` command acts on a
Collector in the same JVM, so it is meant for a wrapper that calls both in one process.

Use the foreground launcher while developing a configuration because validation and connection
failures remain visible.

### Converting an XML configuration

Legacy XML configurations from Collector 6.1 and later remain readable. Convert one to the strict
current YAML format with:

```bash
./ekg-collector.sh --convert config.xml             # writes config.yaml beside the input
./ekg-collector.sh --convert config.xml new.yaml    # or names the result
```

The conversion does not overwrite an existing file and does not carry over the comments of the
XML file. On Windows, where the launcher has no `--convert`, call the class the Unix launcher
calls:

```bat
jre\bin\java -classpath "config;lib\*" io.softwareecg.collector.config.ConvertConfiguration config.xml
```

New configurations should use YAML.

## Configuration model

```yaml
project: Checkout incident

metricGroups:       # what and through which source
  jvm:
    type: jolokia
    metrics:
      - metric: 'java.lang:type=Memory,*'
        wildcard: true

processGroups:      # endpoint or process interpretation
  services:
    - name: checkout
      label: Checkout API
      source: 'http://${REF:hostAddress}:8778/jolokia/'
      args: optional-source-specific-text

hostGroups:         # where
  production:
    username: observer
    password: '${ENV:EKG_PASSWORD}'
    hosts:
      - name: checkout-01
        address: 10.20.1.11

collectors:         # combine and schedule
  - type: remote
    metricGroup: jvm
    processGroup: services
    hostGroup: production
    interval: 15s
    expandInterval: 20m
```

One collector entry produces a job for every process × host in its referenced groups. The YAML
reader rejects unknown keys, duplicate keys, missing references and wrong value types with a line
and path. Durations accept `ms`, `s`, `m`, `h` or `d`; a bare number means milliseconds.

### Metrics and wildcards

A metric can be a string or an object:

```yaml
metrics:
  - '\CPU\us'
  - metric: 'java.lang:type=Threading,*'
    wildcard: true
    regexPostFilter: '.*Threading.*\.(.*)$'
    label: 'threads ${FILTER:1}'
```

A `*` is expanded only in a metric with `wildcard: true`. A metric written as a plain string, or
without `wildcard`, is taken literally even if it contains a `*`. Prometheus is the exception:
every expression expands to the series it returns.

A `regexPostFilter` filters expanded names. Its capture groups are available in `label` as
`${FILTER:1}`, `${FILTER:2}`, and so on. Do not assign one fixed label to many wildcard matches
unless collapsing their names is intentional.

### Variables and secrets

| Form | Meaning |
| --- | --- |
| `${ENV:NAME}` | environment variable; preferred for passwords |
| `${JVMOPT:name}` | JVM property passed as `-Dname=value` |
| `${REF:hostName}` / `${REF:hostAddress}` | current host |
| `${REF:podName}` / `${REF:podAddress}` | current pod |
| `${REF:procName}` / `${REF:procLabel}` / `${REF:procSource}` | current process |
| `${FILTER:n}` | regex capture in an expanded metric label |

Quote counter paths and strings containing `${...}` with single quotes. Never commit expanded
secrets. The Collector resolves variables for the job at runtime.

`${ENV:...}` and `${JVMOPT:...}` replace the whole value they are written in, not only the
placeholder. Use them as the complete value of a setting, as in `password: '${ENV:EKG_PASSWORD}'`.
`${ENV:NAME}` falls back to the JVM property `NAME` if there is no such environment variable.

### Remote and sidecar placement

A `remote` collector requires `hostGroup`. A `sidecar` collector uses `nodeGroup`, which may name a
host group, pod group or individual pod; without it the source reads the Collector's own machine.

```yaml
collectors:
  - type: sidecar
    metricGroup: host
    processGroup: local
    interval: 5s
```

Cloud-native targets can be declared below `platform` with a namespace/project name, API address,
credentials, refresh interval, pod groups, pods and containers. They add namespace, service, pod
and container dimensions to collected rows.

### Scheduling and discovery

- `interval` repeats a job, for example `500ms`, `15s` or `5m`.
- `at` requests a fixed collection time instead of an interval (legacy scheduling surface).
- `expandInterval` re-resolves wildcard metrics and dynamic processes/systems. Without it,
  expansion happens once at startup.
- Prometheus additionally uses the process `args` value `lookback=24h` to backfill its first run.

## Operations

Start with the `mock` configuration, then add one real metric, one process and one host. Watch the
log for wildcard expansion and repository writes before scaling out. A value of `-1` conventionally
means a source read failed; Prometheus returns no row on an unreachable API and logs state changes.

The engine exposes start/stop and per-job status internally; the service wrapper exposes process
status. The separate JavaFX Collector UI still exists as a legacy application with login, runtime,
storage, settings/config import and a simple chart, but the maintained operational path is the
headless CLI plus the Workbench.

## Complete examples

The packaged `config` directory contains executable examples:

- `default/config.yaml` and `mock/config.yaml`: generated data;
- `unix/config.yaml`: process and host metrics through SSH;
- `win_perf_counter/config.yaml`: Windows performance counters of the processes of the local
  machine;
- `jolokia/config-example-solr-localhost.yaml`: JVM and Solr MBeans;
- `cluster-monitoring/config.yaml`: five cluster nodes read with `top` over SSH, with property
  files of its own beside it that select Solr as repository;
- `current/config-template.yaml`: all current YAML fields with comments.

[Next: Source modules →](collector-sources.md)
