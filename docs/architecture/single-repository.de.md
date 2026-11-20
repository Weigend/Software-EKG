# Konsolidierung zum Single Repository

[English](single-repository.md) | **Deutsch**

## Ziel

Der 7x-Aufbau optimiert auf den tatsächlichen Einsatz von Software-EKG: Für einen konkreten Diagnosefall wird eine Datenquelle angebunden, der Collector sammelt die benötigten Messwerte und die Workbench stellt sie als Zeitreihen dar. Eine Erweiterung soll mit wenig Kontext und ohne koordinierte Releases mehrerer EKG-Repositories möglich sein.

Das ist keine fachliche Neuentwicklung. Paketnamen, Persistenzformate, ServiceLoader-Verträge und Anwendungs-Einstiegspunkte bleiben soweit möglich erhalten. Geändert wurde die Build- und Besitzstruktur. Die Collector-Konfiguration ist seit der Konsolidierung YAML statt XML (siehe unten); vorhandene XML-Dateien werden weiter gelesen.

## Architekturentscheidung

Eine Modulgrenze bleibt nur bestehen, wenn mindestens einer dieser Gründe vorliegt:

1. Das Ergebnis ist eine separat start- oder deploybare Anwendung.
2. Code wird zur Laufzeit als Collector-Plugin gefunden.
3. Ein optionaler Adapter bringt einen deutlich eigenen Dependency-Stack mit.

UI-, Business- und API-Layer innerhalb der Workbench erfüllen diese Kriterien nicht. Sie werden durch Pakete getrennt und gemeinsam kompiliert. Beim Collector sind Source- und Repository-Adapter dagegen echte Erweiterungsgrenzen und bleiben eigene JARs.

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
| AWB API, BL, UI, Chart, Importer, Application und Distribution | `workbench` | eine Desktop-Anwendung; Schichten sind Pakete, keine Releases |
| Laufzeitklassen des AWB-SDK | `workbench` | interne Verträge der einzigen Anwendung |
| SDK-Testsupport | `workbench/src/test` | Testfixture statt ausgeliefertem Produktartefakt |

Der Reactor umfasst damit 19 Produktmodule beziehungsweise 20 Maven-Projekte einschließlich des Root-Projekts, statt zuvor 83 Projekten.

## Bewusst nicht übernommen

- Das SDK-Archetype wurde entfernt. Es erzeugte externe Importer-Projekte für eine Erweiterungsform, die nicht mehr angeboten wird.
- Das leere PostgreSQL-Source-Modul wurde entfernt.
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

## Was Module nicht mehr erzwingen

Mit den Maven-Modulen der Workbench entfiel auch, was sie nebenbei sicherstellten. Das ist
ausdrücklich ersetzt:

- **Schichten.** `LayerArchitectureTest` (ArchUnit) prüft die Paketgrenzen: Geschäftslogik ohne
  JavaFX, Oberfläche nicht an der Geschäftslogik vorbei auf die Datenhaltung, Datenhaltung ohne
  Oberfläche. Generierte Avaje-Module sind ausgenommen, weil sie alle Schichten verdrahten.
- **Abhängigkeiten.** Die Workbench bindet `collector-core` ohne dessen JavaScript-Engine
  (GraalJS) und WebSocket-Server ein. Beide braucht nur der eigenständige Collector: GraalJS für
  berechnete Metriken (Quelle `calc`), und auch dort wird die Engine erst gestartet, wenn eine
  solche Metrik abgefragt wird.
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

Importer sind Teil der Workbench. Neue Importer werden im passenden `io.softwareecg.awb.importer`-Paket ergänzt und im selben Reactor getestet. Damit entfallen SDK-Versionierung, Archetype, separates Deployment und binäre Kompatibilitätsversprechen zwischen eng gekoppelten Komponenten.

## Verifikation der Migration

Die Migration wird über drei Ebenen abgesichert:

- der gesamte Reactor kompiliert und paketiert aus einem Root-POM;
- die Tests aller Source-, Repository-, Collector- und Workbench-Komponenten laufen in der neuen Struktur;
- CLI-Integrationstests prüfen die Laufzeitauflösung der Collector-Plugins, Workbench-Tests die eingebettete Prometheus-Erfassung, Speicherung, Importer und JavaFX-Komponenten.

Die alten Java-Paketnamen wurden absichtlich nicht flächig umbenannt. Das reduziert das Migrationsrisiko und hält Konfigurationen und Reflection-Zugriffe kompatibel. Eine spätere Paketbereinigung kann unabhängig davon erfolgen und sollte nur fachliche Grenzen verbessern, nicht erneut Maven-Module simulieren.
