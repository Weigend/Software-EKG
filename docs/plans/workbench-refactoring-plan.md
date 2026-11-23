# Refactoring-Plan: klare Erweiterungspunkte der Workbench

Status: Planungsdokument, noch keine Freigabe zur Umsetzung  
Geltungsbereich: `workbench`  
Nicht im Geltungsbereich: struktureller Umbau des Collectors

## 1. Anlass und Zielbild

Der Collector ist für Entwickler ausreichend klar strukturiert. Neue Collector-Sources liegen unter
`collector/sources`, neue Repository-Adapter unter `collector/repositories`. Modulgrenzen,
ServiceLoader-Verträge und Beispiele machen sichtbar, wo eine Erweiterung beginnt und was sie
implementieren muss.

Die Workbench bietet diese Orientierung derzeit nicht. Für drei zentrale Erweiterungen gibt es
keinen einzelnen, offensichtlichen Einstiegspunkt:

1. eine neue Datenquelle, die im Dialog **New Analysis** angeboten wird;
2. ein neues Repository für Zeitreihen;
3. eine andere Metadatenbank anstelle der eingebetteten H2-Datenbank.

Zusätzlich vermischt der Metric Explorer mehrere große Verantwortungsbereiche. Chart, Header,
Legend, Query, Explorer-Baum und Bookmarks sind zwar teilweise in Unterpaketen abgelegt, ihre
Abhängigkeiten und Zuständigkeiten sind aber nicht klar genug abgegrenzt.

Das Ziel ist keine Rückkehr zu vielen Maven-Modulen. Die Workbench bleibt eine gemeinsam
ausgelieferte Desktop-Anwendung in einem Maven-Modul. Klarheit entsteht durch fachliche Pakete,
kleine explizite Verträge, genau einen Registrierungspunkt pro Erweiterungsart, Contract-Tests und
Architekturregeln.

Nach dem Refactoring muss ein Entwickler die folgenden Fragen allein anhand der Verzeichnisstruktur
und je eines kurzen Entwicklerdokuments beantworten können:

- Wo beginne ich mit einer neuen Analysis-Source?
- Welchen Vertrag muss sie erfüllen, damit sie unter **New Analysis** erscheint?
- Welche Teile sind Source-spezifisch und welche gehören zum allgemeinen Analysis-Ablauf?
- Wo beginne ich mit einem neuen Zeitreihen-Repository?
- Welche Fähigkeiten muss oder kann ein Repository anbieten?
- Wie liefert es Beschreibung, Verbindungseinstellungen, Validierung und Client-Erzeugung?
- Welche Interfaces implementiere ich für eine andere Metadatenbank?
- Wie werden Schema, Migrationen, Transaktionen und Lebenszyklus eines Metadaten-Backends behandelt?
- Wo liegen Chart, Header und Legend, und in welche Richtung dürfen sie voneinander abhängen?

## 2. Begriffe und bewusste Abgrenzungen

### 2.1 Analysis-Source und Collector-Source

Die beiden Begriffe dürfen nicht gleichgesetzt werden:

- Eine **Collector-Source** ist eine Laufzeiterweiterung des Collectors. Sie liest Messwerte und wird
  über die Collector-Verträge geladen.
- Eine **Analysis-Source** ist die Workbench-Integration einer Datenquelle. Sie beschreibt die
  Auswahl in **New Analysis**, Discovery und Eingabeseiten, erzeugt einen Collection-Plan und
  übersetzt diesen in eine Collector-Konfiguration.

Eine Analysis-Source verwendet normalerweise eine Collector-Source, umfasst aber zusätzliche
Workbench-Verantwortung. Der Plan verwendet deshalb durchgehend den Namen `analysis source`.

### 2.2 Zeitreihen-Repository und Metadatenbank

Auch diese beiden Persistenzaufgaben bleiben getrennt:

- Ein **Zeitreihen-Repository** speichert und durchsucht Messwerte, beispielsweise Embedded Solr,
  Remote Solr, Elasticsearch oder ClickHouse.
- Ein **Metadata-Backend** speichert Workbench-Zustand, beispielsweise Projekte,
  Repository-Konfigurationen, Collection-Sources, Dimensions-Aliase und Bookmarks. Heute übernimmt
  H2 diese Aufgabe.

Ein neues Zeitreihen-Repository darf keine Implementierung des Metadata-Backends voraussetzen.
Ein neues Metadata-Backend darf keine Zeitreihenlogik implementieren.

### 2.3 Interne Erweiterung, kein Plugin-Versprechen

Die neuen Verträge schaffen einen klaren internen Erweiterungspunkt. Sie versprechen zunächst keine
binärkompatible Drittanbieter-Plugin-API und keine unabhängige Auslieferung. Alle Implementierungen
bleiben Teil des Workbench-Builds. Eine spätere Plugin-Grenze wäre eine eigene Architekturentscheidung.

## 3. Verbindliche Planungsgrundsätze

1. Vor dem ersten Verschieben oder Umbenennen wird eine reproduzierbare Baseline erhoben.
2. Jeder Migrationsschritt erhält vor der Änderung Charakterisierungstests für das betroffene
   Verhalten.
3. Ein Schritt ändert entweder Struktur oder Verhalten, niemals beides gleichzeitig.
4. Jeder Zwischenstand kompiliert und erfüllt die vollständige Testsuite.
5. Keine abstrakte SPI wird auf Vorrat eingeführt. Analysis-Source- und Repository-Verträge werden
   an mehreren vorhandenen Varianten validiert. Der Metadata-Vertrag ist durch das ausdrücklich
   gesetzte Ziel eines austauschbaren Backends begründet und wird durch H2 plus eine
   technologieunabhängige Contract-Test-Fixture validiert.
6. Registrierungen sind typisiert und werden beim Start validiert. Fehlende IDs, doppelte IDs oder
   unvollständige Fähigkeiten führen zu einer verständlichen Fehlermeldung.
7. Zentrale Dialoge und Services enthalten keine Switches über konkrete Erweiterungstypen.
8. UI-Metadaten gehören zum jeweiligen Provider, nicht in zentrale Enums oder Controller-Maps.
9. Technologieklassen bleiben hinter fachlichen Ports. Business-Logik importiert weder H2- noch
   Solr-, Elasticsearch- oder ClickHouse-Implementierungsklassen.
10. Das Refactoring optimiert Auffindbarkeit und Änderungsradius. Eine zusätzliche Indirektion ist
    nur zulässig, wenn sie einen der definierten Erweiterungswege vereinfacht oder eine messbare
    Abhängigkeitsverletzung beseitigt.

## 4. Ist-Zustand und konkrete Hindernisse

Die folgenden Befunde beschreiben den Startpunkt. Die Zahlen sind eine Momentaufnahme und werden in
Phase 0 durch einen versionierten Baseline-Bericht ersetzt.

### 4.1 Analysis-Sources und New Analysis

Der aktuelle Ablauf verteilt sich hauptsächlich über:

- `datasource.bl.AnalysisTopic`: fachliche Themen, Verfügbarkeit und Zuordnung zu Sources;
- `datasource.bl.AnalysisSource`: zentrale Enum aller angebotenen Source-Arten;
- `datasource.ui.NewAnalysisDialog`: Wizard, Source-Auswahl, Prometheus-Discovery und Plan-Erzeugung;
- `datasource.ui.SshPage` und `datasource.ui.WindowsPage`: Source-spezifische UI;
- `datasource.bl.AnalysisPlan`, `SshPlan`, `WindowsPlan`: Source-spezifische Pläne;
- `datasource.bl.*ConfigCompiler`: Übersetzung in Collector-Konfiguration;
- `datasource.bl.DataCollectionService`: Discovery, Projektanlage, Persistenz, Engine-Start und
  Lebenszyklus;
- `datasource.ui.TopicIcons`: zentrale Darstellung der Topics;
- `datasource.ui.EkgDataSourceModule`: Einbau des Dialogs in das Anwendungsmenü.

`AnalysisTopic.sources()` enthält einen Switch mit den Source-Zuordnungen. `NewAnalysisDialog`
kennt Prometheus, SSH und Windows direkt, erzeugt konkrete Pages und konkrete Plan-Typen. Eine neue
Source erfordert damit Änderungen an mehreren zentralen Dateien. Der Dialog hat derzeit ungefähr
1.215 Zeilen, `DataCollectionService` ungefähr 768 Zeilen, `WindowsPage` ungefähr 509 und
`SshPage` ungefähr 450 Zeilen.

Es gibt zwar den gemeinsamen Vertrag `CollectionPlan`, aber keinen Vertrag für die vollständige
Workbench-Integration einer Analysis-Source.

### 4.2 Zeitreihen-Repositories

Der aktuelle Ablauf enthält mehrere Ebenen:

- `repository.api.EkgRepository` und dessen verschachtelte `Factory`;
- `repository.bl.repositories.EkgRepositoryBase` und konkrete Repository-Typen;
- `da.api.RepositoryClient`, `RepositoryClientFactory`, `SearchIndexClientProvider` und
  `SearchIndexAdminClient`;
- konkrete Client-Provider unter `da.solr`, `da.elasticsearch` und `da.clickhouse`;
- `repository.api.model.EkgRepositoryDbType` als zentrale Enum;
- `repository.bl.mapper.RepositoryMapper`, das alle `EkgRepository.Factory`-Beans durchsucht;
- `repository.ui.admin.EkgRepositoryAdminController` mit zentralen Icons, Disable-Zuständen,
  URL-Regeln und Eingabeverhalten pro Repository-Typ.

Damit ist die Laufzeit-Factory teilweise erweiterbar, Beschreibung und UI bleiben aber zentral
hartcodiert. Ein neuer Typ muss derzeit mindestens Enum, Factory, Client-Provider, Repositoryklasse,
Admin-UI und Ressourcen konsistent ergänzen. Der notwendige Satz ist nicht als ein Vertrag sichtbar.

### 4.3 Metadata-Backend

Für mehrere Tabellen existieren bereits technologieunabhängige Stores:

- `ProjectStore`;
- `RepositoryConfigStore`;
- `CollectionSourceStore`;
- `DimensionAliasStore`;
- `SecretStore` mit einer separaten dateibasierten Implementierung.

Die H2-Implementierungen liegen unter `da.h2`. Der Austausch ist dennoch unvollständig vorbereitet:

- `H2BookmarkRepository` besitzt keinen technologieunabhängigen Port;
- Bookmark-Row-Klassen sind innere Klassen des H2-Adapters;
- `MetricsBookmarkServiceImpl` importiert `H2BookmarkRepository` und seine Row-Typen direkt;
- `H2DataSource` bündelt Connection-Erzeugung, Schema-Bootstrap, Speicherort und Shutdown;
- `schema.sql` ist global benannt und nicht als versionsfähige Backend-Migration organisiert;
- es gibt keinen aggregierten Backend-Vertrag und keine gemeinsame Contract-Test-Suite;
- das leere `MetaDataRepository`-Interface beschreibt keine nutzbare Grenze.

Eine andere Metadatenbank müsste deshalb nicht nur Store-Interfaces implementieren, sondern zuerst
verdeckte H2-Kopplungen finden und auflösen.

### 4.4 Metric Explorer

Die wesentlichen Bereiche sind derzeit:

- `metricanalyzer.ui.chartng`: Workbench-spezifische Chart-Orchestrierung, Commands und Export;
- `common.ui.chartng`: generische Chart-Komponenten, Achsen, Zoom und Legend;
- `metricanalyzer.ui.filterheader`: Header, Filter, Konverter und Header-Modell;
- `metricanalyzer.ui.explorer`: Explorer-Builder und Explorer-Items;
- `metricanalyzer.bl.tsquery`: Query-Modell und Query-Ausführung;
- `metricanalyzer.bl.visualisation`: Transformation, Kombination und Glättung;
- `metricanalyzer.bl.bookmarks` und `metricanalyzer.ui.bookmarks`: Bookmarks.

Chart, Header und Legend liegen zwar in verschiedenen Verzeichnissen, ihre Rollen sind nicht als
Komponentengrenzen abgesichert. `ChartPanelController` importiert den Header direkt. Chart und Legend
sind zwischen `metricanalyzer.ui.chartng` und `common.ui.chartng` verteilt. Große Klassen konzentrieren
viel Verhalten:

- `ChartPanelController`: ungefähr 1.122 Zeilen;
- `ChartHeaderController`: ungefähr 895 Zeilen;
- `ChartHeaderModel`: ungefähr 708 Zeilen;
- `StackedChartController`: ungefähr 599 Zeilen;
- mehrere generische Chart-/Axis-Klassen liegen zwischen 700 und 860 Zeilen.

Die Zeilenzahl allein ist kein Fehler. Sie zeigt aber, wo Verantwortungs- und Abhängigkeitsmessungen
vor dem Umbau besonders wichtig sind.

### 4.5 Befund aus der S202-Komponentenanalyse

Die mit S202 gefundenen API-/Impl-Verletzungen dürfen nicht ungeprüft als Architekturverletzungen
in diesen Plan übernommen werden. Die Detailanalyse zeigt, dass die heutige Heuristik die Struktur
der Workbench nur unzureichend abbildet:

- 428 gemeldete Verletzungen entsprechen 372 verschiedenen Kanten; 56 Kanten werden unter zwei
  Verstoßarten gezählt.
- 37 Kanten entstehen ausschließlich durch das von Avaje generierte `awb.AwbModule`. Als
  Composition Root muss dieses Modul Implementierungen kennen und darf nicht wie ein normaler
  fachlicher Nutzer bewertet werden.
- 69 Kanten sind Zugriffe einer vermeintlichen eigenen API auf die eigene Implementierung. Ursache
  ist die Heuristik „konkrete Klasse im selben Paket wie ein Interface ist API“, nicht eine
  tatsächlich durchbrochene Komponentengrenze.
- Von 439 als Implementierung gemappten Klassen werden nur 66, also 15 Prozent, überhaupt von einer
  anderen Komponente verwendet.
- Nach Abzug der Mapping-Artefakte bleiben 266 fremde Zugriffe auf vermeintliche Implementierungen.
  243 davon, also 91 Prozent, konzentrieren sich auf sieben Bibliotheks- oder Modellbereiche:
  `awb.sdk.core`, `awb.sdk.datamodel`, `awb.explorer.ui`, `awb.common.ui`, `awb.common.cdi`,
  `awb.sdk.importer` und `collector.config.model`.
- Nur drei Kanten umgehen ein ausdrücklich als `api` benanntes Paket: zwei Zugriffe auf
  `repository.bl.BinaryTimeSeries` und ein Zugriff auf `da.h2.H2BookmarkRepository`.
- Der Komponentengraph ist azyklisch. `sdk` hängt von keiner anderen Komponente ab und wird von zehn
  Komponenten benutzt. Zwischen den Produkten existiert nur die Richtung
  `awb.datasource -> Collector`, niemals umgekehrt.

Alternative Mappings bestätigen, dass nicht „nahezu alles API“ ist:

| Mapping | beobachteter API-Anteil an 767 Klassen |
| --- | ---: |
| aktuelle S202-Heuristik | 42 % / 328 Klassen |
| Klasse wird von außen benutzt | 23 % / 181 Klassen |
| Paket enthält eine außen benutzte Klasse | 40 % / 310 Klassen in 78 von 205 Paketen |

Die Struktur ist zweigeteilt:

- **Gemeinsam genutzte Bibliotheken:** `explorer` wird zu 100 Prozent von außen genutzt,
  `collector.control` zu 92 Prozent, `common` zu 90 Prozent, `sdk` zu 80 Prozent und
  `collector.config` zu 77 Prozent. Für diese Bereiche ist eine pauschale API-/Impl-Aufteilung wenig
  aussagekräftig. Ihr Zweck ist gerade, Klassen zur Nutzung bereitzustellen.
- **Geschlossene Features:** `aichat`, `datasource` und `importer` werden von keiner anderen
  Komponente benutzt; `metricanalyzer` gibt nur etwa 15 Prozent seiner Klassen nach außen. Ihre
  Kapselung ist real, obwohl sie nicht aus einem Interface-Vertrag für jede Klasse besteht.

Außerdem werden 213 der 328 von S202 als API gemappten Klassen von keiner anderen Komponente
verwendet. 153 davon sind konkrete Klassen neben einem Interface, weitere 60 sind intern genutzte
Interfaces. Ein Interface ist somit weder automatisch öffentliche API noch der Beweis für eine
Komponentengrenze.

Die Folgerung für dieses Refactoring lautet:

1. Komponenten werden anhand von fachlicher Verantwortung, eigenem Lebenszyklus und tatsächlichen
   Nutzern bestimmt, nicht anhand vorhandener Interfaces oder `api`-Paketnamen.
2. Geschlossene Features brauchen keine künstliche öffentliche API. Interne Interfaces bleiben
   interne Abstraktionen.
3. Bibliotheken brauchen eine dokumentierte Nutzungsfläche und stabile Abhängigkeitsrichtung, aber
   keine erzwungene API-/Impl-Zweiteilung.
4. Ports werden nur dort eingeführt, wo tatsächlich eine Grenze überschritten oder eine
   austauschbare Implementierung benötigt wird: Analysis-Source-Provider innerhalb des Features,
   Zeitreihen-Repository, Metadata-Backend und wenige UI-Kompositionsports.
5. S202-Ergebnisse werden erst nach einem korrigierten Mapping als Gate verwendet.

## 5. Zielstruktur der Workbench

Die Zielstruktur ist fachlich zuerst, technisch innerhalb des Features. Sie unterscheidet echte
Komponenten, gemeinsam genutzte Bibliotheken, interne Teilbereiche und Adapter. Paketnamen `api`,
`bl`, `da` und `ui` dürfen nicht mehr die Komponentenzuordnung bestimmen.

### 5.1 Welche Komponenten weiterhin sinnvoll sind

| Zielbaustein | Art | Entscheidung und Begründung |
| --- | --- | --- |
| Collector | eigenständiges Produkt und Plugin-Plattform | Unverändert lassen. Sources und Repository-Adapter sind bereits auffindbare echte Erweiterungsgrenzen. |
| Workbench Application | Composition Root | Behalten. Verdrahtet Features, Bibliotheken und Adapter. Generierte DI-Module gehören ausschließlich hierher und werden aus normalen Architekturverletzungen ausgeschlossen. |
| Analysis | geschlossenes Feature | Aus `datasource` bilden. Keine öffentliche Feature-API nötig. Die Analysis-Source-Verträge sind interne Erweiterungspunkte dieses Features. |
| Importer | geschlossenes Feature | Behalten und `sdk.importer` hineinziehen. Importer-Basisklassen und Dialoge sind interne Frameworkteile des Features, keine allgemeine Workbench-SDK-Komponente. |
| AI Chat | geschlossenes Feature | Behalten. Keine API-/Impl-Aufteilung erzeugen, solange kein anderer Baustein eine dokumentierte AI-Chat-Leistung konsumiert. |
| Metric Explorer | geschlossenes Feature mit internen Komponenten | Behalten. Chart, Header, Legend, Query, Explorer und Bookmarks sind klar getrennte interne Teile, aber nicht automatisch öffentliche Workbench-Komponenten. Nur tatsächlich konsumierte Ports werden exportiert. |
| Project | zentraler Workbench-Service | Behalten, sofern die Baseline die Nutzung durch mehrere Features bestätigt. Exportiert nur die tatsächlich verwendeten Projekt-Use-Cases und Modelle. |
| Repository Management | zentraler Workbench-Service | Behalten. Verwaltet Repository-Definitionen und Auswahl; konkrete Zeitreihen-Technologien bleiben Adapter. Nicht mit dem Metadata-Backend vermischen. |
| Time-Series Repository | Infrastruktur-Port mit Adaptern | Als echte Grenze beibehalten beziehungsweise schärfen, weil mehrere Features Zeitreihendaten benötigen und Solr, Elasticsearch und ClickHouse austauschbare Implementierungen sind. |
| Metadata Backend | Infrastruktur-Port mit Adaptern | Als echte Grenze vervollständigen, weil H2 ausdrücklich austauschbar werden soll. Die fachlichen Stores sind der Port; H2 ist ein Adapter. |
| Explorer UI | gemeinsam genutzte UI-Bibliothek | Behalten, wenn die Baseline die heutige breite Nutzung bestätigt. Basisklassen und Builder-Verträge sind die Bibliotheksoberfläche; keine künstliche Impl-Hälfte erzeugen. |
| UI Toolkit / Chart Primitives | gemeinsam genutzte UI-Bibliothek | Aus dem wirklich feature-neutralen Teil von `common.ui` bilden. Metric-Explorer-spezifische Chart- und Legend-Klassen gehören zurück in das Feature. |
| Core Runtime | kleine gemeinsam genutzte Bibliothek | Logger, Resource Loading und notwendige Laufzeitdienste aus `sdk.core` nach Verantwortung ordnen. `EkgLogger`, `CdiFxmlLoader` und `ServiceDiscovery` sind Bibliotheksklassen, keine versteckten Implementierungen. |
| Shared Data Model | gemeinsam genutzte Modellbibliothek | `Value`, `TimeSeries` und `RawSeriesData` als bewusst geteiltes Modell behandeln, sofern ihre komponentenübergreifende Nutzung fachlich gewollt ist. Kein API-/Impl-Schema darüberlegen. |
| `sdk` als Gesamtkomponente | historische Sammelkomponente | Auflösen. Core Runtime und Shared Data Model werden benannte Bibliotheken; Importer-Code geht zum Importer; `awbapi`-Verträge gehen zu ihrem fachlichen Eigentümer. Es gibt kein externes SDK-Versprechen mehr. |
| `common` als Gesamtkomponente | historische Sammelkomponente | Auflösen. UI-Primitives, DI-/Composition-Hilfen und fachliche Restklassen erhalten getrennte Eigentümer. `common` ist kein dauerhaftes Zielpaket. |

`Project`, `Repository Management`, `Explorer UI`, `Core Runtime` und `Shared Data Model` werden in
Phase 0 nochmals anhand des nutzungsbasierten Graphen bestätigt. „Behalten“ bedeutet nicht zwingend
ein Maven-Modul, sondern eine benannte Verantwortung mit klarer Eigentümerschaft.

### 5.2 Zielpakete

```text
io.softwareecg.awb
├── analysis
│   ├── source              interne Verträge und Registry für Analysis-Sources
│   ├── application         Start, Add, Pause, Resume und Lebenszyklus
│   ├── ui                  allgemeiner New-Analysis-Wizard und Data-Sources-Dialog
│   └── sources
│       ├── prometheus      vollständige Prometheus-Integration
│       ├── ssh             vollständige SSH/top-Integration
│       └── windows         vollständige Windows-Counter-Integration
├── timeseries
│   ├── api                 fachliche Zeitreihen-Repository-Verträge
│   ├── repository          Provider-Vertrag, Registry und gemeinsame Orchestrierung
│   ├── ui                  allgemeine Repository-Verwaltung
│   └── repositories
│       ├── solr
│       ├── elasticsearch
│       └── clickhouse
├── metadata
│   ├── api                 alle Metadata-Ports und Rows
│   ├── application         fachliche Services, Transaktionsgrenzen
│   └── h2                  H2-Backend, Schema und Migrationen
├── platform
│   ├── explorer            gemeinsam genutzte Explorer-UI-Bibliothek
│   ├── ui                  kleine feature-neutrale UI-Primitives
│   ├── runtime             Logging, Ressourcen und notwendige Laufzeitdienste
│   └── model               bewusst gemeinsam genutztes Datenmodell
└── metricexplorer
    ├── query               Query-Modell und Query-Use-Cases
    ├── explorer            Baum, Items und Builder
    ├── chart               Chart-Komposition und Datenversorgung
    ├── header              Filter-/Zeitraum-/Query-Header
    ├── legend              Legend-Modell, Darstellung und Aktionen
    ├── bookmarks           Bookmark-Use-Cases und UI
    └── visualisation       Kombination, Glättung und Darstellungsvorbereitung
```

Dies ist eine Ziel-Landkarte, keine Anweisung für eine einmalige Massenverschiebung. Die endgültigen
Namen werden in Phase 1 anhand der Baseline bestätigt. Entscheidend sind Verantwortung und
Abhängigkeitsrichtung, nicht die exakte Schreibweise eines Paketsegments.

Die Darstellung zeigt Pakete, nicht automatisch Komponenten. Insbesondere sind
`metricexplorer.chart`, `header` und `legend` interne Komponenten des geschlossenen Features. Die
`platform`-Pakete sind Bibliotheken. Nur `timeseries.api` und `metadata.api` stellen in dieser
Landkarte bewusst komponentenübergreifende Infrastruktur-Ports dar.

## 6. Erweiterungspunkt A: Analysis-Source

### 6.1 Ziel für den Entwickler

Eine neue Source entsteht vollständig in einem Verzeichnis
`analysis/sources/<source-id>`. Sie erscheint in **New Analysis**, sobald ihr Provider durch die
Workbench-DI gefunden wurde. Weder der allgemeine Wizard noch eine zentrale Source-Enum werden dafür
geändert.

### 6.2 Vorgesehene Verträge

Die Namen sind Arbeitsnamen; ihre Verantwortung ist verbindlich:

```java
interface AnalysisSourceProvider {
    AnalysisSourceDescriptor descriptor();
    Set<AnalysisTopicId> supportedTopics();
    AnalysisSourceFlow createFlow(AnalysisSourceContext context);
}

interface AnalysisSourceFlow {
    List<AnalysisWizardStep> steps();
    CompletionStage<SourceDiscoveryResult> discover();
    CollectionPlan buildPlan();
}
```

Ergänzende Werttypen:

- `AnalysisSourceId`: stabile persistierte ID, nicht der Klassen- oder Anzeigename;
- `AnalysisSourceDescriptor`: Titel, Beschreibung, Icon, Sortierung, Verfügbarkeit und
  Aggregating/Direct-Eigenschaft;
- `AnalysisTopicId` und `AnalysisTopicDescriptor`: Topic-Metadaten ohne Source-Switch;
- `AnalysisWizardStep`: klarer UI-Vertrag für Node, Validierung, Aktivierung und Aufräumen;
- `SourceDiscoveryResult`: technologieunabhängiges Ergebnis oder typisierter Provider-Zustand;
- `CollectionPlan`: bleibt die Übergabe an die allgemeine Collection-Anwendung, wird aber frei von
  Verweisen auf konkrete Compiler;
- `CollectorConfigurationCompiler<P extends CollectionPlan>` oder eine äquivalente Methode am
  Provider: Übersetzung des Plans in die Collector-Konfiguration.

### 6.3 Registry und Validierung

`AnalysisSourceRegistry` erhält alle Provider über die bestehende Dependency Injection. Sie:

- indexiert nach stabiler ID;
- lehnt doppelte IDs beim Start ab;
- liefert Topics und Source-Angebote an den Wizard;
- prüft, dass Descriptor, Topics, Flow und Plan-Compiler vollständig sind;
- kennt keine konkrete Source-Klasse.

Die Topic-zu-Source-Zuordnung wird aus den Providern aggregiert. `AnalysisTopic.sources()` und die
Enum `AnalysisSource` entfallen erst, nachdem alle realen Sources migriert sind.

### 6.4 Trennung des allgemeinen Ablaufs

Der allgemeine Application-Service verantwortet nur:

- Projektname und Kollisionsbehandlung;
- Persistenz eines fertigen `CollectionPlan`;
- Secret-Referenz;
- Start/Stop/Pause/Resume der Collector-Engine;
- Status und Historie;
- Öffnen des ersten Charts über ein Ereignis oder einen Port.

Discovery, Source-spezifische Validierung, Wizard-Seiten und Konfigurationskompilierung gehören zum
Provider. Dadurch kennt `DataCollectionService` weder Prometheus-, SSH- noch Windows-Typen.

### 6.5 Contract-Tests für jede Source

Jeder Provider durchläuft dieselbe Test-Suite:

- eindeutige und stabile ID;
- vollständiger Descriptor;
- mindestens ein unterstütztes Topic;
- Wizard kann ohne globale UI-Switches erzeugt werden;
- unvollständige Eingaben erzeugen keinen Plan;
- vollständige Eingaben erzeugen einen validen Plan;
- Plan enthält keine Secrets in exportierter Collector-Konfiguration;
- Compiler erzeugt eine vom Collector lesbare Konfiguration;
- persistierte ID kann nach Neustart wieder aufgelöst werden.

### 6.6 Definition of Done

Eine testweise vierte Analysis-Source kann als Test-Fixture registriert werden, ohne Änderungen an
`NewAnalysisDialog`, Topic-Definitionen, `DataCollectionService` oder zentralen Icon-/UI-Maps. Die
Fixture muss nicht als Produktfunktion ausgeliefert werden; sie beweist nur den Vertrag.

## 7. Erweiterungspunkt B: Zeitreihen-Repository

### 7.1 Ziel für den Entwickler

Ein neues Zeitreihen-Repository entsteht vollständig unter
`timeseries/repositories/<repository-id>`. Der Provider beschreibt Typ, Fähigkeiten,
Verbindungsparameter, Validierung und Client-Erzeugung. Die allgemeine Repository-Verwaltung rendert
daraus die Auswahl und die gemeinsamen Eingabefelder.

### 7.2 Vorgesehener Provider-Vertrag

```java
interface TimeSeriesRepositoryProvider {
    RepositoryTypeId id();
    RepositoryDescriptor descriptor();
    RepositoryCapabilities capabilities();
    ConnectionSchema connectionSchema();
    ValidationResult validate(ConnectionSettings settings);
    EkgRepository open(RepositoryDefinition definition);
}
```

Verantwortungen:

- `RepositoryTypeId`: stabile persistierte ID;
- `RepositoryDescriptor`: Anzeigename, Icon, Sortierung, embedded/remote und Hilfetext;
- `RepositoryCapabilities`: Search, Facets, Streaming, Administration, Export, Schreiben,
  Collection-Erzeugung und weitere tatsächlich genutzte Fähigkeiten;
- `ConnectionSchema`: benötigte Felder, unterstützte Authentisierung und feldbezogene Hinweise;
- `ConnectionSettings`: technologieunabhängige Nutzereingaben;
- `ValidationResult`: Fehler pro Feld plus optionale Verbindungsprüfung;
- `open`: erzeugt die fachliche Repository-Fassade; konkrete Clients bleiben im Adapter.

Der Vertrag soll die heutige Kette aus `EkgRepository.Factory`, `RepositoryClientFactory` und
`SearchIndexClientProvider` nicht blind verdoppeln. In Phase 2 wird entschieden, welche der heutigen
Interfaces intern hinter dem Provider weiterleben und welche zusammengeführt werden können.

### 7.3 UI und Persistenz

Die zentrale Enum `EkgRepositoryDbType` wird durch eine persistierbare ID plus Registry ersetzt.
Während der Migration liest ein Compatibility-Mapper die bisherigen Enum-Namen.

Der Repository-Admin-Dialog darf keine Map konkreter Repository-Typen und keine
technologiespezifischen URL-Patterns enthalten. Gemeinsame Felder werden aus `ConnectionSchema`
erzeugt; hochspezialisierte Eingaben können über einen optionalen, klar begrenzten Editor-Provider
ergänzt werden.

### 7.4 Contract-Tests für jedes Repository

- eindeutige stabile ID und vollständiger Descriptor;
- Connection-Schema und Validierung stimmen überein;
- ungültige Einstellungen öffnen keinen Client;
- Capability-Angaben entsprechen den tatsächlich verfügbaren Operationen;
- Create/Open/Close ist wiederholbar und räumt Ressourcen auf;
- eine minimale Write/Search/Delete-Runde funktioniert, sofern die Fähigkeiten angeboten werden;
- persistierte Definitionen können nach Neustart wieder geöffnet werden;
- Secrets erscheinen nicht in Logs, Fehlermeldungen oder Klartext-Metadaten.

### 7.5 Definition of Done

Ein Test-Repository-Provider kann registriert und im Admin-Modell sichtbar gemacht werden, ohne
Änderungen an zentraler Enum, zentralem Controller, Repository-Mapper oder Icon-Map. Solr,
Elasticsearch und ClickHouse verwenden denselben Provider-Vertrag und dieselbe Contract-Test-Suite.

## 8. Erweiterungspunkt C: Metadata-Backend

### 8.1 Ziel für den Entwickler

Eine andere Metadatenbank implementiert einen vollständigen, auf Anhieb auffindbaren Satz von Ports
und kann mit derselben Contract-Test-Suite wie H2 geprüft werden. Kein fachlicher Service importiert
eine H2-Klasse.

### 8.2 Vollständiger Port-Satz

Unter `metadata.api` liegen ausschließlich technologieunabhängige Interfaces und Row-/Value-Typen:

- `ProjectStore`;
- `RepositoryConfigStore`;
- `CollectionSourceStore`;
- `DimensionAliasStore`;
- `BookmarkStore` als neuer Port;
- gegebenenfalls `MetadataTransaction`, wenn mehrere Stores atomar geändert werden müssen;
- `MetadataBackend` als Aggregation und Lebenszyklusgrenze.

```java
interface MetadataBackend extends AutoCloseable {
    ProjectStore projects();
    RepositoryConfigStore repositoryConfigurations();
    CollectionSourceStore collectionSources();
    DimensionAliasStore dimensionAliases();
    BookmarkStore bookmarks();
    <T> T inTransaction(MetadataWork<T> work);
}
```

Ob Services die einzelnen Stores direkt injiziert bekommen oder über `MetadataBackend`, wird in
Phase 3 anhand der tatsächlichen Transaktionsgrenzen entschieden. Verbindlich ist: Ein Backend muss
den vollständigen Vertrag anbieten und atomare fachliche Operationen ermöglichen.

### 8.3 H2 als erster Adapter

Der H2-Adapter enthält anschließend ausschließlich:

- Connection-/Pool-Lebenszyklus;
- H2-Implementierungen aller Stores;
- SQL-Mapping;
- versionierte Migrationen in einem H2-spezifischen Resource-Pfad;
- Backend-Registrierung;
- H2-Contract- und Migrationstests.

Bookmark-Row-Klassen werden aus `H2BookmarkRepository` in technologieunabhängige Metadata-Typen
verschoben. `MetricsBookmarkServiceImpl` hängt danach nur an `BookmarkStore`.

Das leere `MetaDataRepository` wird entweder durch den vollständigen Vertrag ersetzt oder entfernt.
Es bleibt kein bedeutungsloses Marker-Interface bestehen.

### 8.4 Migration und Kompatibilität

Vor Änderungen am Schema werden festgelegt:

- aktuelle Schema-Version und Tabelleninventar;
- Upgrade-Pfad bestehender `~/.ekg/metadata`-Datenbanken;
- Backup-/Rollback-Verhalten;
- Verhalten bei neuerer, unbekannter Schema-Version;
- Transaktionsgrenzen für kaskadierende Löschungen;
- Zeichensatz-, Zeit- und ID-Konventionen.

Das Refactoring darf bestehende Benutzerdaten nicht stillschweigend neu anlegen oder verwerfen.

### 8.5 Contract-Tests für jedes Metadata-Backend

Die Suite wird einmal als abstrakter Vertrag definiert und gegen jedes Backend ausgeführt:

- CRUD und fachliche Suchvarianten aller Stores;
- Eindeutigkeits- und Fremdschlüsselregeln;
- kaskadierendes Löschen eines Projekts oder Repositories;
- atomare Transaktion und Rollback bei Fehler;
- parallele Zugriffe, soweit von der Workbench benötigt;
- Schema-Erstellung auf leerer Datenbank;
- Upgrade von jeder noch unterstützten Schema-Version;
- Neustart/Persistenz und sauberer Shutdown;
- identische Null-, Sortier- und Fehlersemantik über alle Backends.

### 8.6 Definition of Done

Eine zweite Backend-Implementierung kann ausschließlich gegen `metadata.api` und die
Contract-Test-Suite entwickelt werden. Außer dem Composition Root beziehungsweise der
Konfiguration muss keine fachliche oder UI-Klasse geändert werden. Im Produktivcode außerhalb von
`metadata.h2` existiert kein Import von `org.h2` oder einer `H2*`-Klasse.

## 9. Zieltrennung im Metric Explorer

### 9.1 Verantwortungen

#### Chart

`metricexplorer.chart` verantwortet:

- Komposition der Chart-Ansicht;
- Laden und Aktualisieren der darzustellenden Serien über Query-Ports;
- Chart-Zeitraum, Follow-Modus und Refresh;
- Commands, soweit sie tatsächlich Chart-Use-Cases sind;
- Export-Orchestrierung.

Es enthält keine Header-Controls und keine konkrete Legend-Implementierung.

#### Header

`metricexplorer.header` verantwortet:

- Filterauswahl;
- Zeitraum- und Aggregationsparameter;
- Umschaltung zwischen vereinfachter und Expert-Query;
- Übersetzung von UI-Eingaben in ein technologieunabhängiges `QueryRequest`;
- Validierung und Darstellung des Header-Zustands.

Der Header kennt weder Chart-Controller noch konkrete Repository-Clients. Er publiziert seinen
Zustand über einen kleinen Port oder immutable Requests.

#### Legend

`metricexplorer.legend` verantwortet:

- Legend-Modell und Reihenfolge;
- Farbe, Sichtbarkeit, Achsenzuordnung und Auswahl einer Serie;
- Legend-spezifische Aktionen;
- Darstellung der Legend.

Sie erhält ein Darstellungsmodell vom Chart, greift aber nicht auf Query-Service,
Repository-Service oder Header-Controller zu.

#### Query

`metricexplorer.query` ist UI-frei und verantwortet Query-Modell, Query-Aufbau und Ausführung. Das
Paket exportiert gezielte Ports an Chart, Header und Explorer, aber keine Repository- oder
JavaFX-Implementierungen.

#### Explorer und Bookmarks

Explorer-Baum und Bookmarks bleiben eigene Komponenten. Sie öffnen oder aktualisieren einen Chart
über einen `ChartWorkspace`-/`OpenChart`-Port, nicht über einen konkreten `ChartPanelController`.

### 9.2 Erlaubte Abhängigkeitsrichtung

```text
header ───────┐
              v
           chart-composition ─────> chart-view primitives
              ^       |
legend ───────┘       v
                    query ─────> timeseries.api

explorer ─────> chart workspace port
bookmarks ────> chart workspace port + query model
```

Zusätzliche Regeln:

- `header` und `legend` kennen einander nicht;
- `common.ui.chartng` wird entweder zu klaren, feature-neutralen Chart-Primitives oder vollständig
  in `metricexplorer.chart`/`legend` überführt;
- generische Achsen- und Zoom-Komponenten dürfen keine Imports aus `metricexplorer` besitzen;
- Controller kommunizieren nicht über gegenseitige konkrete Typen;
- UI-Pakete greifen nicht direkt auf `da`-Implementierungen zu.

### 9.3 Zerlegung großer Klassen

Klassen werden nicht nach Zeilenzahl mechanisch geteilt. Vor jeder Zerlegung werden ihre
Verantwortungen mit Methodenclustern und Abhängigkeiten erfasst. Erwartete Extraktionen sind:

- aus `ChartPanelController`: Workspace/Composition, Data Refresh, Command-Anbindung,
  Header-Anbindung, Legend-Anbindung und Export-Anbindung;
- aus `ChartHeaderController` und `ChartHeaderModel`: Filterzustand, Zeitraumzustand,
  Query-Modus, Validierung und View-Binding;
- aus `StackedChartController`: Series-Layer, Axis-Koordination, Layout und Legend-Projektion.

Eine extrahierte Klasse braucht einen fachlichen Namen und einen Test. Reine `*Helper`- oder
`*Manager`-Ablagen gelten nicht als abgeschlossene Zerlegung.

### 9.4 Definition of Done

- Chart, Header und Legend besitzen eigene öffentliche Ports und eigene Tests.
- Keine Komponente importiert den konkreten Controller einer anderen Komponente.
- Die erlaubte Abhängigkeitsrichtung wird mit ArchUnit geprüft.
- Ein Header kann mit einem Fake-Query-/Chart-Port getestet werden.
- Eine Legend kann allein aus einem Darstellungsmodell getestet werden.
- Der Chart kann mit Fake-Header und Fake-Legend komponiert werden.

## 10. Paket- und Namensregeln

1. `api` bezeichnet nur einen bewusst konsumierbaren Vertrag eines fachlichen Bereichs.
2. Implementierungsdetails liegen nicht in `api.impl`, sondern im fachlich benannten Adapterpaket.
3. `common`, `util`, `manager`, `helper` und `misc` sind keine zulässigen neuen Top-Level-Ziele.
4. Technologiepakete heißen nach der Technologie: `h2`, `solr`, `elasticsearch`, `clickhouse`.
5. Ein Provider-Paket enthält Descriptor, Factory/Provider, Validierung und technologiespezifische
   UI-Ressourcen gemeinsam, sofern diese nur zu diesem Provider gehören.
6. Persistierte Typen verwenden stabile String-IDs. Java-Enum-Namen oder Klassennamen werden nicht
   als neues Persistenzformat verwendet.
7. Abkürzungen wie `bl`, `da`, `et`, `awbapi`, `mgrdialog` und `chartng` werden in neuen Paketen
   nicht fortgeführt. Bestehende Pakete werden nur im passenden Migrationsschritt umbenannt.

## 11. Architekturregeln und automatisierte Gates

`LayerArchitectureTest` wird schrittweise zu ausführbaren Grenzen ausgebaut. Mindestens folgende
Regeln müssen am Ende gelten:

1. `analysis.ui` hängt nur von `analysis.api`, `analysis.source` und UI-Ports ab, nicht von
   konkreten Source-Implementierungen. Der Composition Root darf Provider verdrahten.
2. `analysis.application` importiert keine Klassen aus `analysis.sources.*`.
3. Der allgemeine New-Analysis-Wizard enthält keine Switches über Source-IDs oder Providerklassen.
4. `timeseries.repository` importiert keine konkreten Repository-Adapter.
5. `timeseries.ui` enthält keine Fallunterscheidung über konkrete Repository-IDs.
6. Fachliche Pakete außerhalb `metadata.h2` importieren weder H2 noch `metadata.h2`.
7. Metadata-Adapter hängen nach innen nur von `metadata.api` ab.
8. `metricexplorer.query` ist frei von JavaFX.
9. `metricexplorer.header` und `metricexplorer.legend` importieren keinen konkreten Chart-Controller.
10. Feature-UI greift nicht direkt auf Technologieadapter zu.
11. Es existieren keine Paketzyklen zwischen den definierten Zielkomponenten.
12. Ausnahmen sind namentlich, begründet und mit Ablaufdatum dokumentiert; Wildcard-Ausnahmen sind
    unzulässig.

## 12. Statistik- und Baseline-Konzept

Die Statistik wird vor Phase 1 definiert und vor jeder Produktivänderung ausgeführt. Das verhindert,
dass Messgrößen nachträglich passend zum Ergebnis ausgewählt werden.

### 12.1 Reproduzierbarkeit

Es wird ein read-only Analysewerkzeug unter `build/architecture/` vorgesehen. Es liest ausschließlich
getrackte Produktiv- und Testquellen und schreibt einen Bericht nach
`target/architecture-baseline/`. Der Bericht enthält:

- Commit-ID, Java- und Maven-Version;
- genaue Scope-Regeln und Ausschlüsse;
- maschinenlesbares JSON oder CSV;
- eine daraus generierte Markdown-Zusammenfassung;
- Exit-Code ungleich null bei unvollständiger Analyse.

Der erste freigegebene Bericht wird zusätzlich unter
`docs/architecture/baselines/<commit-id>/` versioniert. Spätere Berichte verwenden dieselbe
Werkzeugversion oder dokumentieren eine notwendige Metrikänderung separat.

Generierte Avaje-Klassen, `target`, Ressourcenbibliotheken und Test-Fixtures werden getrennt
ausgewiesen und nicht stillschweigend in Produktivmetriken eingerechnet.

### 12.2 Bestandsmetriken

Für jedes Zielgebiet und jedes Java-Paket werden erhoben:

- Anzahl Produktivklassen und Testklassen;
- nichtleere Codezeilen und Gesamtzeilen;
- öffentliche Typen und öffentliche Methoden;
- Dateien über 300, 500, 800 und 1.000 Zeilen;
- Paketbreite, Pakettiefe und Klassen pro Paket;
- Anzahl `package-info.java` mit aussagekräftiger Verantwortungsbeschreibung;
- Test-zu-Produktivklassen-Verhältnis als Inventar, nicht als Qualitätsquote.

### 12.3 Abhängigkeitsmetriken

Auf Basis kompilierter Klassen, nicht nur Textimports:

- afferente und efferente Kopplung pro Zielkomponente;
- gerichtete Abhängigkeiten zwischen den Zielkomponenten;
- Anzahl und Größe von Paketzyklen;
- direkte Technologieimporte außerhalb des jeweiligen Adapters;
- UI-zu-DA- und BL-zu-UI-Abhängigkeiten;
- konkrete Controller-zu-Controller-Abhängigkeiten;
- Nutzung von `Lookup.lookup`, `Lookup.lookupAll`, statischen Registries und Service Locators;
- ArchUnit-Verstöße je Regel, inklusive bewusst dokumentierter Baseline-Ausnahmen.

Die Auswertung erzeugt drei getrennte Sichten und vermischt sie nicht:

1. **Komponentengraph:** gerichtete Kanten zwischen den in Abschnitt 5.1 bestätigten fachlichen
   Komponenten und Bibliotheken;
2. **beobachtete Nutzungsfläche:** Klassen und Pakete, die tatsächlich von einem anderen Baustein
   referenziert werden;
3. **deklarierte Ports:** Typen, die bewusst als komponentenübergreifender Vertrag festgelegt wurden.

Eine Klasse gilt nicht allein deshalb als API, weil sie ein Interface ist, neben einem Interface
liegt oder sich in einem historisch `api` genannten Paket befindet. Umgekehrt ist eine konkrete
Bibliotheksklasse nicht allein deshalb eine gekapselte Implementierung, weil sie kein Interface
implementiert.

Für S202 wird vor der Refactoring-Bewertung festgelegt:

- generierte DI-Composition-Roots wie `awb.AwbModule` werden ausgeschlossen;
- die Regel „Klasse neben Interface wird API“ wird nicht zur Bewertung dieses Vorhabens verwendet;
- ein nutzungsbasiertes Mapping wird als eigene Sicht ausgegeben;
- Bibliotheken, geschlossene Features und austauschbare Adapter erhalten unterschiedliche
  Bewertungsregeln;
- die bekannten Ausgangswerte 428 Meldungen, 372 eindeutige Kanten, 37 DI-Kanten, 69 interne
  Fehlklassifizierungen und 266 verbleibende fremde Zugriffe werden im Baseline-Bericht
  nachvollziehbar reproduziert oder eine Abweichung wird erklärt.

### 12.4 Änderungsradius der drei Erweiterungswege

Für jeden Erweiterungsweg wird vor dem Refactoring eine statische Touchpoint-Matrix erstellt:

| Erweiterung | Zu ändernde zentrale Dateien | Neue Implementierungsdateien | Registrierungen | UI-Hardcodierungen | Persistenzänderungen |
| --- | ---: | ---: | ---: | ---: | ---: |
| Analysis-Source | Baseline erheben | Baseline erheben | Baseline erheben | Baseline erheben | Baseline erheben |
| Zeitreihen-Repository | Baseline erheben | Baseline erheben | Baseline erheben | Baseline erheben | Baseline erheben |
| Metadata-Backend | Baseline erheben | Baseline erheben | Baseline erheben | Baseline erheben | Baseline erheben |

Ein Touchpoint zählt nur, wenn eine neue konkrete Implementierung ohne Änderung dort nicht
kompilieren, nicht registriert oder nicht benutzbar wäre. Vermutete oder optionale Änderungen werden
separat ausgewiesen.

Nach dem Refactoring werden dieselben Szenarien mit Compile-only Test-Fixtures geprüft. Es wird keine
halbfertige Produktfunktion eingebaut, um die Metrik zu erzeugen.

### 12.5 Historische Änderungsmetriken

Aus der Git-Historie, mit dokumentiertem Zeitraum:

- Dateien, die häufig gemeinsam geändert wurden;
- Hotspots nach Änderungszahl multipliziert mit Dateigröße;
- Defect-/Fix-Commits, soweit anhand der Commit-Metadaten eindeutig erkennbar;
- gemeinsame Änderungen von Chart/Header/Legend;
- gemeinsame Änderungen beim Hinzufügen oder Ändern von Sources und Repositories.

Diese Zahlen dienen zur Priorisierung. Sie sind kein Gate, weil Commit-Historie und Commit-Stil die
Werte beeinflussen.

### 12.6 Verhaltens- und Qualitätsbaseline

Vor jeder Phase werden festgehalten:

- vollständige Maven-Testresultate;
- fokussierte Tests pro Zielgebiet;
- Start einer Analysis für Prometheus, SSH und Windows;
- Persistieren, Neustarten, Pausieren und Entfernen einer Collection-Source;
- Repository-Verwaltung und minimale Repository-Operationen;
- Öffnen einer bestehenden H2-Metadatenbank;
- Bookmark-, Projekt- und Repository-Konfigurationsoperationen;
- Chart öffnen, Header ändern, Legend-Aktion ausführen, Bookmark wiederherstellen und exportieren.

Manuelle Checks werden nur verwendet, wenn eine Automatisierung vor dem Umbau unverhältnismäßig ist.
Jeder manuelle Check bekommt genaue Schritte und erwartetes Ergebnis.

### 12.7 Zielwerte und Gates

Strukturelle Gates sind verbindlicher als pauschale LOC-Ziele:

- null Paketzyklen zwischen den definierten Zielkomponenten;
- null direkte H2-Imports außerhalb des H2-Adapters;
- null zentrale Source-Switches im allgemeinen Analysis-Wizard;
- null zentrale Repository-Typ-Switches im allgemeinen Repository-Dialog;
- genau ein Registrierungskonzept je Erweiterungsart;
- alle realen Provider bestehen ihre jeweilige Contract-Test-Suite;
- keine neuen ArchUnit-Baseline-Ausnahmen;
- bestehende Verhaltens- und Migrationstests bleiben grün.

Für die Komponentensicht gelten zusätzlich:

- der fachliche Komponentengraph bleibt azyklisch;
- geschlossene Features exportieren nur nachweislich benötigte Ports, nicht sämtliche Interfaces;
- Bibliotheksnutzung wird nicht als API-/Impl-Verletzung gezählt;
- keine Architekturentscheidung basiert auf der unbereinigten Zahl von 428 S202-Meldungen;
- die drei echten Umgehungen ausdrücklich benannter API-Pakete werden fachlich entschieden:
  `repository.bl.BinaryTimeSeries` erhält entweder einen bewusst exportierten Eigentümer oder seine
  Nutzer werden nach innen verlegt; `da.h2.H2BookmarkRepository` wird durch `BookmarkStore` ersetzt;
- generierter Composition-Code erscheint getrennt und nicht in der Verletzungszahl.

LOC, Kopplung und Klassengröße werden vor und nach jeder Phase berichtet. Sie sind Diagnosewerte.
Eine Phase darf sie nur verschlechtern, wenn die Review-Begründung den konkreten Nutzen und eine
Folgephase nennt. Es wird kein willkürlicher Zielwert wie „jede Klasse unter 300 Zeilen“ festgelegt.

## 13. Migrationsphasen

Jede Phase ist ein eigener Review- und Freigabepunkt. Ohne Abnahme der Ergebnisse einer Phase beginnt
die nächste nicht.

### Phase 0: Baseline und Entscheidungsgrundlage

Änderungen: nur Analysewerkzeug, Berichte und Dokumentation; keine Produktivstruktur.

1. Scope und Komponentenzuordnung jeder heutigen Klasse festhalten.
2. Statistikwerkzeug implementieren und auf einem festen Commit ausführen.
3. Touchpoint-Matrix für die drei Erweiterungswege erstellen.
4. Paketabhängigkeitsgraph und Zyklen erfassen.
5. bestehende Verhaltens- und Migrationstests katalogisieren.
6. Lücken als notwendige Charakterisierungstests den späteren Phasen zuordnen.
7. Zielpaketnamen und stabile IDs in einem Architecture Decision Record bestätigen.
8. heutige S202-Zuordnung mit den bekannten 428/372-Kanten-Zahlen reproduzieren.
9. `awb.AwbModule` als Composition Root aus der normalen API-/Impl-Auswertung ausschließen.
10. nutzungsbasiertes Mapping erzeugen und jeden Zielbaustein als geschlossenes Feature,
    Bibliothek, Infrastruktur-Port, Adapter oder Composition Root klassifizieren.
11. insbesondere `Project`, `Repository Management`, `Explorer UI`, `Core Runtime` und
    `Shared Data Model` anhand ihrer tatsächlichen Nutzer bestätigen oder neu zuordnen.

Abnahme: reproduzierbarer Baseline-Bericht, freigegebener Komponentenkatalog und erklärter Abgleich
mit den S202-Ausgangszahlen; keine Produktivänderung.

### Phase 1: Verträge festziehen, noch nichts verschieben

1. `AnalysisSourceProvider`, Registry und Source-Contract-Test definieren.
2. `TimeSeriesRepositoryProvider`, Capability-Modell, Registry und Contract-Test definieren.
3. vollständige Metadata-Ports einschließlich Bookmark-Store und Backend-Contract-Test definieren.
4. Ports für Chart Workspace, Header State und Legend Model definieren.
5. ArchUnit-Regeln zunächst gegen die heutige Struktur mit expliziter Baseline aktivieren.

Abnahme: Verträge sind an realen bestehenden Varianten validiert; keine neue Produktfunktion.

### Phase 2: Analysis-Sources vertikal migrieren

Reihenfolge:

1. Prometheus als umfangreichste aggregierende Source;
2. SSH/top als direkte Source;
3. Windows Counters als zweite direkte Source;
4. Entfernung der alten Enum-/Switch-Registrierung.

Pro Source werden Descriptor, Flow, UI-Schritte, Discovery, Plan und Compiler gemeinsam in ihr
Provider-Paket gezogen. Der allgemeine Wizard wird parallel auf Registry und Steps reduziert.

Abnahme: alle Source-Contract-Tests und New-Analysis-Verhaltenstests grün; Test-Fixture benötigt keine
zentralen Änderungen.

### Phase 3: Metadata-Backend vollständig abstrahieren

1. Bookmark-Port und technologieunabhängige Row-Typen einführen.
2. `MetricsBookmarkServiceImpl` von H2 lösen.
3. vorhandene Stores unter `metadata.api` konsolidieren.
4. Transaktionsanforderungen aus den Use Cases festlegen.
5. H2 hinter vollständigem Backend-Vertrag registrieren.
6. Schema versionieren und Migrationstest für bestehende Datenbank ergänzen.
7. direkte H2-Imports per ArchUnit verbieten.
8. leeres `MetaDataRepository` entfernen oder durch den echten Vertrag ersetzen.

Abnahme: H2 besteht die vollständige Backend-Suite; fachlicher Code kennt H2 nicht; bestehende
Benutzerdaten werden nachweislich geöffnet und migriert.

### Phase 4: Zeitreihen-Repositories vertikal migrieren

Reihenfolge:

1. Embedded Solr als Sonderfall für lokale Defaults;
2. Remote Solr/Cloud Solr;
3. Elasticsearch;
4. ClickHouse;
5. Entfernung der zentralen Typ-Enum aus UI und Registry-Pfad.

Pro Adapter werden Descriptor, Capabilities, Connection-Schema, Validierung, Factory und Ressourcen
zusammengeführt. Persistierte alte Enum-Werte bleiben über einen Compatibility-Mapper lesbar.

Abnahme: alle Repository-Contract-Tests grün; allgemeiner Dialog und Mapper kennen keine konkreten
Repository-Typen.

### Phase 5: Metric Explorer nach Komponenten trennen

Die Reihenfolge minimiert gegenseitige Änderungen:

1. aktuelle Abhängigkeiten und Charakterisierungstests für Chart/Header/Legend ergänzen;
2. immutable Query-/View-Modelle und Workspace-Ports einführen;
3. Legend aus Chart-Controller-Abhängigkeiten lösen;
4. Header über Query Request und Chart-Port anbinden;
5. Chart-Komposition von Data Fetch, Commands und Export trennen;
6. `common.ui.chartng` in echte Primitives und Metric-Explorer-spezifische Teile aufteilen;
7. Explorer und Bookmarks nur noch über Workspace-Ports anbinden;
8. Zielpakete herstellen und alte `chartng`-/`filterheader`-Pfade entfernen;
9. ArchUnit-Regeln ohne Baseline-Ausnahmen scharf schalten.

Abnahme: isolierte Komponenten-Tests, keine Controller-zu-Controller-Abhängigkeit, keine Zyklen,
vollständige Chart-Verhaltensbaseline grün.

### Phase 6: Konsolidierung und Entwicklerdokumentation

1. Compatibility-Adapter entfernen, deren Übergangsfrist abgelaufen ist.
2. ungenutzte historische APIs und leere Marker entfernen.
3. Paketdokumentation und Architekturübersicht aktualisieren.
4. je einen Entwicklerleitfaden schreiben:
   - `adding-an-analysis-source.md`;
   - `adding-a-timeseries-repository.md`;
   - `adding-a-metadata-backend.md`;
   - `metric-explorer-components.md`.
5. Vorher-/Nachher-Bericht mit identischer Metrikdefinition erzeugen.
6. verbliebene Schulden mit Eigentümer, Begründung und Folgeschritt dokumentieren.

Abnahme: Ein Entwickler kann jeden Erweiterungsweg vom Leitfaden zum Provider-Paket, Vertrag,
Beispiel und Contract-Test verfolgen, ohne globale Codesuche zu benötigen.

## 14. Arbeitspakete und Review-Schnitt

Ein Pull Request soll genau eines der folgenden Muster verwenden:

- Charakterisierungstest ohne Strukturänderung;
- neuer Vertrag plus Contract-Test, noch über Adapter an den alten Code gebunden;
- Migration genau eines bestehenden Providers/Backends;
- Umschalten eines Konsumenten auf den neuen Port;
- Entfernen eines nachweislich ungenutzten Compatibility-Pfads;
- reine Paketverschiebung nach abgeschlossener Entkopplung;
- neue ArchUnit-Regel nach Beseitigung ihrer Baseline-Verstöße;
- Dokumentation und Baseline-Bericht.

Nicht zulässig sind PRs, die gleichzeitig alle Sources, alle Repositories und den Metric Explorer
verschieben. Ebenso werden keine großflächigen Umbenennungen mit funktionalen Änderungen vermischt.

Jeder PR berichtet:

- betroffener Erweiterungsweg oder Komponentenvertrag;
- Baseline vor und Messung nach dem PR;
- bewusst unverändertes Verhalten;
- hinzugefügte oder entfernte Abhängigkeiten;
- Contract-/Charakterisierungstests;
- Datenmigrationswirkung;
- verbleibender Compatibility-Code und geplanter Entfernungsschritt.

## 15. Risiken und Gegenmaßnahmen

### 15.1 Versehentliche Plugin-Plattform

Risiko: Die Provider-Verträge wachsen zu einer öffentlichen, versionierten Plugin-API.  
Gegenmaßnahme: interne Sichtbarkeit, gemeinsamer Build, keine Binärkompatibilitätszusage; externe
Plugins nur nach separater Entscheidung.

### 15.2 Generische UI wird zu abstrakt

Risiko: Connection- oder Wizard-Schemas können technologiespezifische Bedienung schlecht abbilden.  
Gegenmaßnahme: gemeinsamer kleiner Kern plus optionaler Provider-spezifischer Step/Editor; keine
universelle Formularsprache entwickeln.

### 15.3 Persistierte IDs brechen Bestandsdaten

Risiko: Ablösung von Enums ändert gespeicherte Werte.  
Gegenmaßnahme: stabile ID-Tabelle, expliziter Compatibility-Mapper und Migrationstests mit echten
alten Werten.

### 15.4 H2-Abstraktion ignoriert Transaktionen

Risiko: Einzelne CRUD-Ports wirken austauschbar, fachliche Mehrtabellenoperationen sind es nicht.  
Gegenmaßnahme: Transaktionsfälle vor dem Backend-Vertrag inventarisieren und in der Contract-Suite
erzwingen.

### 15.5 Paketverschiebung erzeugt nur neue Namen

Risiko: Klassen werden verschoben, während Abhängigkeiten gleich bleiben.  
Gegenmaßnahme: Ports und Regeln zuerst, Verschiebung zuletzt; jede Phase wird an Änderungsradius und
Abhängigkeitsgraph abgenommen.

### 15.6 Große UI-Regressionen

Risiko: Chart und New-Analysis-Wizard sind zustandsreiche JavaFX-Komponenten.  
Gegenmaßnahme: Charakterisierungstests vor Extraktion, kleine vertikale Schritte, unveränderte FXML-
und CSS-Ressourcen bis zur jeweiligen UI-Phase.

## 16. Nicht-Ziele

- keine strukturelle Überarbeitung des Collectors;
- keine Aufteilung der Workbench in viele Maven-Module;
- keine neue externe Plugin- oder SDK-Kompatibilitätszusage;
- keine gleichzeitige Neugestaltung der Benutzeroberfläche;
- kein Austausch von H2, Solr, Elasticsearch oder ClickHouse als Teil dieses Refactorings;
- keine Änderung persistierter Formate ohne eigenen Migrationsschritt;
- keine LOC-Reduktion als Selbstzweck;
- keine vorgezogene Implementierung einer geplanten, aber noch nicht verfügbaren Source.

## 17. Gesamtabnahme des Refactorings

Das Vorhaben ist abgeschlossen, wenn alle folgenden Aussagen nachweisbar sind:

1. Analysis-Sources, Zeitreihen-Repositories und Metadata-Backends besitzen je einen klaren Vertrag,
   eine Registry beziehungsweise Composition-Root-Anbindung, eine Contract-Test-Suite und einen
   Entwicklerleitfaden.
2. Eine Compile-only Test-Fixture beweist für jeden Erweiterungsweg, dass keine zentrale Enum, kein
   zentraler Controller und kein fachfremder Service geändert werden muss.
3. Prometheus, SSH und Windows sind normale Analysis-Source-Provider.
4. Solr, Elasticsearch und ClickHouse sind normale Zeitreihen-Repository-Provider.
5. H2 ist ein normaler Metadata-Backend-Adapter und keine Abhängigkeit fachlicher Services.
6. Chart, Header und Legend sind getrennte Komponenten mit gerichteten, getesteten Abhängigkeiten.
7. Zwischen den definierten Zielkomponenten existieren keine Paketzyklen.
8. Bestandsdaten, bestehende Analysis-Abläufe und Repository-Konfigurationen bleiben kompatibel.
9. Der Vorher-/Nachher-Bericht verwendet dieselben Metrikdefinitionen und zeigt den tatsächlichen
   Änderungsradius, die Kopplung und verbleibende Ausnahmen.
10. Die Architektur ist aus Verzeichnisstruktur und Dokumentation verständlich, ohne Wissen über die
    frühere Multi-Modul-Struktur vorauszusetzen.
11. Der Komponentenbericht unterscheidet geschlossene Features, Bibliotheken, Infrastruktur-Ports,
    Adapter und Composition Root; er verwendet keine pauschale Interface-basierte API-Heuristik.
12. Die bekannten S202-Ausgangswerte sind im Baseline-Bericht reproduziert und auf Mapping-Artefakte,
    Bibliotheksnutzung und echte Grenzverletzungen zurückgeführt.

Erst nach Freigabe dieses Plans beginnt Phase 0. Erst nach Abnahme der Phase-0-Baseline werden
Produktivverträge oder Pakete verändert.
