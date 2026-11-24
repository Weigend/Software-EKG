# Konsolidierung zum Single Repository

[English](single-repository.md) | **Deutsch**

## Ziel

Der 7x-Aufbau optimiert auf den tatsächlichen Einsatz von Software-EKG: Für einen konkreten Diagnosefall wird eine Datenquelle angebunden, der Collector sammelt die benötigten Messwerte und die Workbench stellt sie als Zeitreihen dar. Eine Erweiterung soll mit wenig Kontext und ohne koordinierte Releases mehrerer EKG-Repositories möglich sein.

Die Konsolidierung war keine fachliche Neuentwicklung. Persistenzformate und die ServiceLoader-Verträge des Collectors blieben erhalten. Geändert wurde zuerst die Build- und Besitzstruktur; danach wurde die Workbench neu aus [Komponenten](workbench-components.de.md) aufgebaut, mit Paketnamen, die sagen, was sie tun. Die Collector-Konfiguration ist seit der Konsolidierung YAML statt XML (siehe unten); vorhandene XML-Dateien werden weiter gelesen.

## Architekturentscheidung

Eine Modulgrenze bleibt nur bestehen, wenn mindestens einer dieser Gründe vorliegt:

1. Das Ergebnis ist eine separat start- oder deploybare Anwendung.
2. Code wird zur Laufzeit als Collector-Plugin gefunden.
3. Ein optionaler Adapter bringt einen deutlich eigenen Dependency-Stack mit.

Die Teile der Workbench erfüllen diese Kriterien nicht. Sie sind Komponenten in Paketen und werden gemeinsam kompiliert; [Regeln](#was-die-struktur-heute-sicherstellt) halten sie auseinander. Beim [Collector](collector-components.de.md) sind Source- und Repository-Adapter dagegen echte Erweiterungsgrenzen und bleiben eigene JARs.

## Zuordnung alt zu neu

| Bisher | Jetzt | Begründung |
| --- | --- | --- |
| Aggregator mit vier Gitlinks | ein Git-Repository und ein Maven-Reactor | eine Änderung, ein Review, eine Version |
| `ssh` | `collector/core` | ausschließlich Infrastruktur des Collectors und der Unix-Anbindungen |
| Collector Common, Config, Memory, Connector, Repository API, Source Common und Control | `collector/core` | stark gekoppelte Laufzeitbasis ohne eigenständigen Deploymentwert |
| Collector Source-Implementierungen | `collector/sources/*` | echte, per ServiceLoader ladbare Plugins |
| CSV, JSON, RDB, Solr und Elasticsearch | `collector/repositories/*` | optionale Speicheradapter mit unterschiedlichen Abhängigkeiten |
| Collector Delivery | `collector/apps/cli` | eigenständig startbares Produkt |
| Collector UI und Jolokia WAR | `collector/apps/ui`, `collector/apps/jolokia-agent` | eigenständige Deployments |
| AWB API, BL, UI, Chart, Importer, Application und Distribution | `workbench` | eine Desktop-Anwendung; Komponenten sind Pakete, keine Releases |
| Laufzeitklassen des AWB-SDK | `workbench` | interne Verträge der einzigen Anwendung |
| SDK-Testsupport | `workbench/src/test` | Testfixture statt ausgeliefertem Produktartefakt |

Der Reactor umfasst damit 17 Module beziehungsweise 18 Maven-Projekte einschließlich des Root-Projekts, statt zuvor 83 Projekten. Jedes Produkt hat ein eigenes POM, `collector/pom.xml` und `workbench/pom.xml`; das Root-POM listet diese zwei.

## Bewusst nicht übernommen

- Das SDK-Archetype wurde entfernt. Es erzeugte externe Importer-Projekte für eine Erweiterungsform, die nicht mehr angeboten wird.
- Das leere PostgreSQL-Source-Modul wurde entfernt, später auch das JSON-Repository des Collectors und der WebSocket-Connector, die nichts benutzte.
- Die nicht in die Anwendung eingebundenen AWB-Sandbox-Programme (einmalige
  Konverter, Repository-Analyse und experimentelle Prometheus-Clients) wurden
  nicht als Produktcode übernommen.
- Die alten betriebssystemspezifischen AWB-ZIP-Wrapper wurden nicht übernommen.
  Die Distribution der Workbench entsteht direkt aus dem einen `workbench`-Modul
  (Verzeichnis mit Startskripten; mit dem Profil `dist` zusätzlich Java-Laufzeit
  und ZIP), statt wieder drei Anwendungsmodule einzuführen.
- Klon-Skripte, Gitlinks und die CI-Logik zum Zusammensuchen separater EKG-Repositories wurden entfernt.
- Historische manuelle Root-Testdaten wurden nicht dupliziert. Testdaten, die automatisierte Tests benötigen, liegen weiterhin beim jeweiligen Modul.
- Generierte Build-Ausgaben, alte Logs und lokale IDE-/Agent-Einstellungen gehören nicht zum neuen Repository.

WFX bleibt bewusst extern. Es ist ein allgemeines JavaFX-Framework mit eigenem Lebenszyklus und wird als versionierte Maven-Abhängigkeit verwendet. `install-wfx.sh` stellt diese Abhängigkeit auf einer neuen Entwicklungsmaschine bereit.

## Was die Struktur heute sicherstellt

Mit den Maven-Modulen der Workbench entfiel auch, was sie nebenbei sicherstellten. An ihre Stelle
sind Regeln getreten, und die Regeln stellen mehr sicher als die Module. Ein Modul sagte nur, welches
andere Modul es benutzen darf. Jede öffentliche Klasse darin war dann benutzbar, und innerhalb eines
Moduls war alles möglich.

| | 83 Maven-Projekte | Heute |
| --- | --- | --- |
| Richtung der Abhängigkeiten | zwischen Modulen | zwischen Komponenten, Bibliotheken und allen Paketen, auf jeder Ebene |
| Zyklen | zwischen Modulen unmöglich, innerhalb eines Moduls unbemerkt | auf keiner Ebene, weder zwischen Komponenten noch zwischen den Teilen eines Pakets |
| Außenkante | jede öffentliche Klasse | nur `api` und `spi` einer Komponente; alles andere ist innen |
| Verträge | nach Absprache | `api` implementiert die eigene Komponente, `spi` enthält nur Verträge für andere |
| Techniken | getrennt, wenn sie eigene Module hatten | Solr, Elasticsearch und ClickHouse kennen einander nicht, die Arten von Datenquellen auch nicht |
| Geschäftslogik | frei von JavaFX durch die Wahl der Abhängigkeiten | frei von JavaFX durch eine Regel, wie auch die Datenhaltung |
| FXML und CSS | nicht geprüft | benutzen andere Komponenten nur über `api` und `spi` und finden ihre Dateien und Controller |

Drei Tests prüfen das bei jedem Build:

- **`LayerArchitectureTest`** (ArchUnit) prüft den Code: Außenkanten, Schichten, Zyklen, Techniken.
  Die generierten Module der Dependency Injection sind ausgenommen, weil sie alle Komponenten
  verdrahten.
- **`ViewArchitectureTest`** prüft FXML und CSS, wo Klassen und Dateien als Text stehen.
- **`ResourceReferencesTest`** prüft, dass Ansichten, Stylesheets und Icons dort liegen, wo sie gesucht
  werden.

**S202** zeigt die Struktur und misst sie unabhängig von diesen Tests aus dem Bytecode: Zugriffe an einer
`api` vorbei, APIs, die Inneres nach außen geben, Zyklen und Abhängigkeiten gegen die Schichten. Die
Bilder und der aktuelle Bericht stehen in [den Komponenten der Workbench](workbench-components.de.md):
kein Zugriff an einer `api` vorbei, kein Paketzyklus und vier Klassenzyklen innerhalb je eines Pakets, die
bekannt sind und bleiben. S202 findet, wofür niemand eine Regel geschrieben hat, zum Beispiel ein Paket
am falschen Ort; was es findet, wird zur Regel, wenn es nicht wiederkommen darf.

Beim Collector sind weiterhin die Module die Grenzen. Seine Pakete beschreiben
[die Komponenten des Collectors](collector-components.de.md), mit dem, was S202 dazu meldet; Regeln gibt
es dafür noch nicht.

Über die Struktur hinaus:

- **Abhängigkeiten.** Die Workbench bindet `collector-core` ohne dessen JavaScript-Engine
  (GraalJS) ein. Sie braucht nur der eigenständige Collector, für berechnete Metriken (Quelle
  `calc`), und auch dort wird die Engine erst gestartet, wenn eine solche Metrik abgefragt wird.
- **Daten außerhalb des Builds.** Der Solr-Index liegt unter `~/.ekg/solr`, nicht unter
  `target/`. Die Solr-Konfiguration wird bei jedem Start aus der Anwendung dorthin kopiert.
- **Eine Version.** `revision` im Root-POM; Programme und Skripte lesen sie aus dem Build.

## Collector-Konfiguration als YAML

Die Konfiguration des Collectors ist ein formatneutrales Modell
(`io.softwareecg.collector.config.model`), das als YAML gelesen und geschrieben wird. Das XSD,
die daraus generierten JAXB-Klassen und die Abhängigkeit auf JAXB sind entfallen. Geprüft wird
beim Lesen: unbekannte und doppelte Schlüssel, fehlende Angaben und Verweise auf Gruppen, die es
nicht gibt, jeweils mit Ort. XML-Dateien ab Collector 6.1 werden über einen kleinen Leser ohne
JAXB in dasselbe Modell überführt; das ältere Format (`CounterGroups`) wird nicht mehr gelesen.
Die Workbench baut für ihre Datenquellen das Modell auf, statt Text zusammenzusetzen.

## Erweiterungsmodell

### Collector Source

Eine Source implementiert die bestehenden Collector-Verträge und registriert ihre Implementierung über `META-INF/services`. Sie kann anschließend sowohl in der Headless-Distribution als auch – nach Aufnahme als Dependency – in der Workbench verwendet werden. Diese Grenze ist sinnvoll für kundenspezifische APIs, Geräte, Roboter, Switches oder proprietäre Plattformen.

### Workbench-Importer

Importer sind Teil der Workbench. Neue Importer werden in einem eigenen Paket unter `io.softwareecg.awb.importer.adapter` ergänzt, implementieren die Verträge in `importer.spi` und werden im selben Reactor getestet. Damit entfallen SDK-Versionierung, Archetype, separates Deployment und binäre Kompatibilitätsversprechen zwischen eng gekoppelten Komponenten.

## Verifikation der Migration

Die Migration wird über vier Ebenen abgesichert:

- der gesamte Reactor kompiliert und paketiert aus einem Root-POM;
- die Tests aller Source-, Repository-, Collector- und Workbench-Komponenten laufen in der neuen Struktur;
- CLI-Integrationstests prüfen die Laufzeitauflösung der Collector-Plugins, Workbench-Tests die eingebettete Prometheus-Erfassung, Speicherung, Importer und JavaFX-Komponenten;
- End-to-End-Tests starten die ganze Workbench und bedienen sie über ihre Oberfläche. Sie laufen mit jedem Build.

Während der Migration blieben die alten Java-Paketnamen absichtlich erhalten, das verringerte ihr Risiko. Die Bereinigung kam danach und ist für die Workbench abgeschlossen: Die Pakete `sdk`, `common`, `da`, `metricanalyzer` und `project` gibt es nicht mehr, und jedes Top-Paket ist eine Komponente oder eine Bibliothek. Die Grenzen sind fachliche; die früheren Maven-Module wurden nicht nachgebaut.
