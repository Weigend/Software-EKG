# Collector Repository Modules

[Handbook home](README.md) · [Collector](collector.md) · [Source modules](collector-sources.md) ·
[Extensions](extensions.md)

A Collector writes everything it collects through one repository. The factories are loaded with
Java `ServiceLoader`; the one named in `collector.properties` becomes the destination of every
job. The distribution contains CSV, archive, Solr and Elasticsearch implementations. A JSON
repository exists in the source tree but is not part of the distribution.

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

| Repository | Best fit | Trade-off |
| --- | --- | --- |
| CSV | portable incident bundle; later Workbench import | simple and inspectable, not interactive |
| Archive | rotated/compressed CSV hand-off | compact but requires extraction/import |
| Solr | direct Workbench analysis and large sets | operates a search index |
| Elasticsearch | existing remote search infrastructure | version/cluster compatibility matters |

## CSV and archive

CSV writes the Collector interchange format understood by **Import → EKG Collector Logs**. The
files go to `<log directory>/ekg/<start time>/`, with the start time of the Collector written as
`yyyy-MM-dd-HH-mm-ss`. The log directory is `logs` in the distribution unless the launcher is
given another one.

The files rotate. Two keys in `collector.properties` control that:

```properties
Csv4jRepository.MaxFileSize=30MB
Csv4jRepository.MaxBackupIndex=3
```

These are the defaults. The extended format retains cloud dimensions; the Workbench importer also
accepts historical pre-6.1 files.

The archive repository builds on the same row representation and groups/compresses output for
transport. Rolling and archive directory patterns default to `%d{yyyy-MM-dd}` and
`yyyy-MM-dd-HH`. Their keys are `io.softwareecg.collector.source.rollingPolicyPattern` and
`io.softwareecg.collector.source.archiveDirectoryPattern`; override them only after verifying the
resulting paths.

## JSON

The JSON repository (`collector/repositories/json`) is a rotating local repository for
machine-to-machine processing and tests. The Collector distribution does not contain it, and the
Workbench has no JSON import. To use it, build the module, add its jar with its dependencies to
the `lib` directory and select
`io.softwareecg.collector.repository.json.JsonRepositoryFactory`. Its rotation keys are
`Json4jRepository.MaxFileSize` and `Json4jRepository.MaxBackupIndex`. Use CSV for a supported
file hand-off.

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

## Elasticsearch

The Elasticsearch repository implements the same relational/time-series repository contract and
uses the same general RDB controls for commits, compaction and retention. It targets the client
generation pinned by this build. Verify server compatibility in a staging index before pointing a
Collector at a managed cluster.

## Operational safeguards

- Put the project name in YAML so imported/direct series land in the intended investigation.
- Test rotation and disk capacity before a long collection.
- Treat repository passwords as secrets; do not commit filled property files.
- Use a short retention only for a continuously operated repository, not an incident snapshot.
- Stop a test Collector before changing repositories; otherwise two runs can create confusing
  duplicate time grids.

[Next: Workbench →](workbench.md)
