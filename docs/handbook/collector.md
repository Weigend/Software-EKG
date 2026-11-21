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

A configuration does not describe servers one by one. It describes **groups**, and a list of
**collectors** that combine them. The combinations are multiplied out into jobs.

| Part | What it groups |
| --- | --- |
| `hostGroups` | Servers that are alike: the Windows servers, the frontend servers, the backend servers. Alike means that the same things are worth measuring on each of them and that comparing them in one chart makes sense. A group also holds the login its hosts share. |
| `metricGroups` | Metrics of one kind: the JVM metrics read over JMX, the operating-system metrics read with `top`, the events of a log. A metric group has exactly one `type`, the source its metrics are read with. |
| `processGroups` | The processes or endpoints the metrics are read of, on every host of a group. |
| `collectors` | One entry combines one metric group, one process group and one host group, and says how often they are read. |

### Many groups, multiplied out

That there are several groups of each kind is the construction principle. A kind of server
usually needs more than one kind of metric, and a kind of metric is usually wanted for more than
one kind of server. Application servers get JVM metrics and the events of their log. Other
backend services get operating-system metrics and the events of their log. The log metrics are
written once and used for both:

```yaml
project: Checkout incident

metricGroups:                 # WHAT, one source per group
  jvm:
    type: jmx
    metrics:
      - 'MXBean(java.lang:type=Memory).HeapMemoryUsage.used'
      - 'MXBean(java.lang:type=Threading).ThreadCount'
  os:
    type: unix
    metrics:
      - '\CPU\us'
      - '\Load\one'
      - '\Memory\used'
  errors:
    type: log
    metrics:
      - metric: ' ERROR '
        label: errors
      - metric: 'transaction failed.*code=TX-41'
        label: transaction failed TX-41

processGroups:                # OF WHICH processes, endpoints or files
  app-jvm:
    - name: checkout
      label: Checkout API
      source: 'service:jmx:rmi:///jndi/rmi://${REF:hostAddress}:9010/jmxrmi'
  app-log:
    - name: checkout
      label: Checkout API
      source: '/var/log/checkout/checkout.log'
  system:
    - name: global
      source: '${REF:hostAddress}:22'
  service-log:
    - name: service
      source: '/var/log/service/service-${REF:hostName}.log'
      args: 'lookback=24h'

hostGroups:                   # ON WHICH servers, grouped by what they are
  app-servers:
    username: observer
    password: '${ENV:EKG_PASSWORD}'
    hosts:
      - app-01.example.org
      - app-02.example.org
      - app-03.example.org
  backend-services:
    username: observer
    password: '${ENV:EKG_PASSWORD}'
    hosts:
      - name: db-01
        address: 10.20.2.11
      - name: mq-01
        address: 10.20.2.21

collectors:                   # the combinations
  - metricGroup: jvm          # application servers: JVM ...
    processGroup: app-jvm
    hostGroup: app-servers
    interval: 15s
  - metricGroup: errors       # ... and log
    processGroup: app-log
    hostGroup: app-servers
    interval: 15s
  - metricGroup: os           # backend services: operating system ...
    processGroup: system
    hostGroup: backend-services
    interval: 15s
  - metricGroup: errors       # ... and log
    processGroup: service-log
    hostGroup: backend-services
    interval: 15s
```

Every collector yields one job for every host and every process of its groups. A job reads all
metrics of its metric group, of one process, on one host:

| Collector | Hosts × processes | Jobs |
| --- | --- | --- |
| `jvm` on `app-servers` | 3 × 1 | 3 |
| `errors` on `app-servers` | 3 × 1 | 3 |
| `os` on `backend-services` | 2 × 1 | 2 |
| `errors` on `backend-services` | 2 × 1 | 2 |

Ten jobs come out of four collector entries, and the number of series is the jobs times their
metrics, with wildcards expanded for every job. The configuration grows by its groups, not by its
servers:

- a fourth application server is one more line in `app-servers`, and it is read by every
  collector that names the group;
- a second process to watch on every application server is one more entry in `app-jvm`;
- another kind of metric for all backend services is one more metric group and one more
  collector entry.

A host group may be named by any number of collectors, and so may a metric group and a process
group. What cannot be done is to put two sources into one metric group: JVM metrics and log
events of the same servers are two metric groups and two collectors.

### Groups are also what you compare by

The ids of the host group, the process group and the metric group are stored with every series.
In the Workbench they are the dimensions *host group*, *process group* and *metric group*: one
selection shows a metric for all hosts of a group in one chart, and the explorer shows the
processes below their group. Name the groups by what they are (`app-servers`, `app-jvm`, `jvm`),
not by an incident or a date.

The CSV, Solr and Elasticsearch repositories write all three; see
[Collector repositories](collector-repositories.md) for what a Solr collection or an
Elasticsearch index needs for it.

### Reading the file

The YAML reader rejects unknown keys, duplicate keys, missing references and wrong value types
with a line and path. Durations accept `ms`, `s`, `m`, `h` or `d`; a bare number means
milliseconds. A host is written as its address alone, or with `name`, `address` and a login of
its own. A process has a `name`, and optionally a `label` to show instead, the `source` its
metrics are read from and `args` for the source. A collector is of `type: remote` unless it says
`sidecar`.

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
- Prometheus and `log` additionally use the process `args` value `lookback=24h` to fill in what
  the server or the file already has when collecting starts.

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
- `log/config.yaml`: events of a log file, counted and aggregated, over SSH;
- `current/config-template.yaml`: all current YAML fields with comments.

[Next: Source modules →](collector-sources.md)
