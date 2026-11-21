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

The Collector configuration mirrors the question:

1. `metricGroups`: **what** should be measured and through which source adapter?
2. `processGroups`: **of which process or endpoint** should it be read?
3. `hostGroups` or `platform`: **on which systems**?
4. `collectors`: **how often** should a combination run?

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
