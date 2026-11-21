# Concepts and Motivation

[Handbook home](README.md) · [Getting started](getting-started.md) · [Collector](collector.md) ·
[Workbench](workbench.md)

## Why collect metrics that already exist somewhere?

Operational evidence is usually scattered. CPU data may be available through `top`, JVM state
through JMX, request rates in Prometheus, business events in a log file and an old incident in a
CSV archive. Each source already has the data, but none provides the whole comparison.

Software-EKG copies the selected evidence into a common time-series model. That is useful because
an investigation asks cross-source questions: Did heap growth begin before the login rate fell?
Did the same transaction error occur on every node? Was a release marker followed by I/O pressure?
The value is not another copy by itself; it is a stable, comparable investigation set.

The Collector deliberately gathers only named metrics and patterns. It is an adapter between the
systems that already expose facts and the repository the Workbench can analyse. The original
monitoring remains authoritative and continues to alert. Software-EKG supplies a focused case file.

## Why work offline?

Production access is slow, sensitive and often temporary. Repeating wide historical queries can
load monitoring servers, and an incident environment may disappear before the analysis is done.
Local collection or import gives the analyst:

- a reproducible snapshot that does not change beneath the investigation;
- fast interactive filtering and comparison without repeatedly querying production;
- the ability to work after access has expired or while disconnected;
- an explicit boundary for credentials and sensitive data;
- a portable artefact that can be archived with the incident, subject to local data policy.

Offline does not mean isolated forever. A Workbench project may either contain copied data or use a
split source such as Prometheus, and a chart can follow incoming data. The local-project workflow is
the safest default for forensic work; a remote project is useful when freshness matters more than a
frozen snapshot.

## Why build adapters?

Every organisation has a useful interface that a general monitoring product does not understand:
a proprietary status endpoint, a vendor command, an appliance counter or a domain-specific log.
Forcing that source through a large observability migration is disproportionate when an incident
needs five numbers today.

A Collector source is intentionally small: it translates a source-specific metric name into a
number and is discovered through Java `ServiceLoader`. A repository adapter translates collected
rows into storage. Workbench importers translate files or remote APIs into the EKG dimension model.
These narrow seams keep source-specific code out of the analysis engine and make integrations easy
to test. See the [extension guide](extensions.md).

## The data model

A time series is identified by dimensions. Classic infrastructure data normally uses host group,
host, process group, process, measurement, metric group and metric. Cloud-native data can add
namespace, service, pod and container. A project selects a view flavour and can rename dimensions
for a domain—for example an importer can display *Country* instead of *Host*.

## Groups, multiplied out

The Collector configuration is built from groups, and there are several of each kind:

1. `hostGroups` put servers together that are alike. That can be as simple as "the Windows
   servers", or it follows the architecture: the frontend servers, the backend servers.
2. `metricGroups` put metrics of one kind together, read through one source: the JVM metrics
   over JMX, the operating-system metrics from `top`, the events of a log.
3. `processGroups` name the processes or endpoints the metrics are read of.
4. `collectors` combine one group of each kind and say **how often** the combination is read.

The multiplicity is the construction principle. A kind of server needs several kinds of
metrics, and a kind of metric is wanted for several kinds of servers: JVM and operating-system
metrics for the application servers, operating-system metrics and disk I/O for the other backend
services. Each combination is one collector entry, and the Collector multiplies it out into one
job for every host and every process of the groups. Three metric groups, two host groups and
four collector entries cover every server of both kinds; a server that is added later is one
line in its group.

The groups are also what the analysis compares by. A host group holds servers for which a
common chart makes sense, and its name and the name of the metric group are dimensions of every
series. The worked example is in the
[Collector guide](collector.md#many-groups-multiplied-out).

The Workbench then treats all resulting series alike: explore dimensions, select a time range,
overlay series, aggregate, bookmark and export.

## From measurements to temporal events

Numbers are not the only useful time series. A matching log line can be represented as an event or
as a count per time bucket. That makes sequences visible: error `TX-41` on a gateway, then `TX-72`
in a service, then a database rollback. The same technique shows logins per minute, retries,
deployments or any fact that an operator could otherwise obtain with `tail` and `grep`.

Automating this in the Collector matters because manual shell sessions are not repeatable, lose
timestamps, and rarely cover all nodes consistently. The present repository contains the beginning
of this pipeline (`log`, `rlog`, and a `logevents` parsing model), but not a complete supported
log-event-to-chart path. The exact status and a safe implementation target are documented under
[Log files and events](collector-sources.md#log-files-and-events).

[Next: Getting started →](getting-started.md)
