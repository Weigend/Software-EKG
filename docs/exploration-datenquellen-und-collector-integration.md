# Exploration: Datenquellen und Collector-Integration

Stand: 19.11.2026. Dieses Dokument exploriert einen durchgängigen Kernanwendungsfall für das Software-EKG. Es beschreibt ein mögliches Produkt- und Architekturziel, aber noch keinen beschlossenen Umbauplan. Die Machbarkeitsaussagen wurden gegen den aktuellen Stand von AWB, AWB-SDK und Collector geprüft.

## Ausgangslage

Der Kernfehler ist weniger eine fehlende Funktion als ein fehlendes Produktmodell: AWB, Importer und Collector sind technisch vorhanden, aber der Nutzer muss selbst verstehen, wie daraus ein funktionierender Datenfluss entsteht.

Die Lösung sollte daher nicht lediglich einen Konfigurationseditor für den Collector bauen. Der Collector sollte für normale Nutzer weitgehend unsichtbar werden. In der AWB arbeitet der Nutzer mit einem Analyseprojekt und fügt diesem Datenquellen hinzu. Die AWB erzeugt daraus automatisch Import- oder Sammelpläne und entscheidet, ob sie diese selbst, mit einem lokalen Collector oder über einen Remote Collector ausführt.

## Kernanwendungsfall

> Als Performance-Analyst möchte ich in der AWB eine Umgebung anlegen, deren Datenquellen verbinden und innerhalb weniger Minuten erste vergleichbare Zeitreihen sehen, ohne Collector-Konfigurationen, CSV-Verzeichnisse oder Importer kennen zu müssen.

Der vollständige Ablauf:

1. AWB starten.
2. „Neue Analyse“ wählen.
3. Name und optional Zweck der Untersuchung angeben.
4. Eine oder mehrere Quellen hinzufügen:
   - Prometheus
   - Linux/Unix per SSH
   - Windows Server
   - Java-Anwendung per JMX/Jolokia
   - Logdateien
   - PostgreSQL
   - CSV/Datei
5. Verbindung testen und gefundene Systeme, Prozesse und Metriken sehen.
6. Eine sinnvolle Voreinstellung übernehmen oder Metriken auswählen.
7. Entscheiden:
   - einmalig importieren,
   - regelmäßig sammeln,
   - historischen Bestand plus laufende Sammlung einrichten.
8. Optional einen Remote Collector als Ausführungsort wählen.
9. Vorschau der ersten Daten erhalten.
10. Sammlung starten.
11. Direkt in eine vorbereitete erste Vergleichsansicht wechseln.
12. Später im Projekt sehen:
    - welche Quellen existieren,
    - wann zuletzt Daten ankamen,
    - welche Sammlung fehlschlug,
    - wie weit der lokale Bestand reicht.

Der Nutzer formuliert dabei seine Absicht. Technische Artefakte wie `config.yaml`, Collector-Jobs, CSV-Dateien und Importstrategien sind Implementierungsdetails.

## Was bereits vorhanden ist

Die Ausgangslage ist besser, als die heutige UX vermuten lässt:

- Die AWB speichert Projekt- und Repository-Metadaten bereits lokal in H2. Ein Projekt ist einem Ziel-Repository zugeordnet und besitzt Darstellungsmerkmale wie `CLASSIC`, `HYBRID` oder `CLOUD_NATIVE`.
- Es existieren Importer für Collector-CSV und Prometheus sowie weitere Spezialimporte.
- Der Prometheus-Importer unterstützt bereits zwei Betriebsarten:
  - Metadaten lokal speichern und Werte bei Bedarf aus Prometheus lesen („Split Source“),
  - Werte vollständig in das EKG-Repository importieren, einschließlich eines manuell gestarteten Delta-Imports.
- Der Prometheus-Client kann Metriknamen, Serien und Labels abfragen. Damit ist ein wesentlicher Teil der Discovery technisch schon möglich.
- Im Collector existieren registrierte Source-Implementierungen für JMX, Jolokia, Unix `top`, `iostat`, `df`, Windows Performance Counter, Logs und Remote Logs. Wildcard-Expansion kann bei mehreren Quellen als Grundlage einer Metrikvorschau dienen. Vor einer Produktfreigabe braucht dennoch jede Quelle einen aktuellen Kompatibilitäts- und Integrationstest.
- Das Collector-Konfigurationsmodell kann Host-, Prozess- und Metrikgruppen kombinieren und prüft die Konfiguration (`config.yaml`, früher `config.xml` mit XSD) beim Lesen.
- Der Collector kann über austauschbare Repository-Implementierungen nach CSV, Solr oder Elasticsearch schreiben. JSON-Code ist vorhanden, gehört aber nicht zur aktuellen Standardauslieferung.
- Der CSV-Importer versteht die Collector-Formate verschiedener Versionen. Damit ist ein kompatibler Übergangsweg vom Collector in die AWB vorhanden.
- Lokale Speicherung in Solr beziehungsweise ClickHouse passt zum Produktziel einer vom Produktionssystem entkoppelten Langzeitanalyse.

Nicht alles, was README oder Modulname nahelegen, ist bereits produktiv vorhanden. Das PostgreSQL-Source-Modul enthält aktuell keine Implementierung. Auch die Log-Event-Module sind kein allgemein nutzbarer Quellenadapter. Diese Quellen dürfen im Wizard erst angeboten werden, wenn ein Capability-Check ihre tatsächliche Verfügbarkeit bestätigt.

Die vorhandene Projektanlage fragt außerdem schon zu früh nach Repository, Projekttyp und Cloud-Plattform. Das sind technische Entscheidungen, bevor der Nutzer überhaupt eine Quelle verbunden hat. Projekt-Flavors können intern nicht sofort entfallen, weil sie die Filter- und Chartdarstellung steuern. Der Wizard kann sie aber aus den gewählten Quellen ableiten und nur bei Bedarf als erweiterte Option zeigen.

## Ergebnis der Machbarkeitsprüfung

Das Zielbild ist umsetzbar, aber nicht als reine UI-Erweiterung. Die größten Eingriffe liegen nicht im Wizard, sondern im Projektmodell, im zuverlässigen Delta-Import und im Lifecycle des Collectors.

| Vorschlag | Machbarkeit | Befund im aktuellen Code | Konsequenz |
| --- | --- | --- | --- |
| Geführter Prometheus-Import | hoch | Client, Authentisierung, Serien-/Label-Abfrage, Voll- und Delta-Import existieren | bester erster vertikaler Schnitt |
| Prometheus regelmäßig importieren, solange die AWB läuft | hoch | Importtask existiert; Strategy hängt aber noch an einer JavaFX-Property des Dialogs, Scheduler und persistierter Laufstatus fehlen | Importkern von UI-Zustand trennen und Scheduling-Dienst ergänzen |
| Mehrere Quellen in einem Projekt | mittel | Projekt hat nur `importerId`; Importquellen sind genau einmal je Projektname gespeichert | neues Datenquellen- und Planmodell samt H2-Migration nötig |
| Lokalen Collector aus der AWB starten | mittel | Distribution und Kommandozeilenstart existieren; der Kern ist aber ein einmalig gestarteter, statischer Prozess | zunächst als separaten Kindprozess verwalten, nicht in die AWB einbetten |
| Collector-Konfiguration live ändern | gering bis mittel | keine Reload-API; Threads, Jobliste und Connector sind global/statisch | für den Anfang Konfiguration validieren und Collector kontrolliert neu starten |
| Sammlung fortsetzen, wenn die AWB geschlossen ist | mittel | Collector läuft autonom; eingebettetes Solr ist jedoch nur in der AWB-JVM erreichbar | persistenter Collector-Spool oder extern erreichbares Repository erforderlich |
| Remote Collector verwalten | mittel bis hoch | keine Management-API, keine Registrierung, keine sichere Secret-Übertragung | eigener späterer Ausbauschritt, nicht Teil des MVP |
| Unix/SSH integrieren | mittel bis hoch | SSH-Ausführung, Parser und Wildcard-Expansion existieren | nach einem kleinen Discovery-/Diagnose-Adapter gut wiederverwendbar |
| JMX/Jolokia integrieren | mittel | MBean-Wildcards existieren; Verbindungsversuche können lange blockieren | Timeouts, Abbruch und separate Discovery erforderlich |
| Windows integrieren | mittel, plattformabhängig | Zugriff verwendet native Windows-DLLs und Windows Performance Counter | Collector muss auf Windows laufen; nicht als universeller lokaler Agent versprechen |
| PostgreSQL integrieren | derzeit gering | Modul ist leer | neue Source-Implementierung erforderlich |

### Technische Grenzen, die das Zielbild beeinflussen

1. **Das eingebettete Solr ist kein Netzwerkdienst.** Die AWB verwendet einen `EmbeddedSolrServer` in ihrem eigenen Prozess. Ein separat laufender lokaler Collector kann deshalb nicht direkt in den eingebetteten AWB-Bestand schreiben.
2. **Der Collector ist noch kein steuerbarer Agent.** Er liest beim Start eine XML-Datei, erzeugt Threads und wartet auf deren Ende. Es gibt keine API für Planinstallation, Reload oder strukturierten Status; Fehler landen vor allem im Log.
3. **Eine Importquelle pro Projekt ist fest eingebaut.** `ImporterSourceRepository` wird über den Projektnamen gesucht; mehr als ein Treffer gilt als Fehler. Für mehrere Quellen braucht es neue Identitäten und Beziehungen, nicht nur zusätzliche UI-Felder.
4. **Secrets werden heute als normale H2-Spalten gespeichert.** Passwörter und Refresh Tokens sind damit noch kein Secret Store. Zusätzlich akzeptiert der Prometheus-Client derzeit jedes TLS-Zertifikat und deaktiviert die Hostname-Prüfung.
5. **Das aktuelle Zeitreihenschema kennt keine `sourceId`, `resourceId`, Einheit oder Qualitätsmarkierung.** Ein neues internes Envelope ist sinnvoll, darf aber nicht unbemerkt eine Migration aller Solr-, Elasticsearch- und ClickHouse-Adapter auslösen.
6. **Der Prometheus-Vollimport skaliert noch nicht für beliebige Bestände.** Er listet alle Metriken und Serien, hält die Definitionen im Speicher und führt anschließend mindestens eine Werteabfrage pro Serie aus. Ohne Auswahl, Chunking und Grenzen kann ein großer Prometheus-Bestand die AWB oder den Server stark belasten.
7. **„Split Source“ ist eine Projekteigenschaft.** Fetcher und `importerId` werden auf Projektebene ausgewählt. Ein Projekt kann daher heute nicht transparent lokale Collector-Daten mit on-demand geladenen Prometheus-Werten mischen.
8. **Der Prometheus-Import ist noch UI-gebunden.** Die lokale Strategy erhält ihre Quellkonfiguration über eine `ReadOnlyProperty` des Importdialogs. Ein Zeitplan braucht stattdessen einen headless aufrufbaren Importservice mit unveränderlicher Run-Konfiguration.

Diese Grenzen widerlegen den Vorschlag nicht. Sie bestimmen aber die Reihenfolge: zuerst den vorhandenen Prometheus-Weg produkttauglich machen, danach einen lokalen Collector über einen unsichtbaren Spool anbinden und erst anschließend eine Remote-Control-API entwickeln.

## Was fehlt

### 1. Gemeinsames fachliches Modell

Heute existieren nebeneinander:

- AWB-Projekt
- Importer-Konfiguration
- Collector-Konfiguration
- Repository-Konfiguration
- CSV-Ausgabe
- Remote-Datenbank

Es fehlt ein verbindendes Modell:

```text
Analyseprojekt
 ├── Datenquellen
 ├── erfasste Systeme und Prozesse
 ├── Metrikauswahl
 ├── Sammelpläne
 ├── Ausführungsorte
 └── lokaler Datenbestand
```

Ein „Collector-Projekt“ ist aus Nutzersicht zu technisch. Besser wäre schlicht „Analyse“ oder „Arbeitsbereich“.

Das neue Modell sollte nicht direkt in `Project` eingebaut werden. Belastbarer ist eine getrennte, referenzierte Struktur:

```text
AnalysisProject
 ├── DataSourceDefinition 1..n
 │    ├── type
 │    ├── endpoint und nicht geheime Optionen
 │    └── credentialRef
 ├── CollectionPlan 0..n
 │    ├── sourceId
 │    ├── selection und dimensionMapping
 │    ├── mode: ON_DEMAND | SCHEDULED | CONTINUOUS
 │    ├── schedule
 │    └── runtimeId
 ├── IngestionCursor 0..n
 └── CollectionRun 0..n
```

`DataSourceDefinition` beschreibt die Verbindung, `CollectionPlan` die gewünschte Datenerfassung und `CollectionRun` einen konkreten Lauf. Diese Trennung verhindert, dass Zugangsdaten, Zeitplan und letzter Fehler in einer einzigen Konfigurationsentität vermischt werden.

Die neuen Tabellen können zunächst zusätzlich zu den bestehenden Projekt- und Repository-Tabellen in H2 entstehen. Bestehende Prometheus-Quellen lassen sich beim Lesen als Legacy-Quelle abbilden; eine sofortige destruktive Migration ist nicht nötig.

### 2. Quellenkatalog

Die AWB benötigt einen zentralen Katalog aller Quellentypen. Ein Quellenadapter muss nicht nur Daten lesen, sondern auch beschreiben können:

- Welche Verbindungsdaten werden benötigt?
- Kann er die Verbindung testen?
- Kann er Hosts, Prozesse oder Metriken entdecken?
- Unterstützt er historische Daten?
- Unterstützt er regelmäßige Sammlung?
- Wo darf er ausgeführt werden?
- Welche Standardmetriken sind empfehlenswert?
- Welche Berechtigungen braucht er?
- Welche Secret-Felder besitzt er?

Die bestehenden AWB-Importer und Collector-Sources erfüllen jeweils nur Teile dieses Vertrags.

Der Quellenkatalog sollte deshalb zunächst ein AWB-internes SPI sein. Die alten `ImportModule`- und `CounterSource`-Schnittstellen werden durch Adapter eingebunden. Eine sofortige Vereinheitlichung beider APIs wäre riskant: `ImportModule` verarbeitet historische Datenströme, während `CounterSource` im Wesentlichen periodisch einen Satz numerischer Werte abfragt.

Ein Quellentyp veröffentlicht mindestens folgende Capabilities:

```text
HISTORICAL_READ
SCHEDULED_READ
CONTINUOUS_READ
DISCOVER_RESOURCES
DISCOVER_METRICS
LOCAL_RUNTIME
REMOTE_RUNTIME
```

Der Wizard zeigt nur Funktionen, die der konkrete Adapter und der gewählte Ausführungsort tatsächlich beherrschen. Damit werden beispielsweise PostgreSQL oder Windows nicht allein aufgrund eines Modulnamens angeboten.

### 3. Discovery und Vorschau

Der heutige Collector erwartet weitgehend, dass der Nutzer Hosts, Prozesse und Metriken bereits kennt und als XML formuliert.

Benötigt werden Discovery-Funktionen:

- Prometheus: Labels, Jobs, Instanzen und verfügbare Metriken entdecken.
- SSH: Betriebssystem erkennen, verfügbare Kommandos prüfen, `top`, `vmstat`, `iostat`, `df` testen.
- Java: JMX/Jolokia-Endpunkt erkennen, MBeans abfragen, JVM-Typ und Version feststellen.
- Windows: erreichbare Performance Counter auflisten.
- Logs: Format anhand einiger Zeilen vorschlagen.
- PostgreSQL: Version und verfügbare Statistiken erkennen.

Jede Quelle sollte vor dem Speichern eine kleine Datenvorschau liefern. „Verbindung erfolgreich“ ist zu wenig; entscheidend ist: „Diese fünf Zeitreihen würden ankommen.“

Discovery muss begrenzt und abbrechbar sein. Gerade JMX-Wildcard-Auflösung und Prometheus-Serienabfragen können teuer oder blockierend sein. Jeder Adapter braucht deshalb Timeout, Cancellation, Ergebnislimit und eine strukturierte Diagnose. Ein „Test“ darf niemals den eigentlichen, potenziell sehr großen Import auslösen.

### 4. Collector Control API

Der Collector besitzt intern Jobsteuerung und Jobzustände, aber keine geeignete Management-Schnittstelle für die AWB.

Ein lokaler oder entfernter Collector benötigt eine versionierte API, beispielsweise:

- Fähigkeiten und Version abfragen
- Verbindung oder Quelle testen
- Discovery ausführen
- Konfiguration validieren
- Sammelplan installieren
- Plan starten, pausieren und stoppen
- Status und Fehler abrufen
- Datenfortschritt melden
- Konfiguration revisionssicher aktualisieren
- Logs beziehungsweise Diagnoseinformationen abrufen

Die AWB darf nicht direkt eine fremde `config.yaml` überschreiben und anschließend hoffen, dass der Collector korrekt startet.

Für den lokalen MVP ist jedoch noch keine vollständige Control API nötig. Der vorhandene Collector lässt sich risikoärmer als verwalteter Kindprozess kapseln:

1. Die AWB erzeugt ein unveränderliches Runtime-Bundle aus XML, Properties und Secret-Referenzen.
2. Vor dem Start wird das Bundle strukturell und semantisch validiert.
3. Die AWB startet einen eigenen Collector-Prozess und überwacht PID, Exit-Code und Log.
4. Eine Änderung erzeugt eine neue Bundle-Revision und führt zu einem kontrollierten Neustart.
5. Die letzte funktionierende Revision bleibt für Rollback erhalten.

Dieses Supervisor-Modell passt zum aktuellen statischen Collector-Lifecycle. Erst für Remote-Betrieb oder unterbrechungsfreie Einzelplan-Updates lohnt es sich, `CollectorCore` in einen instanzbasierten Service mit echter Jobverwaltung umzubauen.

Soll der Collector nach dem Schließen der AWB weiterlaufen, muss der Supervisor die Prozesskennung persistent speichern, Standardausgaben in Dateien umleiten und den noch laufenden Prozess beim nächsten AWB-Start wieder übernehmen. Ein bloß an die JavaFX-Lebensdauer gebundener Child-Process erfüllt die Always-on-Anforderung nicht.

### 5. Sicheres Deployment

Für Remote Collector fehlen insbesondere:

- Authentisierung und Autorisierung
- TLS
- Secret-Verwaltung
- Konfigurationsversionen
- Rollback
- Kompatibilitätsprüfung
- Audit-Informationen
- eindeutige Zuordnung zwischen Analyseprojekt und Collector-Plan

Secrets gehören weder in AWB-Projektdateien noch im Klartext in generierte XML-Dateien. Die Konfiguration sollte Secret-Referenzen enthalten, deren Werte lokal am jeweiligen Ausführungsort gespeichert werden.

Das ist auch für den lokalen Betrieb relevant. Vor einer breiten Auslieferung sind mindestens nötig:

- Secret-Werte aus den bestehenden H2-Zeilen in einen Betriebssystem-Keyring oder einen verschlüsselten lokalen Store verschieben,
- Collector-Bundles ohne Klartextpasswörter erzeugen; das vorhandene `${ENV:...}`-Verfahren kann lokal als Übergang dienen,
- TLS-Prüfung standardmäßig aktivieren und selbstsignierte Zertifikate nur nach explizitem Vertrauen zulassen,
- Secrets und Tokens aus Logs, Diagnoseexporten und Fehlermeldungen entfernen.

### 6. Einheitlicher Datenweg

CSV ist als Austauschformat nützlich, aber als regulärer Produktworkflow problematisch:

```text
Quelle → Collector → CSV-Verzeichnis → Importdialog → AWB-Projekt
```

Jeder Pfeil erzeugt Konfiguration, Dateiverwaltung, Fehlerzustände und Erklärungsbedarf.

Der bevorzugte fachliche Weg sollte sein:

```text
Quelle → Erfassungsruntime → lokaler Projektbestand → Analyse
```

CSV bleibt:

- für Offline-Transfer,
- für Air-Gap-Umgebungen,
- als Export- und Debugformat,
- als Kompatibilitätsweg für bestehende Installationen.

Technisch ist für den lokalen Collector zunächst ein unsichtbarer Datei-Spool die realistischste Brücke, weil das eingebettete Solr nicht von einem zweiten Prozess erreichbar ist:

```text
Quelle
  → verwalteter Collector-Prozess
  → interner, rotierender Spool
  → inkrementeller AWB-Ingest
  → eingebettetes Repository
```

Der vorhandene CSV-Writer und CSV-Parser können dafür wiederverwendet werden. Der heutige Importer liest jedoch ganze Dateien; er besitzt keinen stabilen Cursor für eine wachsende Datei. Benötigt werden deshalb Rotation in abgeschlossene Segmente, atomare Übergabe, Segment-Manifest mit stabiler ID und Prüfsumme sowie idempotentes Wiederanlaufen. Der Nutzer sieht weder Dateien noch einen Importdialog.

Eine H2-Markierung „Segment importiert“ allein reicht nicht für Exactly-once: Zwischen Repository-Commit und H2-Update kann der Prozess abstürzen. Realistisch ist At-least-once-Übertragung mit idempotenter Wirkung, beispielsweise durch deterministische Dokument-IDs aus Plan, Segment und Serien-Schlüssel oder durch eine repositoryseitige Deduplizierung. Dieser Mechanismus ist Teil des Collector-Roundtrip-Spikes.

Wenn die Sammlung auch bei geschlossener AWB weiterläuft, puffert der Collector im Spool. Beim nächsten Start übernimmt die AWB die abgeschlossenen Segmente. Für sehr große oder dauerhaft entfernte Installationen bleibt das direkte Schreiben in ein externes Solr-/Elasticsearch-Repository eine Alternative. Die Schema- und Versionskompatibilität zwischen aktuellem Collector-Writer und den aktuellen AWB-Repositories muss dafür zuerst durch einen Integrationstest belegt werden. Ein neues Netzwerk-Ingest-Protokoll ist erst dann sinnvoll, wenn Rückstau, Wiederholung und Versionierung sauber spezifiziert sind.

### 7. Laufender Betriebszustand in der AWB

Ein Projekt braucht eine Seite „Daten“, auf der der Nutzer sofort erkennt:

- Prometheus: verbunden, letzter Import vor 4 Minuten
- Linux-Server: 12 von 12 erreichbar
- Java-Prozesse: 11 aktiv, einer nicht gefunden
- Datenbestand: 1. März bis heute
- nächste Sammlung: in 56 Sekunden
- Fehler: Berechtigung für `iostat` auf `server-07` fehlt

Das ist kein Monitoring-Dashboard für die Zielsysteme, sondern die Funktionskontrolle der eigenen Datenerfassung.

## Zielbild

### AWB als Control Plane

Die AWB übernimmt:

- Analyseprojekt und UX
- Quellenkatalog
- Verbindungsprofile
- Auswahl von Systemen und Metriken
- Verwaltung der Sammelpläne
- lokale Datenhaltung
- Importfortschritt und Datenqualität
- Startpunkt der Analyse

### Collector als ausführbarer Agent

Der Collector übernimmt:

- Zugriff auf entfernte Systeme
- zeitgesteuerte Abfragen
- Normalisierung und Puffern
- zuverlässige Übertragung
- Ausführung nahe an geschützten Datenquellen
- Betrieb ohne laufende AWB

Der Collector ist damit kein zweites Produkt mehr, das der Benutzer separat verstehen muss, sondern eine Ausführungsruntime.

„Zuverlässige Übertragung“ ist dabei eine Zielanforderung, noch keine vorhandene Eigenschaft. Der heutige CSV-Weg kennt keine Ende-zu-Ende-Bestätigung durch die AWB. Für den Spool muss deshalb ein Segment erst dann gelöscht oder archiviert werden, wenn die AWB dessen Import samt Commit bestätigt hat.

### Quellenadapter als gemeinsames Plugin-Konzept

Ein Adapter sollte drei getrennte Fähigkeiten anbieten:

```text
SourceDescriptor
  └── erklärt Felder, Möglichkeiten und Voraussetzungen

SourceDiscovery
  └── testet Verbindung und entdeckt Systeme/Metriken

SourceReader
  └── liest historische oder aktuelle Messwerte
```

Zusätzlich erzeugt ein `CollectionPlanCompiler` aus der fachlichen Konfiguration die konkrete Collector-Konfiguration. So kann die bestehende XML-Konfiguration zunächst intern weiterverwendet werden, ohne sie zum UI- oder API-Vertrag zu machen.

Die drei Fähigkeiten reichen für die Planung noch nicht vollständig aus. Praktisch werden zusätzlich benötigt:

```text
SourceValidator
  └── prüft Verbindung, Berechtigungen und Voraussetzungen

SourcePreview
  └── führt eine begrenzte Probemessung aus

PlanCompiler
  └── übersetzt einen fachlichen Plan in einen AWB-Import oder ein Collector-Bundle
```

Die Implementierungen dürfen quellspezifisch bleiben. Einheitlich sein müssen Ergebnis-, Fehler- und Statusmodelle, damit der Wizard nicht für jede Quelle eine neue Bedienlogik benötigt.

### Control Plane und Data Plane getrennt halten

Die AWB verwaltet Pläne und Status; Messwerte sollten nicht durch dieselbe Management-API fließen. Dadurch bleiben zwei Protokolle mit unterschiedlichen Anforderungen getrennt:

```text
Control Plane: Plan, Revision, Start/Stop, Status, Diagnose
Data Plane:    Zeitreihen-Segmente, Commit, Cursor, Wiederholung
```

Diese Trennung erlaubt lokal einen Datei-Spool und später remote beispielsweise Objektspeicher, direkten Repository-Zugriff oder ein Upload-Protokoll, ohne das Wizard- und Planmodell zu ändern.

## Intuitiver Wizard

### Schritt 1: Was möchten Sie untersuchen?

Keine Frage nach „Classic“, „Cloud Native“ oder Repository.

Beispiele:

- Server und Betriebssysteme
- Java-Anwendungen
- Kubernetes/Prometheus
- Datenbank
- Logs
- vorhandene Dateien

Die Auswahl dient nur dazu, passende Quellen vorzuschlagen.

### Schritt 2: Quelle verbinden

Beispiel Prometheus:

- URL
- Authentisierung
- „Verbindung testen“

Danach zeigt die AWB:

- erreichbarer Endpunkt und erfolgreiche Authentisierung
- gefundene Metriken sowie aus Labels abgeleitete Jobs und Instanzen
- einen geschätzten Zeitraum anhand einer begrenzten Probe
- geschätzte Serienzahl

Metriknamen und Serienlabels sind mit dem vorhandenen Client erreichbar. Prometheus-Version und exakte Retention benötigen zusätzliche API-Aufrufe beziehungsweise sind nicht immer zuverlässig bestimmbar; sie sind für den ersten Wizard nicht erforderlich.

Beispiel SSH:

- ein Host, Liste oder Import aus Datei
- Benutzer und Authentisierung
- Test
- erkannte Betriebssysteme und verfügbare Werkzeuge

### Schritt 3: Was soll gesammelt werden?

Nicht zuerst rohe Metriknamen präsentieren, sondern Profile:

- Basis-Systemmetriken
- Java/JVM Basis
- Java Garbage Collection
- Netzwerk und Datenträger
- PostgreSQL Basis
- alles Gefundene
- eigene Auswahl

Ein Profil ist transparent: Der Nutzer kann aufklappen, was enthalten ist.

Wichtig ist eine Kostenanzeige:

- 12 Hosts
- 46 Metriken pro Host
- Intervall 30 Sekunden
- ungefähr 1.590.000 Werte pro Tag

### Schritt 4: Zeitraum und Rhythmus

- vorhandene Historie importieren: letzter Tag, Woche, Monat oder eigener Zeitraum
- ab jetzt regelmäßig sammeln
- beides

Sinnvolle Defaults hängen von der Quelle ab. Bei Prometheus wäre „letzte sieben Tage und danach alle fünf Minuten aktualisieren“ ein guter Einstieg. Für `top` per SSH könnte ein 30- oder 60-Sekunden-Intervall sinnvoller sein.

### Schritt 5: Wo soll die Sammlung laufen?

Standardmäßig keine technische Entscheidung verlangen:

- „Auf diesem Rechner“ ist vorausgewählt.
- Erkannte Remote Collector werden als Alternative angeboten.
- „Später einrichten“ bleibt möglich.

Die AWB prüft automatisch, ob der gewählte Ausführungsort die Quelle erreichen kann.

Diese Auswahl darf nur erscheinen, wenn tatsächlich mehr als ein kompatibler Ausführungsort registriert ist. Solange es noch keine Remote-Collector-Registrierung gibt, startet der lokale Flow ohne diesen Schritt. Ein deaktivierter Remote-Eintrag würde lediglich eine noch nicht vorhandene Funktion bewerben.

### Schritt 6: Probemessung

Die AWB führt eine Messung aus und zeigt:

- Beispielwerte
- Zeitstempel und Zeitzone
- entstehende Dimensionen wie Host, Prozess und Namespace
- Warnungen
- vorgeschlagene Seriennamen

Damit werden falsche Prozesszuordnungen und unbrauchbare Dimensionen erkannt, bevor tagelang Daten gesammelt wurden.

### Schritt 7: Fertig – direkt zur ersten Erkenntnis

Nach „Starten“ nicht in einem leeren Projekt enden, sondern:

- Basisdaten importieren,
- Fortschritt zeigen,
- eine erste Ansicht erzeugen,
- vergleichbare Serien gruppieren,
- einen kurzen Hinweis geben: „Vergleichen Sie CPU aller 12 Hosts“.

Time-to-first-chart ist die wichtigste Onboarding-Metrik.

## Beispiel: Prometheus

Prometheus ist der beste erste vertikale Schnitt, weil schon ein Importer existiert.

Der Nutzer gibt nur URL und Zugangsdaten ein. Die AWB entdeckt Jobs, Instanzen, Labels und Zeitbereich. Danach kann er beispielsweise „alle JVMs des Jobs `orderservice`“ auswählen.

Die AWB erzeugt:

- ein Verbindungsprofil,
- eine Label- und Dimensionszuordnung,
- einen historischen Importauftrag,
- einen Delta-Importplan,
- vorbereitete Vergleichsansichten.

Für den ersten Release darf die AWB den Delta-Import selbst ausführen, solange sie läuft. Für zuverlässigen Dauerbetrieb wird derselbe fachliche Plan später von einer headless Prometheus-Runtime ausgeführt. Das kann nach einer Code-Extraktion der Collector oder ein kleiner Import-Runner sein; der heutige Collector besitzt noch keine Prometheus-Source. Das UI bleibt identisch.

Vor einem automatischen Zeitplan muss der bestehende Import jedoch gehärtet werden:

- **Auswahl vor Vollscan:** Nicht automatisch alle Metriken und Serien laden. Zuerst Jobs, Präfixe oder ausgewählte Metriken eingrenzen und die erwartete Serienzahl anzeigen.
- **Abfragen stückeln:** Lange Zeiträume und große Ergebnismengen in begrenzten Zeitfenstern laden und schrittweise committen. Der aktuelle Import sammelt zunächst alle Series-Definitionen im Speicher.
- **Stabiler Serien-Schlüssel:** Die `measurement`-Dimension darf bei jedem Delta-Lauf nicht allein wegen des neuen Importzeitraums wechseln. Sonst entstehen fachlich verschiedene Serienblöcke für dieselbe Prometheus-Serie. Importlauf und fachliche Measurement sind getrennte Konzepte.
- **Cursor je Serie oder sichere Überlappung:** Der aktuelle Delta-Start verwendet den global jüngsten Zeitstempel des Projekts. Dadurch können neu hinzugekommene oder zeitweise ausgefallene Serien Daten verlieren. Ein persistierter Cursor je Remote-Series-Key ist korrekt; als Übergang ist ein überlappendes Zeitfenster mit idempotenter Deduplizierung möglich.
- **Teilfehler sichtbar machen:** Eine fehlgeschlagene Werteabfrage darf nicht nur als leere Serie weiterlaufen. Der Lauf braucht Zustände wie `SUCCESS`, `PARTIAL` und `FAILED` sowie die betroffenen Serien.
- **TLS sicher konfigurieren:** Zertifikats- und Hostname-Prüfung müssen standardmäßig aktiv sein.

Prometheus gehört in Phase 1 weiterhin direkt in die AWB. Eine neue Prometheus-Quelle im Collector wäre zunächst doppelte Implementierung ohne Nutzen. Erst wenn Importe unabhängig von einer laufenden AWB werden müssen, sollte der vorhandene Prometheus-Code in eine gemeinsam nutzbare Bibliothek oder eine eigenständige Runtime extrahiert werden.

## Beispiel: Linux und Java per SSH/Jolokia

Der Nutzer trägt Hosts ein oder importiert eine Liste. Die AWB prüft SSH, erkennt Linux und findet optional Java-Prozesse beziehungsweise Jolokia-Endpunkte.

Sie schlägt vor:

- CPU, Speicher, Load, Dateisystem
- Prozess-CPU und RSS
- JVM Heap, GC und Threads
- Intervall 30 Sekunden
- lokaler Collector

Im Hintergrund werden Host-, Prozess- und Metrikgruppen erzeugt. Das bestehende Kreuzproduktmodell des Collectors kann weiterarbeiten. Im Wizard sollte dieses Kreuzprodukt aber vor dem Start als konkrete Aussage sichtbar sein:

> 12 Hosts × 2 Prozesse × 3 Metrikprofile ergeben 72 Sammeljobs.

Damit bleibt die Mächtigkeit erhalten, ohne XML-Kenntnisse vorauszusetzen.

## Produktentscheidungen

### „Projekt“ und „Collector-Projekt“ trennen

Ein Analyseprojekt sollte mehrere Quellen enthalten können. Der Begriff „Collector-Projekt“ sollte aus der normalen Oberfläche verschwinden.

Ein Projekt könnte gleichzeitig enthalten:

- Prometheus-Historie,
- per SSH gesammelte Systemmetriken,
- JMX-Werte,
- Release-Ereignisse,
- Logs.

Genau diese Kombination wird später für die Ursachenanalyse benötigt.

Die Umstellung ist mit dem heutigen Modell nicht nur eine Umbenennung. `importerId`, `isSplitSourceProject` und die einzelne `ImporterSourceRepository` koppeln ein Projekt an einen dominanten Importer und Datenweg. Kurzfristig bleiben diese Felder zur Kompatibilität erhalten; neue Projekte verwenden parallel die neuen Quellen- und Plantabellen. Erst nach einer Migration sollten die Legacy-Felder entfallen.

Für neue Mehrquellen-Projekte sollten im MVP alle Werte in das EKG-Repository importiert werden. Der vorhandene Split-Source-Modus bleibt als kompatibler Expertenmodus für reine Prometheus-Projekte erhalten. Eine Mischung würde sonst zusätzlich ein Fetcher-Routing je Serie und eine neue Fehlersemantik zur Anzeigezeit erfordern; das ist für den Kernworkflow nicht nötig.

### Push oder Pull beim Remote Collector

Das direkte Pushen von Konfigurationen von der AWB zum Collector ist einfach, funktioniert aber in restriktiven Netzen oft nicht.

Deshalb sollte das Protokoll beide Betriebsarten ermöglichen:

- AWB → Collector für erreichbare interne Collector.
- Collector → Control Endpoint beziehungsweise Plan-Polling für abgeschottete Zonen.

Für eine erste Version genügt Push über HTTPS. Die Planidentität sollte aber so modelliert sein, dass später ein Pull-Agent möglich bleibt.

### Normalisiertes internes Datenformat

Alle Quellen sollten in einen gemeinsamen Messwert-Envelope übersetzen:

```text
timestamp
metric
value
unit
dimensions
sourceId
resourceId
quality/status
```

„Host“, „Prozess“, „Pod“, „Service“ und „Namespace“ sind Dimensionen, keine separaten Importwelten. Quellenspezifische Rohdaten dürfen zusätzlich erhalten bleiben.

Dieser Envelope ist zunächst ein Transport- und Adaptermodell, nicht automatisch das neue persistierte AWB-Schema. Für den MVP werden seine Felder auf die vorhandenen EKG-Dimensionen abgebildet; `sourceId`, Cursor, Einheit und Qualitätsstatus bleiben als Metadaten des Sammelplans beziehungsweise Laufs in H2. Erst ein nachgewiesener Analysebedarf rechtfertigt neue Felder in sämtlichen Repository-Backends.

## Eine radikalere Alternative

Man könnte den klassischen Collector langfristig vollständig durch einen kleinen „EKG Agent“ ersetzen:

- ein einzelner Prozess,
- quellenspezifische Plugins,
- lokale Queue,
- gRPC/HTTP-Verbindung zur AWB oder zum lokalen Speicher,
- deklarative Sammelpläne,
- keine XML-Dateien,
- keine CSV-Zwischenstufe.

Das wäre konzeptionell sauberer, aber ein teurer Rewrite. Viele schwierige Bestandteile – Windows Counter, SSH, JMX, Wildcards, Prozesszuordnung und Repository-Ausgaben – existieren bereits.

Die Empfehlung lautet deshalb:

> Produktmodell und API neu entwerfen, vorhandene Collector-Implementierungen zunächst als Ausführungsengine weiterverwenden.

Die bestehende XML-Datei wird vom neuen Plan-Compiler erzeugt. Später können einzelne Teile des Collectors ersetzt werden, ohne dass sich der Nutzerworkflow erneut ändert.

## Umsetzung in Phasen

### Phase 0: Zwei technische Spikes

Vor dem Produktumbau sollten zwei kleine, zeitlich begrenzte Durchstiche die größten Unsicherheiten klären.

**Spike A – Prometheus-Importintegrität**

- denselben Bestand mehrfach per Delta importieren,
- neue und zeitweise inaktive Serien simulieren,
- prüfen, ob `measurement`, Dokumentzusammenführung und globaler Delta-Cursor Lücken oder Dubletten erzeugen,
- Speicher und Laufzeit für einen repräsentativ großen Prometheus-Bestand messen,
- daraus Serien-Schlüssel, Cursorstrategie, Chunk-Größe und Importlimits festlegen.

**Spike B – Collector-Roundtrip**

- aus einem minimalen fachlichen Unix-Plan eine gültige `config.yaml` erzeugen,
- Collector als Kindprozess starten und stoppen,
- rotierendes Collector-CSV als internen Spool behandeln,
- genau ein abgeschlossenes Segment automatisch mit At-least-once-Übertragung und idempotenter Wirkung in das eingebettete Repository importieren,
- einen Fehler bis zur Datenquellen-Seite der AWB propagieren.

Erst wenn beide Spikes funktionieren, ist die vorgeschlagene evolutionäre Architektur belegt. Scheitert insbesondere der Spool-Roundtrip an Format oder Idempotenz, sollte vor weiterer UI-Arbeit ein kleines versioniertes Segmentformat eingeführt werden.

### Phase 1: Durchgängiger Prometheus-Weg

- neue Projektanlage vereinfachen
- Seite „Datenquellen“ einführen
- zusätzliche H2-Tabellen für Quelle, Plan, Lauf und Cursor einführen
- Prometheus-Importkern vom JavaFX-Dialog entkoppeln und über einen headless Importservice aufrufen
- bestehende Prometheus-Quelle über einen Adapter weiterverwenden
- Verbindungstest sowie begrenzte Metrik- und Label-Discovery anbieten
- Metrikauswahl, Serienabschätzung und Schutzlimits vor dem Import einführen
- historischen und manuellen Delta-Import als einen Datenplan darstellen
- Delta-Cursor, stabile Serienidentität, Chunking und Teilfehler korrigieren
- optionalen Zeitplan ausführen, solange die AWB läuft
- Laufstatus und Diagnose im Projekt anzeigen
- erste Vergleichsansicht automatisch erzeugen

Erfolgskriterium: Ein neuer Nutzer erreicht ohne Dokumentation in unter sieben Minuten einen Chart mit ausgewählten eigenen Daten. Ein wiederholter Delta-Import erzeugt weder Lücken noch unkontrollierte Dubletten.

### Phase 2: Lokaler Collector

- Collector gebündelt mit der AWB ausliefern
- Collector als separaten Kindprozess durch einen Supervisor steuern
- fachlichen Unix/SSH-Plan nach bestehendem XML kompilieren
- Runtime-Bundles validieren, versionieren und bei Änderung kontrolliert neu starten
- rotierenden, bestätigten Spool und inkrementellen Import implementieren
- Zugangsdaten über Secret-Referenzen bereitstellen
- Prozess-, Job-, Spool- und Importfehler in der AWB anzeigen
- SSH/Unix als erste Collector-Quelle integrieren

Erfolgskriterium: Linux-Host verbinden und regelmäßige Sammlung starten, ohne eine Datei manuell anzulegen. Nach Schließen und erneutem Öffnen der AWB werden währenddessen gesammelte Daten ohne fachliche Dubletten übernommen.

### Phase 3: Mehrere Quellen, Java und Windows

- mehrere Quellen und Pläne in einem Analyseprojekt vollständig freigeben
- JMX/Jolokia Discovery
- JVM-Metrikprofile
- Timeouts und Abbruch für blockierende Collector-Quellen
- Windows Counter Discovery nur mit registrierter Windows-Runtime
- Prozessidentität und vergleichbare Dimensionsnamen verbessern
- Probemessung und Datenqualitätsprüfungen

### Phase 4: Remote Collector

- Collector-Supervisor als eigenständigen Remote-Dienst betreiben
- sichere Registrierung
- Fähigkeiten- und Versionsabfrage
- Secret Store
- Konfigurationsrevisionen und Rollback
- Statusübertragung
- Netzwerk- und Kompatibilitätsdiagnose
- Push zunächst nur für erreichbare Collector unterstützen
- Pull/Plan-Polling erst bei einem konkreten Netzwerkbedarf ergänzen

### Phase 5: Ökosystem

- weitere Quellen als Plugins
- Profile exportieren und teilen
- Organisationsvorlagen
- Air-Gap-Pakete
- Kubernetes-Discovery
- Agentenunterstützung beim Vorschlagen sinnvoller Metriken

PostgreSQL gehört erst in diese Phase, sofern der Use Case priorisiert wird, weil die aktuelle Source noch nicht implementiert ist.

## Architekturentscheidungen vor Implementierungsbeginn

Folgende Entscheidungen sollten nach den Spikes ausdrücklich festgehalten werden:

1. **Always-on-Anforderung:** Muss lokale Sammlung bei geschlossener AWB funktionieren? Die Empfehlung lautet Ja; damit ist ein persistenter Spool Pflicht.
2. **Spool-Aufbewahrung:** Größenlimit, Verhalten bei vollem Datenträger, Löschung nach Bestätigung und manuelle Wiederholung.
3. **Serienidentität:** Welche Dimensionen bilden den stabilen Schlüssel, und wie werden Label-Änderungen behandelt?
4. **Delta-Semantik:** Cursor je Serie oder überlappende Fenster mit Deduplizierung.
5. **Secret Store:** Betriebssystem-Keyring versus verschlüsselter portabler Store und Verhalten beim Projekttransfer.
6. **Projekt-Flavor:** automatisch aus Quellen ableiten oder als fachliche Darstellung explizit beibehalten.
7. **Externe Repositories:** Bleiben direkt beschreibbare Solr-/Elasticsearch-Ziele ein Expertenmodus oder werden sie Teil des Standardflows?

Nicht vorab entschieden werden müssen gRPC versus REST, Push versus Pull für alle Netze oder ein neues universelles Binärformat. Diese Entscheidungen werden erst relevant, wenn der lokale vertikale Schnitt belastbar funktioniert.

## Was ausdrücklich nicht zuerst gebaut werden sollte

- Ein grafischer Editor für `config.yaml`.
- Eine vollständige Neuimplementierung des Collectors.
- Ein universeller Workflow-Designer.
- Eine neue Live-Monitoring-Oberfläche.
- Remote Deployment, bevor der lokale End-to-End-Weg funktioniert.
- Dutzende Quellen, bevor Prometheus und SSH wirklich einfach sind.
- Eine automatische Auswahl „aller Metriken“ ohne Mengen- und Kostenkontrolle.
- Eine Hot-Reload-API im statischen Collector-Kern, bevor der Prozess-Supervisor erprobt ist.
- Neue persistierte Zeitreihenfelder, solange Plan- und Laufmetadaten in H2 ausreichen.

## Messbare Produktziele

Die neue Lösung sollte anhand konkreter Werte beurteilt werden:

- Zeit vom ersten Start bis zum ersten eigenen Chart
- Anzahl manueller Eingaben
- Anteil erfolgreicher Verbindungsversuche
- Anteil der Nutzer, die ohne Dokumentation Daten importieren
- verständlich gelöste Fehlersituationen
- Zeit zum Hinzufügen eines weiteren Hosts
- Zahl unbemerkter Sammelausfälle
- Anteil der Projekte mit mehr als einer kombinierten Quelle

Ein gutes Ziel für den ersten vertikalen Schnitt wäre:

- maximal sieben Minuten bis zum ersten Prometheus-Chart,
- maximal zehn Minuten bis zur ersten laufenden SSH-Sammlung,
- keine manuell bearbeitete Konfigurationsdatei,
- Fehler bis auf Host-, Quelle- und Berechtigungsebene verständlich sichtbar.

Zusätzlich braucht die technische Abnahme harte Zuverlässigkeitskriterien:

- Neustart von AWB oder Collector verliert kein bestätigtes Spool-Segment,
- ein Segment wird nach Wiederanlauf höchstens einmal fachlich wirksam,
- ein Delta-Import überspringt keine langsamere oder neu hinzugekommene Serie,
- Abbruch eines Imports hinterlässt einen erkennbaren Laufstatus,
- ein ungültiger neuer Plan lässt die letzte funktionierende Collector-Revision weiterlaufen,
- Passwörter und Tokens erscheinen weder in generierten Bundles noch in Logs.

## Prüfgrundlage im aktuellen Code

Die wesentlichen Machbarkeitsaussagen stützen sich auf folgende Stellen:

- Projektmodell und heutige Legacy-Kopplung: [`Project.java`](../awb/awb-project/awb-project-api/src/main/java/io/softwareecg/awb/project/api/model/Project.java)
- Speicherung genau einer Importquelle je Projektname und Klartextfelder für Credentials: [`H2RepositoryConfigRepository.java`](../awb/awb-da/awb-da-h2/src/main/java/io/softwareecg/awb/da/h2/H2RepositoryConfigRepository.java)
- Prometheus-Voll- und Delta-Import: [`PrometheusLocalImportStrategy.java`](../awb/awb-importer/awb-importer-prometheus/src/main/java/io/softwareecg/awb/importer/prometheus/bl/PrometheusLocalImportStrategy.java)
- Prometheus-API und derzeit permissive TLS-Konfiguration: [`PrometheusClient.java`](../awb/awb-importer/awb-importer-prometheus/src/main/java/io/softwareecg/awb/importer/prometheus/client/PrometheusClient.java)
- Eingebettetes Solr innerhalb der AWB-JVM: [`EmbeddedRepositoryClientProvider.java`](../awb/awb-da/awb-da-solr/src/main/java/io/softwareecg/awb/da/solr/embedded/EmbeddedRepositoryClientProvider.java)
- Einmaliger XML-basierter Collector-Start und statischer Lifecycle: [`CollectorCore.java`](../collector/collector/collector-control/src/main/java/io/softwareecg/collector/control/CollectorCore.java)
- Periodische Collector-Schleife und Repository-Ausgabe: [`CollectorJob.java`](../collector/collector/collector-control/src/main/java/io/softwareecg/collector/control/jobs/CollectorJob.java)
- Schmale bestehende Source-Schnittstelle: [`CounterSource.java`](../collector/collector/collector-source/collector-source-common/src/main/java/io/softwareecg/collector/source/CounterSource.java)
- Collector-CSV-Import ohne persistierten Inkremental-Cursor: [`CollectorLogImportStrategy.java`](../awb/awb-importer/awb-importer-csv/src/main/java/io/softwareecg/awb/importer/csv/bl/CollectorLogImportStrategy.java)
- Leeres PostgreSQL-Source-Modul: [`collector-source-postgres/pom.xml`](../collector/collector/collector-source/collector-source-postgres/pom.xml)

Die Bewertung ist eine statische Codeprüfung. Laufzeitverhalten, Datenintegrität und Skalierung sind durch die beiden vorgeschlagenen Spikes und spätere Integrationstests zu belegen.

## Empfehlung

Der zentrale neue Baustein sollte ein „Data Source & Collection“-Subsystem in der AWB sein. Es verbindet Projekt, Importer, Collector und lokale Ablage über ein gemeinsames fachliches Planmodell.

Der wichtigste UX-Wechsel lautet:

> Nicht mehr „Erzeuge ein Collector-Projekt und importiere dessen Ausgaben“, sondern „Füge deiner Analyse eine Datenquelle hinzu“.

Damit bleibt die technische Unabhängigkeit von AWB und Collector erhalten. Gleichzeitig entsteht erstmals ein geschlossenes Produkt: Quelle verbinden, Daten gewinnen, vergleichen und analysieren – ohne dass der Nutzer vorher die interne Architektur des Software-EKG verstehen muss.

Die Vorschläge sind mit der vorhandenen Codebasis grundsätzlich umsetzbar. Die realistische evolutionäre Route lautet:

1. Prometheus-Import stabilisieren und in einen Quellen-Workflow einbetten.
2. Das Mehrquellen- und Planmodell additiv in H2 einführen.
3. Den bestehenden Collector unverändert genug als verwalteten Prozess nutzen.
4. Seine CSV-Ausgabe zunächst zu einem unsichtbaren, zuverlässigen Spool machen.
5. Erst nach diesem End-to-End-Nachweis Remote-Steuerung und ein mögliches neues Data-Plane-Protokoll bauen.

So werden vorhandene Parser, Quellen und Importservices genutzt, ohne ihre heutigen Grenzen zur langfristigen öffentlichen Architektur zu erklären.
