<p align="center">
  <img src="docs/images/logo.png" alt="Software-EKG" width="420">
</p>

# Software-EKG 7x

Software-EKG erfasst gezielt technische Messwerte und macht sie in einer JavaFX-Workbench als Zeitreihen analysierbar. Es ist kein Grafana-Ersatz, sondern ein Werkzeugkasten für konkrete Performance- und Diagnosefälle: Eine Datenquelle anbinden, nur die relevanten Werte sammeln, lokal oder in einer entfernten Datenbank speichern und unmittelbar untersuchen.

Dieses Repository ist die konsolidierte Variante von AWB, AWB-SDK und Collector. Es gibt keine Gitlinks oder voneinander versionierte EKG-Teilprojekte mehr. Implementierungsdetails der Workbench sind Java-Pakete statt Maven-Artefakte; echte Laufzeit- und Erweiterungsgrenzen des Collectors bleiben Module.

![Software-EKG Workbench mit Explorer, Zeitreihen-Chart der JVM-Heap-Nutzung und About-Dialog](docs/images/fullscreen.png)

## Struktur

| Verzeichnis | Zweck |
| --- | --- |
| `workbench` | Eine JavaFX-Anwendung mit UI, Chart Engine, Importern, lokaler Speicherung und eingebettetem Collector |
| `collector/core` | Konfiguration, Collector Engine, SSH, Speicher- und Source-Verträge |
| `collector/sources/*` | Per ServiceLoader ladbare Datenquellen, etwa Prometheus, Unix, Windows, JMX und Logs |
| `collector/repositories/*` | CSV-, JSON-, Solr- und Elasticsearch-Speicheradapter einschließlich Kompression |
| `collector/apps/cli` | Headless Collector und dessen Distribution |
| `collector/apps/ui` | Bestehende eigenständige Collector-UI |
| `collector/apps/jolokia-agent` | Jolokia-Agent als WAR |
| `build/collector-parent` | Gemeinsame interne Build-Konfiguration der Collector-Module |

Die historische Zuordnung und die bewusst entfernten Teile sind in [docs/architecture/single-repository.md](docs/architecture/single-repository.md) dokumentiert.

## Voraussetzungen

- JDK 25 mit JavaFX 25 (zum Beispiel Liberica Full JDK)
- Maven 3.9 oder neuer
- ein Display für JavaFX-Tests; in headless Linux-Umgebungen funktioniert `xvfb-run`
- WFX 1.4.1 im lokalen Maven-Repository

WFX ist ein eigenständiges UI-Framework und bleibt eine normale externe Abhängigkeit. Die passende Version wird einmalig installiert:

```bash
./install-wfx.sh
```

## Bauen und testen

```bash
# kompletter Build
mvn clean package

# kompletter Testlauf unter Linux
xvfb-run -a mvn test

# schneller Build ohne Tests
mvn clean package -DskipTests
```

`.mvn/jvm.config` gibt Maven zwei JVM-Optionen mit, damit Java 25 nicht bei jedem Aufruf vor
Bibliotheken warnt, die Maven selbst mitbringt (jansi, guava).

Die Core-Tests öffnen lokale SSH- und WebSocket-Ports. In eingeschränkten CI-Schritten können sie getrennt ausgeführt werden:

```bash
mvn test -Dskip.core.tests=true
mvn -pl collector/core test
```

## Starten

Aus dem Quellbaum, jeweils mit vorherigem Build (ohne Tests):

```bash
./run-workbench.sh                  # Workbench
./run-collector.sh                  # Collector mit Beispielwerten (Quelle "mock")
./run-collector.sh meine.yaml       # Collector mit eigener Konfiguration
```

Mit `--no-build` als erstem Argument starten beide, was zuletzt gebaut wurde. Die Skripte
verwenden die Startskripte der Distributionen; es gibt also nur einen Weg, wie die Programme
gestartet werden.

## Distributionen

```bash
mvn package -DskipTests           # startbare Verzeichnisse
mvn package -DskipTests -Pdist    # zusätzlich mit eigener Java-Laufzeit und als ZIP
```

| | Verzeichnis | ZIP (`-Pdist`) | Start |
| --- | --- | --- | --- |
| Workbench | `workbench/target/software-ekg-workbench-<Version>/` | `workbench/target/software-ekg-workbench-<Version>.zip` | `bin/software-ekg.sh`, `bin\software-ekg.bat` |
| Collector | `collector/apps/cli/product/software-ekg-collector-<Version>/` | `collector/apps/cli/product/software-ekg-collector-<Version>.zip` | `ekg-collector.sh`, `ekg-collector.cmd`; als Dienst `ekg-collector start\|stop\|status` |

Ohne `-Pdist` brauchen die Distributionen ein installiertes Java 25 (die Workbench eines mit
JavaFX). Mit `-Pdist` schneidet `jlink` die Laufzeit aus dem JDK, mit dem gebaut wird; das ZIP
passt dann zum Betriebssystem des Builds und braucht kein installiertes Java. Für die Workbench
muss dieses JDK JavaFX enthalten (Liberica Full JDK).

Die Version steht an genau einer Stelle, `revision` im Root-`pom.xml`. Programme und Skripte
lesen sie aus dem Build (`build.properties`, `version.properties`) und nennen keine eigene.

## Collector-Konfiguration

Eine Konfiguration ist eine YAML-Datei mit vier Teilen: *was* gemessen wird (`metricGroups`),
*von welchen Prozessen* (`processGroups`), *auf welchen Maschinen* (`hostGroups`) und den
`collectors`, die die drei kombinieren:

```yaml
metricGroups:
  host:
    type: unix                      # die Quelle: top über SSH
    metrics:
      - '\CPU\us'
      - '\Load\one'

processGroups:
  system:
    - name: global
      source: '${REF:hostAddress}:22'

hostGroups:
  db:
    username: ekg
    password: '${ENV:EKG_SSH_PASSWORD}'   # Passwörter stehen nie in der Datei
    hosts: [db-01, db-02]

collectors:
  - metricGroup: host
    processGroup: system
    hostGroup: db
    interval: 15s
```

Alle Einstellungen erklärt `config/current/config-template.yaml` in der Collector-Distribution;
daneben liegen Beispiele je Quelle. Die Datei wird streng gelesen: Ein unbekannter Schlüssel ist
ein Fehler mit Zeilenangabe, keine stillschweigend wirkungslose Einstellung.

Konfigurationen im XML-Format früherer Versionen (`config.xml`, ab Collector 6.1) werden weiterhin
gelesen und lassen sich umwandeln:

```bash
./ekg-collector.sh --convert config.xml      # schreibt config.yaml daneben
```

Die Workbench erzeugt für ihre Datenquellen dieselben YAML-Dateien (Export im Dialog
"Data Sources") und liest ältere, als XML gespeicherte weiter.

## Daten der Workbench

Projekte, Zeitreihen und Zugangsdaten liegen im Benutzerverzeichnis, nie im Quellbaum oder in
der Distribution:

| Ort | Inhalt |
| --- | --- |
| `~/.ekg/metadata.mv.db` | Projekte, Repositories, Lesezeichen, Datenquellen |
| `~/.ekg/solr` | Zeitreihen (eingebettetes Solr); verlegbar mit `EKG_SOLR_HOME` |
| `~/.ekg/logs` | Logdateien beim Start über `software-ekg.sh` |

Ein `mvn clean` löscht deshalb keine Messdaten mehr.

## Architekturregeln

Die Workbench ist ein Modul; ihre Schichten sind Pakete. Dass die Paketgrenzen halten - keine
JavaFX-Typen in der Geschäftslogik, kein Zugriff der Oberfläche an der Geschäftslogik vorbei auf
die Datenhaltung -, prüft `LayerArchitectureTest` (ArchUnit) bei jedem Testlauf.

## Erweiterungen

Eine neue Collector Source oder ein neues Repository ist weiterhin ein kleines Plugin mit ServiceLoader-Registrierung. Ein neuer Workbench-Importer wird dagegen direkt als Paket in `workbench` implementiert. Das frühere öffentliche SDK und sein Archetype sind entfallen, weil sie eine stabile Drittanbieter-API suggerierten, die für den heutigen, dienstleistungsnahen Einsatz nicht benötigt wird.

## Lizenz

Apache License 2.0, siehe [LICENSE](LICENSE) und [NOTICE](NOTICE).
