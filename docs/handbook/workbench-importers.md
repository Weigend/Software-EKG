# Workbench Importers

[Handbook home](README.md) · [Workbench](workbench.md) · [Extensions](extensions.md)

Importer modules appear alphabetically in the **Import** menu and, when supported, in a project's
context menu. An importer declares whether it supports local projects, split/remote projects,
classic or cloud views, and specialised dimension aliases.

## EKG Collector Logs

Imports CSV files written by every published Collector format, including classic pre-6.1 rows and
the extended cloud-native format. Select one or more files/directories in the dialog and import into
a local project. Use this route for an air-gapped transfer or a frozen incident snapshot.

Before import, keep all rotated parts together and do not edit delimiters or timestamps. Duplicate
input can produce duplicate evidence depending on the target repository, so import each bundle once.

## Prometheus Data

Prometheus is the most flexible importer. It supports:

- copying a selected interval into a local project;
- a split-source project that queries Prometheus remotely;
- classic and cloud-native view flavours;
- no authentication, username/password, JWT bearer token and OAuth2 client credentials;
- series filters and dimension mapping from importer configuration.

The bundled `dimension-mapping.conf` says which Prometheus label fills which EKG dimension. A line
is `DIMENSION:label1;label2;...`, and the first label a series has is used:

```text
HOST_GROUP:role
HOST:*host;instance;host;node_name
NAMESPACE:kubernetes_namespace;namespace
SERVICE:servicename;kubernetes_name;MICROSERVICEID;MICROSERVICENAME
POD:pod_name;node
CONTAINER:container_name
MEASUREMENT:job
PROCESS:job
METRIC_GROUP:*prefix
METRIC_NAME:__name__
```

`*host` takes the host name out of a `host:port` value, and `*prefix` derives the metric group
from the prefix of the metric name (`jvm_`, `node_`).

Use a local copy for reproducibility and offline work. Use split source when data volume makes a copy
impractical or the latest sample is essential. The bundled configuration area also contains
standard and regex series-filter examples (`series-filter-std.conf`, `series-filter-regex.conf`).

## OWID COVID-19 Data

Imports Our World in Data CSV or CSV.GZ datasets into a specialised local project. It maps EKG
dimensions to Continent, Country, main category and metric. This module demonstrates that EKG is a
general comparative time-series tool, not limited to software telemetry.

## Binance Market Data

Imports Binance public `aggTrades` ZIP files (the format distributed by `data.binance.vision`) into a
local project. Aliases include Exchange, Market, Quote Asset, Symbol, Period, Category and Metric.
The importer parses the files; it does not download or trade.

## Yahoo Finance Market Data

Imports CSV/JSON directory output created by a yfinance downloader. Options independently include
price history, corporate actions, intraday data, adjusted close, repaired flags and metadata-derived
dimensions. Aliases include Value Group, Value, Country, Exchange, Period and Metric Group. The
Workbench does not itself promise a live Yahoo download API through this module.

## Common import workflow

1. Create/select the target project and confirm its repository and view flavour.
2. Open the importer from **Import** or the project context menu.
3. Select files or configure the remote endpoint and authentication.
4. Limit the time interval and series set before a large import.
5. Run the import; the status bar reports background progress and offers cancellation where the
   strategy supports it.
6. Open a chart and validate series count, time zone, dimensions and one known value.
7. Save a bookmark documenting the validation query.

Importers can rename displayed dimensions. If a field looks surprising, check the project's alias
mapping before treating the imported hierarchy as wrong.

[Next: Extension guide →](extensions.md)
