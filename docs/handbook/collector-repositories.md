# Collector Repository Modules

[Handbook home](README.md) · [Collector](collector.md) · [Source modules](collector-sources.md) ·
[Extensions](extensions.md)

A Collector writes everything it collects through one repository. The factories are loaded with
Java `ServiceLoader`; the one named in `collector.properties` becomes the destination of every
job. The distribution contains CSV, archive, Solr and Elasticsearch implementations.

CSV and archive repositories write files to the local filesystem of the machine on which the
Collector is running. Solr and Elasticsearch repositories are remote clients: they send collected
data to a repository server. Therefore CSV/archive require local disk space on the Collector host,
whereas Solr/Elasticsearch require network access and storage capacity on the remote server.

## Selecting a repository

One line of `config/collector.properties` selects the factory by its class name:

```properties
io.softwareecg.collector.repository.RepositoryFactory=io.softwareecg.collector.repository.csv.CsvRepositoryFactory
```

| Repository | Factory class |
| --- | --- |
| CSV (the shipped default) | `io.softwareecg.collector.repository.csv.CsvRepositoryFactory` |
| Archive | `io.softwareecg.collector.repository.archive.ArchiveRepositoryFactory` |
| Solr | `io.softwareecg.collector.repository.rdb.solr.SolrRepositoryFactory` |
| Elasticsearch | `io.softwareecg.collector.repository.rdb.elasticsearch.ElasticSearchRepositoryFactory` |

Always set the line. Without it the Collector takes the first factory `ServiceLoader` finds,
which is not a choice to rely on. A class name that is not registered stops the Collector with a
configuration error.

Solr and Elasticsearch read their connection from a second file,
`remote-repository-config.properties`.

### Where the property files are found

The Collector looks for `collector.properties` in this order and takes the first it finds:

1. the file, or the directory containing it, named by the JVM property
   `-DcollectorPropertiesPath=...`;
2. the same, named by an environment variable `collectorPropertiesPath`;
3. beside the configuration YAML;
4. `./config/` below the working directory;
5. the classpath, which with the shipped launchers is the `config` directory of the distribution;
6. the working directory.

`remote-repository-config.properties` is found the same way; its property and environment
variable are called `collectorRepositoryPropertiesPath`. Because of step 3, a configuration can
bring property files of its own, as `config/cluster-monitoring` does.

Unlike Collector YAML, Java `.properties` files do not expand `${ENV:...}` references. Protect a
file with a password or generate it securely at deployment time.

## Choosing storage

| Repository | Storage location | Best fit | Trade-off |
| --- | --- | --- | --- |
| CSV | local filesystem of the Collector host | portable incident bundle; later Workbench import | simple and inspectable; a file per day, week or month, of a limited size |
| Archive | local filesystem of the Collector host | compressed daily files in the older format | grouped by day, week or month; no limit of size |
| Solr | remote Solr server | direct Workbench analysis and large sets | operates a search index |
| Elasticsearch | remote Elasticsearch server | existing remote search infrastructure | version/cluster compatibility matters |

## CSV and archive

Both write the values of a job (a host, a process and a metric group) into files of its own on
the Collector host. The CSV repository is the one to use; the archive repository is the older
way to get a file per day.

### CSV: a file per period, of a limited size

CSV writes the Collector interchange format understood by **Import → EKG Collector Logs**. The
files go to `<log directory>/ekg/<start time>/`, with the start time of the Collector written as
`yyyy-MM-dd-HH-mm-ss`. The log directory is `logs` in the distribution unless the launcher is
given another one. Every start of the Collector thus has a directory of its own.

The files of a job tell by their names what time they hold:

```text
db-01_global_os_2026-11-20.csv        the day before
db-01_global_os_2026-11-21.csv        today
db-01_global_os_2026-11-21_2.csv      ... and what did not fit into the first file of today
```

Three keys in `collector.properties` control that. These are the defaults:

```properties
io.softwareecg.collector.source.rollingPolicyPattern=%d{yyyy-MM-dd}
Csv4jRepository.MaxFileSize=30MB
Csv4jRepository.MaxBackupIndex=0
```

| Key | Effect |
| --- | --- |
| `rollingPolicyPattern` | The period a file holds, as a date pattern in `%d{...}`. `%d{yyyy-MM-dd}` is a day, `%d{YYYY-'W'ww}` a week from Monday to Sunday, `%d{yyyy-MM}` a month, `%d{yyyy-MM-dd-HH}` an hour. `none` turns files by time off. |
| `MaxFileSize` | What does not fit into a file goes into a further file of the same period, numbered `_2`, `_3` and so on. Units are `KB`, `MB` and `GB`. |
| `MaxBackupIndex` | How many files of a job are kept besides the one that is written. `0` keeps all. With a number, the oldest files of the job are deleted. |

- A row goes into the file of **its own time**, not of the moment it is written. What a source
  delivers of yesterday, with a `lookback` for example, is in the file of yesterday. Periods
  begin and end in the time zone of the Collector.
- Every file begins with the header and can be read by itself. The header names the job: its
  project, host group and host, process group and process, and metric group. The process group
  is new in it; a Workbench of an earlier version than this Collector does not read such a file.
- Nothing is renamed, and with the defaults nothing is deleted. A Collector that runs for long
  needs room for it: at most `MaxFileSize` per file, but any number of files. Set
  `MaxBackupIndex` if the directory must not grow without bound, and know that the oldest values
  are gone then.
- If the series of a job change, which happens with wildcards and `expandInterval`, the rows no
  longer fit the header. The job then continues in a further file of the period, with the new
  header.
- A value that is missing in a row leaves its column empty.
- A setting that is not usable ends the job with the reason.

The extended format retains cloud dimensions; the Workbench importer also accepts historical
pre-6.1 files.

### Archive: a file per day

The archive repository writes compressed files, `<log directory>/ekg/current/<name>.csv.gz`, in
the classic format with a date column. Every midnight it moves each file into an archive folder
and begins a new one:

```text
logs/ekg/current/db-01_global_os.csv.gz     today, being written
logs/ekg/archiv/20261120.zip                yesterday: db-01_global_os_2026.11.21_00.00.00.csv.gz ...
logs/ekg/archiv/20261119.zip                the day before
```

A file in the archive carries the time it was moved there. The folder is named by a date
pattern, and it is packed into a ZIP as soon as the next folder begins:

```properties
io.softwareecg.collector.source.archiveDirectoryPattern=yyyyMMdd
```

`yyyyMMdd`, as the shipped file sets it, gives a ZIP per day. `yyyy-'W'ww` gives a folder per
week that fills with seven daily files and is packed when the week is over, `yyyy-MM` one per
month. The files are always cut at midnight; the
pattern only decides how they are grouped. Nothing is deleted. Files left in `current` by an
earlier run are moved to the archive when the Collector starts.

The archive repository has no limit of size, and its files are in the older format without the
metadata of a job in their header. For new collections the CSV repository does both.

## Solr

Solr stores EKG measurement documents and supports compaction, retention and direct query from the
Workbench. Relevant RDB properties include:

```properties
io.softwareecg.collector.repository.rdb.databaseUrl=http://localhost:8983/solr
io.softwareecg.collector.repository.rdb.repositoryName=ekgdata
io.softwareecg.collector.repository.rdb.user=admin
io.softwareecg.collector.repository.rdb.password=replace-with-a-deployment-provided-secret
io.softwareecg.collector.repository.rdb.autoCreateNoExistingRepository=false
io.softwareecg.collector.repository.rdb.compactTimeSeries=true
io.softwareecg.collector.repository.rdb.compactIntervalStrategy=DailyCompactStrategy
io.softwareecg.collector.repository.rdb.commitMaxUpdateEvents=100
io.softwareecg.collector.repository.rdb.commitTimeInterval=01:00:00
io.softwareecg.collector.repository.rdb.explicitCommit=true
io.softwareecg.collector.repository.rdb.dataMaxAgeInDays=40
```

These are the keys the code reads, with its defaults. The shipped
`remote-repository-config.properties` sets other values for some of them, for example a commit
every minute.

Set auto-create deliberately: convenient in development, surprising in production. The
alternative compaction strategy is `FiveMinuteCompactStrategy`. Cluster mode uses `isClusterMode`
and `clusterManager` (default `localhost:9881`); for Solr the cluster manager is the ZooKeeper
address.

The Workbench includes embedded Solr under `~/.ekg/solr`. That store is ideal for offline projects;
an external Solr is appropriate when Collector and Workbench must share a long-lived repository.

Every record names the job it comes from: project, host group and host, process group and
process, metric group and metric. The process group is written as the field `ts_processGroup`.

- The collection needs that field in its schema. The schema that comes with the Workbench
  (`solr/ekgdata/conf/schema.xml`) has it. A collection set up from an older schema rejects the
  records; the job then shows a storage error with the reason and keeps trying.
- Records written by an earlier Collector have no process group. In the Workbench a series that
  is continued by this Collector therefore begins anew at that point, below its process group.
- Records of one series are merged by a key over these names. The process group is part of it,
  so records written before and after the change of version are not merged with each other.

## Elasticsearch

The Elasticsearch repository implements the same relational/time-series repository contract and
uses the same general RDB controls for commits, compaction and retention. It targets the client
generation pinned by this build. Verify server compatibility in a staging index before pointing a
Collector at a managed cluster.

The process group is written here as well, as `ts_processGroup`. An index the Collector creates
knows it as a keyword, like the other dimensions. An index created by an earlier Collector gets
the field added by Elasticsearch on the first record, with the mapping Elasticsearch chooses for
texts; create a new index if the process group is to be filtered exactly. What is said above
about records of an earlier Collector holds here too.

## Operational safeguards

- Put the project name in YAML so imported/direct series land in the intended investigation.
- Test rotation and disk capacity before a long collection.
- Treat repository passwords as secrets; do not commit filled property files.
- Use a short retention only for a continuously operated repository, not an incident snapshot.
- Stop a test Collector before changing repositories; otherwise two runs can create confusing
  duplicate time grids.

[Next: Workbench →](workbench.md)
