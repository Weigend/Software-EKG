# Collector Source Modules

[Handbook home](README.md) · [Collector](collector.md) · [Repositories](collector-repositories.md) ·
[Extensions](extensions.md)

A source is selected by `metricGroups.<id>.type`. The current distribution registers the IDs
below through Java `ServiceLoader`.

| ID | Reads | Wildcards / history | Status |
| --- | --- | --- | --- |
| `calc` | JavaScript expressions over values in Collector memory | referenced-counter expansion | supported, advanced |
| `mock` | generated values | wildcard expansion | supported, diagnostic |
| `unix` | `top` locally or through SSH | metric and process expansion | supported |
| `unixio` | `iostat` | device and NFS mount expansion | supported |
| `unixdf` | `df -m` for one path | none | supported |
| `win` | Windows Performance Counters, local or of other hosts | counter expansion | supported, Collector on Windows |
| `jmx` | direct JMX/RMI | MBean expansion | supported |
| `jolokia` | JMX over HTTP | MBean expansion | supported |
| `ekgjolokia` | older EKG Jolokia endpoint | expansion | legacy |
| `prometheus` | Prometheus HTTP API | every expression expands to series; backfill | supported |
| `log` | `tail --follow=name` plus regex | intended event matching | incomplete |
| `rlog` | whole remote file over HTTPS :19888 | none | legacy |

Wildcards are expanded only for a metric with `wildcard: true`; see
[Metrics and wildcards](collector.md#metrics-and-wildcards). `prometheus` is the exception.

## Calculated metrics

`calc` runs after ordinary jobs and evaluates JavaScript expressions over their values held in
Collector memory. Built-in functions are `value(counter)`, `avg(counter)`, `sum(counter)`,
`delta(counter)`, `median(counter)`, `min(counter)` and `max(counter)`. The source is delayed so its
inputs can be recorded first and it starts GraalJS lazily on the first calculation.

```yaml
metricGroups:
  host:
    type: unix
    metrics:
      - '\CPU\us'
      - '\CPU\sy'
  derived:
    type: calc
    metrics:
      - metric: 'value("\CPU\us") + value("\CPU\sy")'
        label: 'cpu busy'
      - metric: 'delta("\CPU\us")'
        label: 'cpu user change'
```

A counter is named by the label of the base metric, or by the metric itself if it has no label.
Write each backslash once: the source escapes them for JavaScript itself, and a doubled backslash
no longer matches the name. The values come from the Collector memory of the same host address,
so collect the base metrics and the calculated ones for the same hosts, and deploy and validate
the base metrics first.

Expressions are executable JavaScript with host access in the current implementation. Treat
configuration authors as trusted code authors and do not accept expressions from untrusted users.

## Mock

Use `mock` to verify scheduling, storage and Workbench import without access to a monitored system.
It returns changing invented values; it must never be interpreted as operational evidence.

```yaml
metricGroups:
  smoke-test:
    type: mock
    metrics: ['ABC']
```

## Unix `top`

`unix` runs `top` in batch mode (`top -c -n 1 -b`, as the `top` of Linux offers it) and parses host
and process counters. The process name can match a command line; repeated matches are suffixed
(`java`, `java#1`, ...).

The process `source` names host and SSH port. Without it the address of the host and port 22 are
used. Only a host written as `localhost` is run as a local process; `127.0.0.1` goes through SSH
like any other address.

```yaml
metricGroups:
  host-and-java:
    type: unix
    metrics:
      - metric: '\CPU\*'
        wildcard: true
      - metric: '\Memory\*'
        wildcard: true
      - metric: '\Load\*'
        wildcard: true
      - '\Process(${REF:procName})\CPU'
      - '\Process(${REF:procName})\MEM'

processGroups:
  java:
    - name: 'java -Xmx1536m'
      source: '${REF:hostAddress}:22'
```

Available global groups are `CPU`, `Memory`, `Swap`, `Tasks`, `Load`, `User` and `Uptime`.
Process counters include `PID`, `USER`, `PR`, `NI`, `VIRT`, `RES`, `SHR`, `S`, `TIME`, `CPU` and
`MEM`. Authentication comes from the host/group. SSH currently uses password credentials in this
Collector path.

## Unix I/O and filesystems

`unixio` runs `iostat -x` and exposes the columns of each device below
`\Device(<name>)\<column>`, for example `\Device(sda)\wrqm/s`. Where `iostat` reports NFS mounts,
they appear below `\Filesystem(<mount>)\<column>`.

`unixdf` runs `df -m` for one path and reads the four values of its line. The path is the `args`
of the process; the metrics are the fixed names `1M-blocks`, `Used`, `Available` and `Use%`.
There is nothing to expand: use one process per path.

Both use the same local/SSH execution model as `unix`.

```yaml
metricGroups:
  disks:
    type: unixio
    metrics:
      - metric: '\Device(*)\*'
        wildcard: true
  disk-space:
    type: unixdf
    metrics:
      - '1M-blocks'
      - 'Used'
      - 'Available'
      - 'Use%'

processGroups:
  volumes:
    - name: data
      source: '${REF:hostAddress}:22'
      args: /var/lib/data
```

The exact columns reflect the installed operating-system tools. Test against a representative host
before deploying one configuration across different Unix families.

## Windows Performance Counters

`win` reads Windows performance counters through a native library,
`bin\dlls\wpcaccess-5.1.0-x64.dll`. The Collector therefore has to run on Windows. The library is
part of the Windows packages of a release; a local build contains it only with the Maven profile
`wpc` (see `collector/sources/windows/wpc/README.md`). Without the library the Collector starts
as usual and logs that the Windows counters are not available.

The source reads the machine it runs on for the host address `localhost` or `127.0.0.1`, and
other Windows hosts by their address. It reads them as the user the Collector runs as; the host
credentials of the configuration are not used.

Write a counter without the machine. English names work on a Windows of every language: Windows
numbers its performance objects and counters the same everywhere, and the source translates the
English name into the one of the machine. A name in the language of the machine works as well.

```yaml
metricGroups:
  windows-host:
    type: win
    metrics:
      - '\Processor(_Total)\% User Time'
      - '\Memory\Available Bytes'
      - metric: '\Network Interface(*)\Bytes Received/sec'
        wildcard: true
  windows-process:
    type: win
    metrics:
      - '\Process(${REF:procName})\% Processor Time'
      - '\Process(${REF:procName})\Working Set'

processGroups:
  programs:
    - name: java
```

Name the process with `${REF:procName}`. A placeholder `%1`, as configurations of earlier
versions may contain it, is not replaced by anything.

To see whether a distribution can read counters, call in its directory:

```bat
jre\bin\java -cp "lib\*" -Djava.library.path=bin\dlls io.softwareecg.collector.source.wincount.WindowsPerformanceCounterCheck \System\Threads
```

Another host must allow its counters to be read over the network.
`bin/install/activate-performance-counter.bat` prepares a host for that: it starts the services
RemoteRegistry and Server and opens file and printer sharing in the firewall. It needs
administrator rights and `ntrights.exe` of the Windows Server Resource Kit on the `PATH`. The
script is old: its messages are German and it uses `netsh firewall`. Read it before running it.

`io.softwareecg.collector.source.wincount.WindowsPerformanceCounterAccess.UseBulkQuery`
in `collector.properties` selects bulk querying and defaults to `true`.

## Direct JMX

`jmx` connects through a JMX service URL in the process `source`. A metric is written as
`MXBean(<object name>).<attribute>`. Only numeric readable MXBean attributes are useful; composite
data uses dot notation after the attribute.

```yaml
metricGroups:
  jvm:
    type: jmx
    metrics:
      - 'MXBean(java.lang:type=Memory).HeapMemoryUsage.used'
      - 'MXBean(java.lang:type=Threading).ThreadCount'

processGroups:
  app:
    - name: checkout
      source: 'service:jmx:rmi:///jndi/rmi://${REF:hostAddress}:9010/jmxrmi'
```

Use `source: self` to attach to the local Collector JVM. JMX credentials are inherited from the
host. Failed connections are retried after 120 seconds.

## Jolokia

`jolokia` queries MBeans through Jolokia HTTP. A metric is an object-name pattern plus attribute
path. It is often easier through firewalls than RMI and works well for JVM, application server and
Solr metrics.

```yaml
metricGroups:
  jvm-memory:
    type: jolokia
    metrics:
      - metric: 'java.lang:type=Memory,*'
        wildcard: true
      - metric: 'java.lang:type=GarbageCollector,*'
        wildcard: true

processGroups:
  solr:
    - name: Solr
      source: 'http://${REF:hostAddress}:7777/jolokia/'
```

End the `source` URL with a slash; the source warns about a URL without one.

The repository includes a Jolokia-agent web application and a detailed Solr example under
`config/jolokia`. `ekgjolokia` is retained for the historical filtered EKG endpoint; choose normal
`jolokia` for new configurations.

## Prometheus

For `prometheus`, the host address is the Prometheus base URL and every metric is a PromQL
expression. Each returned label set becomes its own EKG series. The source uses instant queries for
current values and range queries to retrieve all values since the last stored point.

```yaml
metricGroups:
  nodes:
    type: prometheus
    metrics:
      - 'up{job="node"}'
      - 'rate(node_cpu_seconds_total{mode!="idle"}[5m])'

processGroups:
  api:
    - name: Prometheus
      args: 'lookback=24h'

hostGroups:
  monitoring:
    username: api-reader
    password: '${ENV:PROMETHEUS_PASSWORD}'
    hosts:
      - name: central-prometheus
        address: 'https://prometheus.example.org'
```

If the host has a username, the source sends it with the password as Basic authentication. A
password without a username is sent as a Bearer token.

`lookback=<n><s|m|h|d>` applies only to the first fill. The job interval is the query-range step;
values are aligned to that grid. Queries are windowed to at most 10,000 points and roughly 500,000
in-memory values. Set `expandInterval` so newly appearing label sets become series.

## Log files and events

### Why this source matters

Logs contain temporal facts that numeric monitoring misses. Turning matches into chartable events
or bucketed counts makes it possible to compare:

- the exact sequence of transaction error codes across services;
- logins, failed logins or retries per minute;
- deployment, restart and failover markers beside CPU or latency;
- any recurring line that an operator could find with `tail -F ... | grep ...`.

The Collector is the right place to automate that operation: it can use one definition on every
host, reconnect, preserve timestamps and feed the same dimensions as all other measurements.

### Present implementation status

The registered `log` source starts `tail --retry --follow=name <file>` locally or over SSH and
tests each line against Java regular expressions supplied as metrics. `${HOSTNAME}` in the file
name is replaced. However, on the first matching line the current implementation reaches an
explicit `NotImplementedException`: raw-log persistence and `logLinesWritten` are not implemented.
Consequently `log` is **not currently an end-to-end supported event source** and should not be used
for evidence collection without completing and testing the adapter.

This is the intended shape of its configuration, documented for development rather than production:

```yaml
metricGroups:
  transaction-events:
    type: log
    metrics:
      - '.*transaction failed.*code=(TX-41|TX-72).*'
      - '.*user login succeeded.*'

processGroups:
  checkout-log:
    - name: checkout
      source: '/var/log/checkout/checkout-${HOSTNAME}.log'

hostGroups:
  checkout-nodes:
    username: ekg
    password: '${ENV:EKG_SSH_PASSWORD}'
    hosts: [checkout-01, checkout-02]
```

The `logevents` module supplies event, interval, payload-extraction and value-conversion types, but
does not register a `CounterSource`. `rlog` is a different legacy adapter: it downloads a named
whole file from `https://<host>:19888`, saves it below the Collector log directory and reports its
byte size. It is not grep-style event extraction.

To finish the desired workflow, implement match-to-value semantics (for example `1` per match or a
captured numeric value), extract or assign timestamps, batch safe repository writes, handle
rotation/reconnect deduplication, and register tests for local and SSH files. The concrete extension
steps are in [Building a source](extensions.md#build-a-collector-source).

[Next: Collector repositories →](collector-repositories.md)
