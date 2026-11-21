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
| `log` | lines of a log file, on this machine or over SSH | none; reads what the file already has (`lookback`) | supported |
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

`lookback=<n><s|m|h|d>` applies to the first fill: when the storage holds no values yet, or, as
the repositories of the standalone Collector, does not tell where they end. The job interval is the query-range step;
values are aligned to that grid. Queries are windowed to at most 10,000 points and roughly 500,000
in-memory values. Set `expandInterval` so newly appearing label sets become series.

## Log files and events

### Why this source matters

Logs contain temporal facts that numeric monitoring misses. Turning matching lines into series
makes it possible to compare:

- the exact sequence of transaction error codes across services;
- logins, failed logins or retries per minute;
- deployment, restart and failover markers beside CPU or latency;
- any recurring line that an operator could find with `tail -F ... | grep ...`.

The Collector is the right place to automate that operation: it uses one definition on every
host, survives connections that break, keeps the time of every event and feeds the same
dimensions as all other measurements. It is also what cannot be had afterwards from a metrics
server: what an application only writes into its log never reached Prometheus.

### Metrics are regular expressions

The process `source` is the log file, and every metric is a Java regular expression. The series
of a metric is the **number of lines per interval** that contain it:

```yaml
metricGroups:
  checkout-events:
    type: log
    metrics:
      - metric: 'user login succeeded'
        label: logins
      - metric: 'transaction failed.*code=TX-41'
        label: transaction failed TX-41
      - metric: 'transaction failed.*code=TX-72'
        label: transaction failed TX-72
      - metric: 'request took (?<avg>\d+) ms'
        label: request time ms, average
      - metric: 'request took (?<max>\d+) ms'
        label: request time ms, slowest

processGroups:
  checkout:
    - name: checkout
      source: '/var/log/checkout/checkout-${REF:hostName}.log'
      args: 'lookback=24h'

hostGroups:
  checkout-servers:
    username: ekg
    password: '${ENV:EKG_SSH_PASSWORD}'
    hosts: [checkout-01, checkout-02]

collectors:
  - metricGroup: checkout-events
    processGroup: checkout
    hostGroup: checkout-servers
    interval: 15s
```

The expression is searched in the line; it does not have to describe all of it. A line counts
once for a metric, however often it matches. Give every metric a `label`: the expression itself
is a poor name for a series.

- Upper and lower case matter. Begin an expression with `(?i)` to ignore them:
  `'(?i)connection refused'`.
- Write expressions in single quotes, so that YAML leaves backslashes as they are.
- `wildcard` and `expandInterval` have no meaning for this source: a metric is one series, and
  the series of a job never change.
- The distribution has a complete example, `config/log/config.yaml`.

A group with one of five names turns the count into an aggregate of the numbers the group
captures:

| Metric | Series | Interval without a matching line |
| --- | --- | --- |
| `'login succeeded'` | number of matching lines | 0 |
| `'sent (?<sum>\d+) bytes'` | sum of the numbers | 0 |
| `'took (?<avg>\d+) ms'` | their average | no value |
| `'took (?<min>\d+) ms'` | the smallest | no value |
| `'took (?<max>\d+) ms'` | the largest | no value |
| `'queue length (?<last>\d+)'` | the number of the last matching line | no value |

A number may be written with a decimal point or a decimal comma. A line whose group captures
something that is not a number does not count. A metric is one series: an expression with two of
these groups is rejected. For a series per error code, write one metric per code, as above.

### Intervals

Time is cut into intervals of the length of the collector `interval`, on a fixed grid. A value
carries the start of its interval and says what happened from then until the next one. So the
`interval` is the resolution of the series: with `15s` there is one value for every 15 seconds.

An interval in which no line matched is delivered as well, with a 0 for a count. That nothing
happened is what the file says, and a chart must not draw a line across it.

- An interval shorter than one second is treated as one second.
- An interval is delivered when it is over, so a value appears up to one interval after its
  lines were written.
- The interval in which following begins is delivered too. It holds only what was written after
  the start.

In the Workbench, add the intervals up with the smoothing type **Sum** to see events per minute
or per hour.

### The time of a line

A line belongs to the interval of **its timestamp**, not to the moment it is read. That is what
places an event correctly when a file is read later: content it already had, or what was written
while a host could not be reached.

These formats are recognised without configuration:

| Written as | Typical for |
| --- | --- |
| `2026-11-21 10:15:30,123`, `2026-11-21T10:15:30.123+01:00`, `2026-11-21T09:15:30Z` | logback, log4j, most applications, JSON logs |
| `[21/Nov/2026:10:15:30 +0100]` | access logs of web servers |
| `Nov 21 10:15:30` at the start of the line | syslog. It names no year: the current one is taken, or the last one if the date would lie in the future. |

The timestamp is looked for at the beginning of a line, in its first 256 characters; a date
further into a long message is not mistaken for it.

Any other format is described with the argument `timestamp`, as a pattern of Java's
`DateTimeFormatter`, for example `timestamp=dd.MM.yyyy HH:mm:ss`. The pattern has to hold date
and time. Month names in it are English. A timestamp that names no offset is taken in the time zone of the Collector; if the
host writes in another one, say so with `zone=Europe/Berlin`.

- A line without a timestamp, such as the further lines of a stack trace, belongs where the line
  before it does.
- If the lines have no timestamps at all, they count at the time they are read.
- A timestamp more than a minute ahead of the Collector's clock is not believed, and the line
  counts at the time it is read. The Collector logs a hint: usually the `zone` is missing.
- A line whose interval was delivered already counts in the next one. Nothing is lost; the time
  is off by what the line came late.

### What the file already has

Without `lookback` the source reads what is written from the start of the job on, like
`tail -f`. With `lookback=24h` it first reads the file from its beginning and delivers everything
of the last 24 hours at the time it happened. Units are `s`, `m`, `h` and `d`.

- The lookback needs timestamps. Old lines of which nobody knows when they were written are left
  out.
- It reaches back to the first line of the current file at most. Files that were rotated away
  are not read.
- The repositories of the standalone Collector do not tell a job where their content ends. A job
  that is started again therefore reads the lookback again and delivers its intervals again. A
  CSV repository begins a new directory with every start. With Solr or Elasticsearch, which keep
  their content, use the lookback for the first fill and take it out before the Collector is
  started again.
- One value per interval is delivered for the whole period: `lookback=7d` at an interval of `1s`
  is 600,000 values per metric.

### Reading, outages and rotation

The file is not followed by a process that stays connected to it. The source remembers up to
where it read and, every interval, reads what was added. A host that cannot be reached for a
while therefore costs only the delay: afterwards reading continues where it ended, and the lines
written in between count at their own time. No line is read twice.

A file that is replaced (renamed and begun anew, or emptied) is read from the beginning of the
new one. Lines added to the old file after the last reading and before it was replaced are not
seen: at most those of one interval.

A line is evaluated when it is complete. One that is still being written waits for its end and
is counted with the next reading. A long file is read in parts of one megabyte, so catching up
with a large file takes several readings but little memory.

| Host address | How the file is read |
| --- | --- |
| `localhost` or `127.0.0.1` | directly by the Collector. Needs nothing but Java, so it works on Windows too. |
| any other | over SSH with `username` and `password` of the host. The host needs `sh`, `ls`, `wc`, `tail` and `head`, as every Linux has them. The login needs nothing but the right to read the file. |

Over SSH every job runs one short command per interval. Use `${REF:hostName}` if the name of
the host is part of the name of the file; `${HOSTNAME}`, as earlier versions wrote it, still
works.

### Arguments

The `args` of the process, separated by **semicolons**, all optional:

| Argument | Meaning | Default |
| --- | --- | --- |
| `lookback=24h` | also read what the file already has of that period | only what is written from now on |
| `timestamp=dd.MM.yyyy HH:mm:ss` | the pattern of the timestamps | the usual formats are recognised |
| `zone=Europe/Berlin` | the time zone of timestamps that name none | the one of the Collector |
| `charset=ISO-8859-1` | the character set of the file; over SSH the host converts it with `iconv` | UTF-8 |
| `port=2222` | the SSH port | 22 |

An argument that is not known is an error, like an unknown key of the configuration.

### When something is wrong

- A file that does not exist or may not be read, and a host that cannot be reached, are shown in
  the state of the job with the reason. The job keeps trying, with growing pauses.
- A metric that is not a regular expression, an unknown argument and a missing SSH login end the
  job with the reason.

### Limits

- One series per expression. A series for every value a group captures (every error code that
  occurs) is not built; write one metric per value.
- The text of the lines is not stored, only what they add up to.
- Lines are evaluated one by one. An event of several lines is found by an expression for one of
  its lines.
- SSH logins use a password, as the other sources do.
- The source is part of the standalone Collector. **New Analysis** in the Workbench does not
  offer it; its series reach the Workbench through **Import → EKG Collector Logs** or a shared
  Solr repository.

The `logevents` module in the source tree is something else: a model of events and intervals
that is not registered as a source and not shipped. `rlog` is a legacy adapter that downloads a
whole file from `https://<host>:19888` and reports its size; it does not look into the file.

[Next: Collector repositories →](collector-repositories.md)
