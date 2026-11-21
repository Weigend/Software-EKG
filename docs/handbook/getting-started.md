# Getting Started

[Handbook home](README.md) · [Concepts](concepts.md) · [Architecture](architecture.md) ·
[Collector](collector.md) · [Workbench](workbench.md)

## Install a release

Download a release from the
[release page](https://github.com/Weigend/Software-EKG/releases/latest) and unpack it. There are
packages for Linux x64, macOS Apple Silicon and Windows x64, each with its own Java runtime.

A package built locally without the `dist` Maven profile needs an installed Java 25. The
Workbench takes JavaFX from that runtime if it has it and otherwise from the jars in
`lib/javafx`, which fit the platform of the build. The Workbench also needs a desktop session,
on Linux with GTK 3.

| Application | Linux / macOS | Windows |
| --- | --- | --- |
| Workbench | `bin/software-ekg.sh` | `bin\software-ekg.bat` |
| Collector | `./ekg-collector.sh` | `ekg-collector.cmd` |
| Collector in the background | `./ekg-collector start\|stop\|restart\|status` | see [service operation](collector.md#background-and-service-operation) |

The Unix launchers check what they need and can run only that check. The Windows launchers have
no such option:

```bash
bin/software-ekg.sh --check
./ekg-collector.sh --check
```

On macOS, a browser download may be quarantined because the package is unsigned:

```bash
xattr -dr com.apple.quarantine /path/to/unpacked/software-ekg
```

## Run from this repository

```bash
# Build the relevant modules and start them
./run-workbench.sh
./run-collector.sh

# Reuse the last build
./run-workbench.sh --no-build
./run-collector.sh --no-build
```

The no-argument Collector configuration uses the `mock` source and creates changing sample values.
It is the quickest health check and needs no monitored system.

## The shortest way to a first chart

The Workbench contains a Collector of its own. A first analysis therefore needs neither a
separate Collector nor an import:

1. Start the Workbench and choose **Analysis → New Analysis...**.
2. Choose what the analysis is about. **Linux / Unix**, **Windows Server** and **Java / JVM** can
   be chosen today; the other topics are shown greyed out. Windows Server needs a Workbench
   running on Windows.
3. Choose how the data comes: through a Prometheus server that already collects from the
   systems, or directly from each host (SSH and `top` for Unix, performance counters for
   Windows).
4. Say where the data is: the address of the Prometheus server, whose systems are then found and
   offered for selection, or the hosts to read directly.
5. Finish the dialog. The first chart opens as soon as the first values are there and follows
   the data from then on.

The Workbench collects for as long as it runs. Details are in
[Data Sources and New Analysis](workbench.md#data-sources-and-new-analysis). The rest of this
page describes the other way: a standalone Collector whose data is imported afterwards.

## Make a first real collection

Copy `config/current/config-template.yaml` from the Collector distribution. The following minimal
configuration samples CPU user time and load from two Unix hosts over SSH:

```yaml
project: First investigation

metricGroups:
  host-health:
    type: unix
    metrics:
      - '\CPU\us'
      - '\Load\one'

processGroups:
  operating-system:
    - name: global
      source: '${REF:hostAddress}:22'

hostGroups:
  database-nodes:
    username: ekg
    password: '${ENV:EKG_SSH_PASSWORD}'
    hosts:
      - db-01.example.org
      - db-02.example.org

collectors:
  - metricGroup: host-health
    processGroup: operating-system
    hostGroup: database-nodes
    interval: 15s
```

This is the smallest case: one group of each kind and one collector. A real configuration has
several host groups and metric groups and combines them; see
[Many groups, multiplied out](collector.md#many-groups-multiplied-out).

Keep secrets outside YAML and start the Collector in the foreground:

```bash
export EKG_SSH_PASSWORD='replace-me'
./ekg-collector.sh /path/to/first-investigation.yaml
```

The repository is selected in `config/collector.properties`; the shipped default is CSV. The
Collector then writes its files to `logs/ekg/<start time>/` in the distribution, or below the log
directory given as second argument. CSV is convenient for an air-gapped hand-off; Solr lets the
Workbench query a repository directly. See [Collector repositories](collector-repositories.md).

## Chart the collected data

1. Start the Workbench and create or select a project.
2. If the Collector wrote CSV, choose **Import → EKG Collector Logs** and select the files from
   `logs/ekg/<start time>/`. If the data is in Prometheus, choose **Import → Prometheus Data**.
3. Open **Window → New Chart** if no chart is visible.
4. Expand the explorer or use the filter header to select hosts, processes and metrics.
5. Set the time range, then overlay comparable series. Zoom by selecting a range in the chart.
6. Save a useful query as a bookmark; export evidence as PNG or Excel from the chart controls.

The Workbench stores metadata in `~/.ekg/metadata.mv.db`, embedded series under `~/.ekg/solr`,
secrets under `~/.ekg/secrets`, and launcher logs under `~/.ekg/logs`. These locations survive a
source-tree clean.

## Build from source

```bash
./install-wfx.sh
mvn clean package
xvfb-run -a mvn test             # Linux without a real display
mvn package -DskipTests -Pdist   # self-contained platform-specific ZIPs
```

[Next: Collector →](collector.md)
