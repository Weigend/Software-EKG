# Bestandsaufnahme: Was im Workbench-SDK SPI ist und was nicht

Status: reine Bestandsaufnahme, nichts davon ist umgesetzt oder beschlossen  
Stand: 23.11.2026, Commit `136ebdd`  
Geltungsbereich: `workbench`, Paket `io.softwareecg.awb.sdk` und die Repository-Verträge außerhalb davon

## 1. Einordnung

Dieses Dokument steht für sich. Es gehört nicht zum `workbench-refactoring-plan.md`; jener Plan
wird nicht umgesetzt, weil er als Ganzes zu groß und zu prozesslastig ist. Stattdessen wird in
kleinen, einzeln abgeschlossenen Schritten gearbeitet.

Die Ziele, an denen sich jeder Schritt messen lassen muss:

1. das Over-Engineering im Produktkern zurückbauen;
2. eine klare Komponenten- und Erweiterungsarchitektur erreichen;
3. Zuständigkeiten bündeln, statt sie über Pakete zu verteilen.

Für das SDK heißt das: so klein wie möglich, aber mit allen SPI-Interfaces, die man zum Bau eines
Importers oder eines neuen Repositories braucht. Was nur Hilfsklasse ist, gehört nicht hinein.

## 2. Vorgehen und Grenzen der Auswertung

Für jede der 89 Klassen unter `sdk` (rund 8.300 Zeilen) wurde ermittelt, welche anderen Pakete sie
verwenden und wer ihre Interfaces implementiert. Die Auswertung liest Imports und Namen im
Quelltext von `src/main/java`, nicht den Bytecode. FXML-Verweise wurden gesondert geprüft.

Daraus folgt: Die Zuordnung ist belastbar, aber vor jedem Löschen ist einmal zu kompilieren und die
Testsuite auszuführen.

Zwei Eigenschaften des heutigen Zustands sind für alles Weitere wichtig:

- `sdk` importiert kein anderes Workbench-Paket. Es ist das unterste Paket.
- `common` hängt von `sdk` ab (`ExplorerUpdateEvent`, `AwbErrorEvent`, `EkgLogger`). Eine Klasse,
  die das SDK selbst braucht, kann deshalb nicht nach `common` wandern.

## 3. Ergebnis im Überblick

| Gruppe | Klassen | Bedeutung |
| --- | ---: | --- |
| Importer-SPI und Host-API | 65 | gehört ins SDK |
| Utils, die nur andere Features nutzen | 10 | können gezielt zu ihrem Nutzer |
| Utils, die das SDK selbst nutzt | 8 | bleiben oder brauchen ein Paket unter dem SDK |
| ohne Verwender | 6 | Löschkandidaten |

Das SDK ist also zu gut 70 Prozent bereits Importer-SPI. Der eigentliche Mangel liegt woanders:
Die Verträge für Repositories liegen gar nicht im SDK (Abschnitt 5).

## 4. Die 89 Klassen im Einzelnen

### 4.1 Gehört ins SDK (65)

**`awbapi` vollständig (23).** Entweder implementiert es ein Importer, oder die Workbench bietet es
den Importern an.

| Paket | Klassen | Rolle |
| --- | --- | --- |
| `awbapi.explorer` | `ContextMenuAware`, `ContextMenuEntry`, `ImporterTreeIconProvider` | vom Importer implementiert |
| `awbapi.explorer` | `TreeIconProviderPluginRegistry`, `ExplorerUpdateEvent` | von der Workbench bereitgestellt |
| `awbapi.project` | `Project`, `ProjectSelector`, `TargetProjectConfig`, `ProjectTimeSeriesType`, `ProjectFlavor`, `ProjectViewFlavor`, `CloudPlatformType` | von der Workbench bereitgestellt, Projektmodell |
| `awbapi.repository` | `AliasMappingProvider`, `AliasMappingDefinition`, `FilterDimension` | vom Importer implementiert oder benutzt |
| `awbapi.repository` | `Repository`, `RepositorySelector`, `SeriesImportService`, `SourceRepositoryService`, `ImporterSourceRepository`, `ResourceAuthType`, `RepositoryException`, `RepositoryDataEvent` | von der Workbench bereitgestellt |

**`datamodel` (4).** `RawSeriesData`, `SeriesData`, `Value`, `TimeSeries`.

**Vertrag des Importer-Frameworks (23).** Das, was ein Importer erweitert, implementiert oder
zurückgibt:

- `importer.ImportModule`;
- `importer.api`: `LocalProjectSupport`, `SplitSourceProjectSupport`, `ClassicViewFlavorSupport`,
  `CloudViewFlavorSupport`, `RemoteSeriesDataFetcher`;
- `importer.tasks`: `LocalProjectImportStrategy`, `SplitSourceProjectImportStrategy`,
  `LocalProjectImportTask`;
- `importer.ui.dialogs`: `ImportDialog`, `FileImportDialog`, `FileImportDialogController`,
  `QueryImportDialog`;
- `importer.tasks.steps`: `SeriesFilter`, `SeriesDeltaFilter`, `MetadataDeltaFilter`,
  `RawToSeriesDataMapper`, `RawToMetadataMapper`, `ComputeSeriesProcessor`,
  `ComputeMetadataProcessor`, `SeriesPayloadReader`, `SeriesWriter`, `SeriesMetaWriter`.

**Interna des Importer-Frameworks (12).** Kein Importer fasst sie an, aber die Basisklassen oben
hängen an ihnen. Sie können das SDK deshalb nicht verlassen, ohne dass das SDK nach oben abhängt.

- `importer.tasks`: `ImportTask`, `RemoteProjectImportTask`;
- `importer.ui.controls`: `ProjectSelectorProxy`, `RepositorySelectorProxy` (nur aus FXML benutzt),
  `LabeledFileSelector` mit Controller und Model, `QuerySourceSelector` mit Controller und Model;
- `importer.ui.dialogs.queryimport.QueryImportDialogController`;
- `importer.ui.skins.DefaultImportTreeIconProvider`.

Ein Unterpaket `sdk.importer.internal` würde sichtbar machen, dass dies kein Vertrag ist.

**Aus `core` (3).** `NamedEnum`, `AwbErrorEvent`, `CancellableTaskEvent`.

Beobachtung zum Zuschnitt: Die Importer-SPI besteht aus Basisklassen mit JavaFX, nicht aus
Interfaces. `ImportModule` hat 331 Zeilen, `ImporterTreeIconProvider` ist ein Interface mit 262
Zeilen. Ein SDK aus reinen Interfaces wäre ein Neuentwurf, kein Verschieben.

### 4.2 Utils, die nur andere Features nutzen (10, rund 1.050 Zeilen)

Weder ein Importer noch das Importer-Framework verwendet diese Klassen. Sie können zu ihrem
einzigen Nutzer.

| Klasse | Zeilen | Nutzer | naheliegendes Ziel |
| --- | ---: | --- | --- |
| `datamodel.Counter` | 103 | metricanalyzer | `metricanalyzer` |
| `core.DateValuePairMapper` | 122 | repository, metricanalyzer | `repository` |
| `core.util.TaskUtils` | 125 | explorer, metricanalyzer | `common.ui` |
| `core.events.ChangedEvent` | 54 | repository | `repository.api.events` |
| `core.security.SecurityContext` | 65 | importer.prometheus | `importer.prometheus` |
| `core.security.RequestSecHeaderEnricher` | 53 | importer.prometheus | `importer.prometheus` |
| `core.security.oauth.OAuthToken` | 83 | nur `core.security` | `importer.prometheus` |
| `core.security.oauth.OpenIdConnectClientCredentialFlow` | 214 | nur `core.security` | `importer.prometheus` |
| `core.config.EkgConfigStore` | 204 | importer.prometheus | `importer.prometheus` |
| `core.config.ModuleType` | 26 | importer.prometheus, `EkgConfigStore` | `importer.prometheus` |

Nebenwirkung: Apache HttpClient und Jackson werden im SDK nur von `core.security` benutzt. Mit
diesen vier Klassen verschwinden beide Abhängigkeiten aus dem SDK.

### 4.3 Utils, die das SDK selbst nutzt (8, rund 800 Zeilen)

| Klasse | Zeilen | Nutzer außerhalb des SDK |
| --- | ---: | --- |
| `core.log.EkgLogger` | 44 | fast alle Pakete, rund 50 Dateien |
| `core.lookup.ServiceDiscovery` | 36 | datasource, metricanalyzer, project, repository |
| `core.di.DiInjection` | 117 | keine |
| `core.resourceloader.CdiFxmlLoader` | 102 | importer.csv, metricanalyzer, project, repository |
| `core.resourceloader.ViewLoadResult` | 68 | metricanalyzer, project, repository |
| `core.resourceloader.CachingSvgLoader` | 126 | metricanalyzer, project |
| `core.config.PropertyConfigStore` | 108 | metricanalyzer |
| `importer.ui.skins.EkgComboBoxListViewSkin` | 199 | project, repository |

Dem Wesen nach sind das Utils, keine SPI. Weil das Importer-Framework sie benutzt, gibt es nur
zwei Möglichkeiten: Sie bleiben im SDK, oder sie bekommen ein eigenes Paket unterhalb des SDK
(Arbeitsname `awb.runtime`), von dem das SDK abhängen darf und das selbst von nichts abhängt.

### 4.4 Ohne Verwender (6)

`importer.tasks.steps.reader.SeriesReader`, `DeltaSeriesReader`, `FullSeriesReader`,
`importer.tasks.steps.writer.DeltaSeriesWriter`, `FullSeriesWriter` und
`importer.ui.dialogs.queryimport.QueryImportDialogModel`.

`SeriesReader` wird nur von `DeltaSeriesReader` referenziert, das selbst niemand verwendet.

## 5. Was im SDK fehlt: die Repository-SPI

Wer ein neues Zeitreihen-Repository baut, findet seine Verträge heute nicht im SDK, sondern an zwei
anderen Stellen:

**`da.api` (38 von 48 Dateien):**

- `RepositoryClient`, `RepositoryClientFactory`, `SearchIndexClientProvider`,
  `SearchIndexAdminClient`, `RepositoryNotAvailableException`;
- `dataobject/**` (23): Ausdrücke, Facetten, Such-, Export-, Lösch- und Update-Parameter, Status,
  Collection-Eigenschaften;
- `schema/**` (5): `EkgSchemaField`, `Field`, `FieldReader`, `PersistedField`, `DocumentType`;
- `types/**` (5): die Qualifier `Solr`, `ElasticSearch`, `ClickHouse`, `Embedded`, `Remote`.

**`repository.api` (3 Dateien):** `EkgRepository` mit der inneren `Factory`,
`RepositoryClientAware`, `model.EkgRepositoryDbType`.

Diese 41 Dateien importieren aus der Workbench ausschließlich SDK-Klassen. Sie ließen sich deshalb
ohne Zyklus als eigener Bereich in das SDK aufnehmen (Arbeitsname `sdk.repository`).

Nicht dazu gehören die übrigen zehn Dateien in `da.api`: `ProjectStore`, `RepositoryConfigStore`,
`CollectionSourceStore`, `DimensionAliasStore`, `SecretStore` und ihre Row-Typen. Das sind
Metadaten-Ports der Workbench, keine Erweiterungs-SPI. Heute liegen beide Arten im selben Paket.

Ebenfalls nicht SPI in `repository.api`: `RepositoryService`, die Events, `AbstractEt` und das
leere `MetaDataRepository`.

## 6. Was Verschieben nicht löst

- **Zwei zentrale Listen pro Repository-Typ.** Die Enum `EkgRepositoryDbType` (16 Verwender, Teil
  der Signatur von `EkgRepository.Factory`) und die Qualifier-Annotationen in `da.api.types`. Ein
  neuer Repository-Typ muss dort weiterhin zentral eingetragen werden, zusätzlich zu den Icons und
  URL-Regeln in `EkgRepositoryAdminController`.
- **Keine SPI für Analysis-Sources.** Der einzige Vertrag ist `CollectionPlan` in `datasource.bl`.
  Topics, Sources und Wizard-Seiten sind zentral verdrahtet. Hier gibt es nichts zu verschieben,
  nur etwas zu entwerfen.
- **Mehrstufige Ketten für eine Aufgabe.** Ein Repository entsteht über `EkgRepository.Factory`,
  `RepositoryClientFactory` und `SearchIndexClientProvider`. Ob drei Ebenen nötig sind, ist eine
  Frage der Vereinfachung, nicht der Paketstruktur.

## 7. Mögliche kleine Schritte

Kandidaten, keine Reihenfolge und kein Beschluss. Jeder Schritt ist für sich abgeschlossen,
kompiliert und lässt die Testsuite grün.

| Schritt | Umfang | Art |
| --- | --- | --- |
| die sechs Klassen aus 4.4 löschen | 6 Dateien | Löschen |
| die zehn Klassen aus 4.2 zu ihrem Nutzer verschieben | 10 Dateien, wenige Importe | reines Verschieben |
| Interna des Importer-Frameworks nach `sdk.importer.internal` | 12 Dateien, 4 FXML-Dateien | reines Verschieben |
| Repository-SPI als `sdk.repository` aufnehmen | 41 Dateien, viele Importe | reines Verschieben |
| Metadaten-Stores aus `da.api` von der Repository-SPI trennen | 10 Dateien | reines Verschieben |
| Utils aus 4.3 unter das SDK legen | 8 Dateien, rund 50 Importe für `EkgLogger` | reines Verschieben, rein kosmetisch |
| `EkgRepositoryDbType` und Qualifier durch eine Registrierung ersetzen | Entwurf nötig | Verhaltensänderung |
| Vertrag für Analysis-Sources | Entwurf nötig | Verhaltensänderung |

Die ersten sechs ändern kein Verhalten. Die letzten beiden sind die eigentlichen Beiträge zur
Erweiterungsarchitektur und brauchen jeweils eine eigene kurze Entscheidung.
