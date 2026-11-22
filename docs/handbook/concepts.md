# Concepts and Motivation

[Handbook home](README.md) · [Architecture](architecture.md) ·
[Getting started](getting-started.md) · [Collector](collector.md) · [Workbench](workbench.md)

## A recorder for the software emergency doctor

Software-EKG is an expert instrument for a concrete diagnostic assignment. Like a medical
recorder, it preserves signals over time so that an experienced analyst can compare them. It does
not decide in advance which signals must matter in every incident: the relevant metrics may be
heap and garbage collection in one case, connection pools, operating-system values, log events or
business counters in another.

The investigation proceeds from coarse to fine. The analyst first overlays broad, comparable
signals from many systems to find which system and period differ. That observation determines the
next, more specific metrics, processes and time range. Software-EKG supports this repeated
narrowing; a profiler or trace tool can take over once the affected component and period are known.

This is why Software-EKG complements monitoring rather than replacing it. Monitoring continuously
answers whether known conditions are healthy and raises alarms. The recorder helps an expert ask
what is different in this particular case, including patterns that remain inside every alarm
threshold.

## Why collect metrics that already exist somewhere?

Monitoring platforms such as Prometheus and Grafana, Zabbix and similar systems can provide broad,
well-designed monitoring of a whole environment. An investigation nevertheless has a different
scope. A long-running monitoring system either collects a great volume of data, or deliberately
omits the specialised exporter, agent item, JVM attribute, log event or high-frequency value that
only becomes relevant to one incident. Software-EKG reuses the useful monitoring series, adds those
missing pieces from sources such as `top`, JMX, logs or the CSV files of an earlier collection,
and limits the result to the current analysis scope. For the time of the investigation it can
therefore sample deeper, more precisely or faster than would be sensible for permanent
monitoring.

Software-EKG copies the selected evidence into a common time-series model. That is useful because
an investigation asks cross-source questions: Did heap growth begin before the login rate fell?
Did the same transaction error occur on every node? Was a release marker followed by I/O pressure?
The value is not another copy by itself; it is a stable, comparable investigation set.

The Collector deliberately gathers only named metrics and patterns. It is an adapter between the
systems that already expose facts and the repository the Workbench can analyse. The original
monitoring remains authoritative and continues to alert. Software-EKG supplies a focused case file.

## From a bookmark to the report

During the investigation a [bookmark](workbench.md#bookmarks) preserves the decisive view: the
filters that select its series, and the time range. It stores the query, not a picture, so the
view can be opened again and changed. That is intentionally lighter than a finding database. The final result usually has to fit
an existing presentation, incident report or customer document, so document authoring remains
outside the Workbench.

The picture of the bookmarked chart and an optional export of its data, both made with the
[export](workbench.md#export) of the chart, are the evidence handed to that next step. An AI can use them to draft slides or prose. The screenshot communicates the pattern; series
names, filters, time range and exported values should accompany it whenever the exact identity or
value matters. This division keeps Software-EKG focused on recording and diagnosis while leaving
communication to the tool appropriate for the audience.

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

This remains valuable in an AI-assisted investigation. An AI can explore faster and test more
hypotheses, but it still needs controlled, structured evidence instead of disconnected dashboards
and screenshots. Software-EKG builds that investigation dataset; the analyst and the AI Assistant
can then compare systems and verify hypotheses against the same reproducible time series. The
assistant itself is not offline: what it is shown goes to the service of its model, see
[AI Assistant](workbench.md#ai-assistant).

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
metrics, and a kind of metric is wanted for several kinds of servers: JVM metrics and log events
for the application servers, operating-system metrics and log events for the other backend
services. Each combination is one collector entry, and the Collector multiplies it out into one
job for every host and every process of the groups. Three metric groups, two host groups and
four collector entries cover every server of both kinds; a server that is added later is one
line in its group.

The groups are also what the analysis compares by. A host group holds servers for which a
common chart makes sense, and its name and the names of the process group and the metric group
are dimensions of every series. The worked example is in the
[Collector guide](collector.md#many-groups-multiplied-out).

The Workbench then treats all resulting series alike: explore dimensions, select a time range,
overlay series, aggregate, bookmark and export.

## What makes this model different

Software-EKG combines a pluggable source pipeline with a comparison-oriented collection model.
Metric, process and host groups are defined independently and expanded into concrete collection
jobs. Every series keeps its groups as dimensions for later analysis: the group of its host, of
its process and of its metrics, beside the host, the process and the metric themselves.

Other collectors commonly provide source plugins or host templates. Software-EKG connects both
ideas to build a focused, heterogeneous and offline investigation dataset.

> Define independently what to measure, where to measure it and what to compare.

## From measurements to temporal events

Numbers are not the only useful time series. A matching log line can be represented as an event or
as a count per time bucket. That makes sequences visible: error `TX-41` on a gateway, then `TX-72`
in a service, then a database rollback. The same technique shows logins per minute, retries,
deployments or any fact that an operator could otherwise obtain with `tail` and `grep`.

Automating this in the Collector matters because manual shell sessions are not repeatable, lose
timestamps, and rarely cover all nodes consistently. It also closes a gap that no metrics server
closes afterwards: what an application only writes into its log was never a metric anywhere.

The source `log` does this. A metric is a regular expression, and its series is the number of
matching lines per interval, or an aggregate of the numbers they hold. Every line counts at the
time of its own timestamp, so that content a file already has and lines written during an outage
end up where they belong. See [Log files and events](collector-sources.md#log-files-and-events).

[Next: Architecture →](architecture.md)
