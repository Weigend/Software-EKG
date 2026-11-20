# Migrationsplan: GraalVM Native-Image (Release B)

> Hinweis: Reine Planung. Keine Code-Änderungen. Reviewbasis vor Beginn der Migration.
>
> **Vorbedingung**: [migration-plan-avaje.md](migration-plan-avaje.md) (Release A) ist abgeschlossen und produktiv. Solange Weld/CDI im Stack ist, ist Native-Image nicht durchführbar.
>
> Verwandte Dokumente:
> - [architecture-cleanup-plan.md](architecture-cleanup-plan.md)
> - [migration-plan-avaje.md](migration-plan-avaje.md)

---

## 1. Executive Summary

Ziel: native Binaries via GraalVM Native-Image für **Collector** (CLI, hohe Erfolgswahrscheinlichkeit) und optional **AWB** (JavaFX-Desktop, niedrigere Erfolgswahrscheinlichkeit). Resultat:

- Sub-100 ms Cold-Start, kein JVM-Overhead, deutlich reduziertes Distribution-Bundle.
- Single-Binary-Deployment, keine JRE-Voraussetzung beim Anwender.

Zwei harte Risiken bestimmen den Plan:

1. **Apache Solr 9.x** ist nicht native-image-fähig (Lucene-SPI, Codec-Discovery, embedded Jetty/Jersey). Native-Builds laufen daher **ohne Solr**.
2. **JavaFX + Native-Image** läuft nicht über das Standard-`native-image`-Tool, sondern nur über **GluonHQ Substrate** (separater Werkzeugkasten). Erfolg ist projektspezifisch und erst nach einem belastbaren Spike beurteilbar.

Aufwand realistisch: **5–8 Wochen Mannarbeit**, davon ein Großteil im Risiko-Bereich. Plan-B (siehe Abschnitt 7): bei jlink-Image bleiben, nur Collector als Native-Binary ausliefern.

---

## 2. Warum Release A Vorbedingung ist

CDI-Container (Weld) sind mit Native-Image strukturell unverträglich:

- **Klassenpfad-Scan zur Laufzeit** — Native-Image arbeitet im Closed-World-Modell: zur Build-Zeit wird der Klassengraph fixiert. Weld scannt zur Laufzeit `META-INF/beans.xml`-Archive, das gibt es im Native-Image nicht.
- **Dynamische Proxy-Generierung** für `@ApplicationScoped`-Beans via ByteBuddy/Javassist. Native-Image kann keine neuen Klassen zur Laufzeit erzeugen.
- **Reflection-Tiefe** — Weld reflektiert auf jede Bean-Klasse, jede `@Inject`-Stelle, jede Producer-Methode. Müsste komplett in `reachability-metadata.json` gespiegelt werden — praktisch nicht handhabbar.

Avaje Inject hingegen generiert zur Compile-Zeit Plain-Java-Code (`...$DI.java`), der genau die Konstruktor-Aufrufe und Field-Sets enthält. Zur Laufzeit keine Reflection, kein Klassenpfad-Scan. Damit ist der DI-Stack ab Release A nativ-tauglich.

---

## 3. Showstopper-Analyse pro Drittabhängigkeit

| Lib | Native-Image-Status | Strategie |
|---|---|---|
| **Apache Solr 9.x (embedded)** | nicht machbar — Lucene Codec-SPI, Jetty, Jersey | Native-Profil ohne Solr-DA-Modul |
| **Solr (remote SolrJ-Client only)** | machbar mit Reachability-Hints | optional einbeziehen |
| **JavaFX 21** | nicht via `native-image` direkt; nur via GluonHQ Substrate | siehe Phase 4 |
| **Apache Lucene** | partiell, Codec-SPI problematisch | nur transitiv via Solr — entfällt mit Solr |
| **ClickHouse JDBC (neuer Driver)** | machbar; Reachability-Metadata-Repo deckt es ab | OOTB |
| **H2** | machbar mit Tuning | Hints im Reachability-Repo |
| **Elasticsearch-Java-Client** | offizielle Reachability-Hints vorhanden | OOTB |
| **Jackson (databind)** | gut etabliert, Hints im Reachability-Repo | DTO-Klassen mit `@RegisterForReflection` annotieren |
| **Logback + SLF4J** | etabliert, Reachability-Hints da | `logback.xml` als Resource registrieren |
| **Apache POI** | etabliert mit Hints; Office-Templates müssen als Resources registriert werden | OOTB + Resource-Config |
| **Apache Commons (lang3, io, collections4, validator)** | OOTB native-tauglich | – |
| **HikariCP** | machbar, Reflection auf Connection-Properties | Hints |
| **Apache HttpClient 5** | etabliert | OOTB |
| **controlsfx, jfxtras** | nur über Gluon Substrate (siehe JavaFX) | wenn AWB-Native, dann hier |
| **WFX (eigene Lib)** | unbekannt — eigene Reflection in Lookup/EventBus | im Zuge der Migration eigene Hints in WFX einbauen |
| **Avaje Inject** | OOTB native-tauglich (Compile-time-DI) | – |

**Kernerkenntnis**: Für den **Collector** ist der Pfad sauber. Die Risiko-Komponenten (Solr-Embedded, JavaFX) gehören nur zum **AWB**.

---

## 4. Build-Strategie und Profile

```
Maven-Profile:
  default            → klassischer JVM-Build (jlink-Image), enthält Solr
  native-collector   → native-image-Build des Collectors, ohne Solr
  native-awb         → Gluon-Substrate-Build des AWB, ohne Solr (conditional)
```

**Reactor-Anpassung**: Modul `awb-da-solr` wird im `native-collector`/`native-awb`-Profil aus dem aktiven Reactor-Set ausgeschlossen. Der Repository-Discovery-Code (`ServiceLoader.load(EkgRepository.class)`) verträgt das ohne Code-Änderung — fehlt das Solr-Modul, fehlt eben dieser Provider, der Rest läuft.

**Konsumenten-Effekt**: AWB-UI muss erkennen können, ob "Solr" als Repository-Typ verfügbar ist. Pattern:
- `RepositoryTypeRegistry` befragt `ServiceLoader.load(RepositoryTypeProvider.class)`.
- Solr-Provider ist nur im JVM-Build vorhanden.
- UI rendert die Liste der verfügbaren Typen dynamisch.

Das ist im Cleanup-Plan ([architecture-cleanup-plan.md](architecture-cleanup-plan.md)) ohnehin als sauberes Plugin-Pattern angelegt.

---

## 5. Migrationsplan

### Phase 0 — Spike & Go/No-Go (1–2 Wochen)

Ziel: vor Code-Migration die zwei harten Risiken belastbar bewerten.

#### S0.1 — Collector-Native-Spike (1 Woche)
Throw-away-Branch. Collector-Build mit `native-maven-plugin`, ohne Solr, mit H2 und ClickHouse als Repository-Targets. Erkenntnisziele:
- Baut das Binary durch?
- Welche Reachability-Hints fehlen, die nicht im offiziellen Repo abgedeckt sind?
- Wie groß ist das Binary, wie schnell der Start?
- Funktioniert ein End-to-End-Run (Source → Repository-Schreibe → Watch)?

**Output**: Go für S5/S6 (Collector-Native als reguläres Produkt) oder Liste der ungelösten Reachability-Probleme.

#### S0.2 — Gluon-Substrate-Spike (1 Woche)
Throw-away-Branch. Minimales JavaFX-Hello-FXML mit Gluon Substrate native bauen. Erkenntnisziele:
- Baut das Binary durch?
- Funktionieren FXML-Loading, CSS, Bilder?
- Verträgt Avaje Inject das Build-Setup?
- Plattform-Spezifika (Linux/macOS/Windows) — wie schwer ist Cross-Compile?

**Output**: Go/No-Go für Phase 4 (AWB-Native). Bei No-Go bleibt AWB auf jlink, nur Collector wird native.

#### S0.3 — Reachability-Surface-Inventar (parallel, 0,5 Woche)
- Native-Image-Tracing-Agent gegen einen typischen AWB-Lauf laufen lassen.
- Generierte JSON-Dateien sammeln und nach Library-Zugehörigkeit klassifizieren.
- Vergleich gegen das offizielle Reachability-Metadata-Repository — welche Hints sind schon abgedeckt, welche müssen wir selbst pflegen?

**Output**: Liste der Drittlibs, für die wir eigene Hints brauchen. Schätzung der Restarbeit für Phase 2.

---

### Phase 1 — Build-Infrastruktur (1 Woche)

#### S1.1 — `native-maven-plugin` einbauen
- `org.graalvm.buildtools:native-maven-plugin` ins Reactor-`<pluginManagement>`.
- Profile `native-collector`, `native-awb` anlegen.
- Reachability-Metadata-Repository-Lookup aktivieren (das Plugin macht das automatisch).

**Aufwand**: 2 Tage.

#### S1.2 — `awb-da-solr`-Profil-Kondition
- `awb-da-solr` aus den Native-Profilen ausschließen.
- AWB- und Collector-Bootstrap so umstellen, dass das Solr-Modul optional ist (Plugin-Discovery via ServiceLoader, kein harter Compile-Link).

**Aufwand**: 2 Tage.

#### S1.3 — Build-Server-Vorbereitung
- GraalVM 21 oder 24 (LTS-Strategie diskutieren) als Build-Toolchain einrichten.
- CI-Job für Native-Build auf Linux einbauen.
- macOS- und Windows-Native-Builds: erst nach S0.1 entscheiden — Cross-Compile mit GraalVM ist nicht trivial.

**Aufwand**: 1 Tag.

---

### Phase 2 — Reachability-Metadata für eigene Module (1–2 Wochen)

#### S2.1 — FXML-Controller-Reflection
~50 FXML-Controller-Klassen mit `@RegisterForReflection` annotieren (oder zentral via `reflect-config.json` registrieren). Alternative: einen `@RegisterForReflection`-Marker an einem Stamm-Controller vererben.

**Aufwand**: 2 Tage.

#### S2.2 — Resource-Konfiguration
Alle FXML-Files, CSS-Files, Bilder (Icons, Splashscreens), Bundles in `resource-config.json` listen. Glob-Pattern statt Einzeldateien. Wird typischerweise pro Modul angelegt.

**Aufwand**: 2 Tage.

#### S2.3 — Jackson-DTO-Reflection
DTOs in den Importern und `awb-aichat` mit `@RegisterForReflection` markieren. Optional Wechsel auf `jackson-jr` (kein Reflection nötig), falls die DTO-Surface klein genug ist.

**Aufwand**: 1 Tag.

#### S2.4 — WFX-eigene Reflection-Hints
WFX' Lookup und EventBus haben ggf. eigene Reflection-Pfade (z.B. Annotation-basierte Subscription). Hints gehören ins WFX-Modul selbst, nicht ins AWB. Während Release A wurde das schon vereinfacht — hier nur noch das, was übrig bleibt.

**Aufwand**: 2 Tage.

#### S2.5 — Drittlibs ohne offizielle Hints
Restliste aus S0.3 abarbeiten. Pro Lib: tracing-agent-Output sichten, in `reachability-metadata.json` ins Projekt einchecken, idealerweise mit Hinweis im Modul-README.

**Aufwand**: 3–5 Tage (stark abhängig von S0.3).

---

### Phase 3 — Collector-Native-Build (1 Woche)

#### S3.1 — Native-Build durchziehen
- Profil `native-collector` aktivieren.
- Build durch alle Source-Adapter und Repository-Adapter (außer Solr) durchprüfen.
- Smoke-Tests für die Standard-Sources (Linux Procfs, JMX, HTTP-Probes — was auch immer aktiv ist).

**Aufwand**: 3 Tage.

#### S3.2 — Distribution-Assembly
- Native-Binary verpacken (Linux: tar.gz, macOS: zip, Windows: zip).
- `start-collector.sh`/`.bat`-Skripte ggf. obsolet — Binary direkt aufrufbar.
- Versions-Stamping ins Binary einbauen.

**Aufwand**: 2 Tage.

#### S3.3 — Akzeptanz-Tests
- Cold-Start-Zeit messen (Erwartung: < 100 ms).
- Speicherverbrauch messen.
- Mehrtägige Soak-Tests gegen reale Quellen.

**Aufwand**: 2 Tage.

---

### Phase 4 — AWB-Native-Build via Gluon Substrate (2–3 Wochen, conditional)

**Nur wenn S0.2 ein Go ergab.** Sonst: Phase 4 entfällt, AWB bleibt auf jlink.

#### S4.1 — Substrate-Build-Infrastruktur
- `gluonhq/substrate`-Plugin einbinden (kein normales Maven-Plugin, eigene Build-Pipeline).
- Profile `native-awb-{linux,macos,windows}`.
- Gluon-spezifische Konfiguration: `META-INF/substrate/config/`.

**Aufwand**: 3 Tage.

#### S4.2 — JavaFX-Native-Spezifika
- JavaFX-Module-Konfiguration für Substrate.
- `controlsfx`/`jfxtras`-Hints — Substrate hat eine eigene Liste, ergänzen falls nötig.
- Plattform-spezifische Native-Libs (libjfxgl, libglass, …) korrekt bündeln.

**Aufwand**: 5 Tage.

#### S4.3 — End-to-End-Test
- AWB komplett durchlaufen: Login → Repository-Verbindung (H2) → Import → Charts.
- Performance-Vergleich gegen jlink-Build.
- Memory-Vergleich.

**Aufwand**: 3 Tage.

#### S4.4 — Distribution-Assembly für drei Plattformen
Pro Plattform getrennter Build, weil Substrate keine echten Cross-Compiles erlaubt. CI braucht entsprechende Runner (Linux + macOS + Windows).

**Aufwand**: 3 Tage (wenn Build-Runner schon stehen).

---

### Phase 5 — Stabilisierung & Doku (1 Woche)

- Native-Build-Anleitung in README.
- Troubleshooting-Doku für typische Reachability-Fehler.
- Release-Checkliste pro Plattform.
- Größen-/Performance-Vergleichstabelle JVM vs. Native dokumentieren.

---

## 6. Risiken und Mitigation

| Risiko | Wahrscheinlichkeit | Impact | Mitigation |
|---|---|---|---|
| Solr-Embedded ist im Stack zwingend benötigt | gering (parallele Adapter da) | hoch | UI auf optionale Solr-Verfügbarkeit umbauen (S1.2) |
| Gluon Substrate scheitert an FXML/Plug-ins | mittel | hoch | S0.2 Spike, Plan-B: AWB bleibt jlink |
| Reachability-Metadata explodiert | mittel | mittel | Tracing-Agent + iterative Pflege; Reachability-Repo ist gut |
| WFX-eigene Reflection nicht erfasst | mittel | mittel | Eigene Hints in WFX einchecken; EventBus-API minimieren |
| ClickHouse-/H2-/ES-Treiber haben Edge-Cases | gering | mittel | Reachability-Repo deckt sie ab; Smoke-Tests |
| Native-Build-Zeit zu lang für CI | hoch | gering | nur on-demand bauen, nicht bei jedem PR |
| GraalVM-Version-Drift (Avaje, Native-Plugin) | gering | gering | LTS-GraalVM pinnen |
| Plattform-spezifische Bugs (macOS notarization, Windows-AV) | mittel | mittel | erst nach Stabilisierung adressieren |

---

## 7. Plan-B

Falls Phase 0 (insbesondere S0.2 Gluon-Spike) ergibt, dass AWB-Native nicht praktikabel ist:

1. **Collector-Native ausliefern** (Phase 1–3 abschließen). Liefert spürbaren Kundennutzen (CLI-Tool, kleines Binary, schneller Start).
2. **AWB bleibt auf jlink** (klassisches Distribution-Bundle, JRE eingebunden).
3. **Release B endet damit erfolgreich** — kein Misserfolg, sondern eingegrenztes Ergebnis.

Dies ist der wahrscheinlichste Realausgang. Der Plan ist so geschnitten, dass Plan-B nicht als Niederlage wirkt — der Collector-Native-Build alleine rechtfertigt 3–4 Wochen Aufwand.

---

## 8. Aufwand-Übersicht

| Phase | Aufwand | Risiko |
|---|---|---|
| 0 — Spike | 1–2 Wochen | hoch (Go/No-Go) |
| 1 — Build-Infrastruktur | 1 Woche | gering |
| 2 — Reachability-Metadata | 1–2 Wochen | mittel |
| 3 — Collector-Native | 1 Woche | gering |
| 4 — AWB-Native (conditional) | 2–3 Wochen | hoch |
| 5 — Stabilisierung | 1 Woche | gering |
| **Summe ohne Phase 4** | **5–7 Wochen** | |
| **Summe mit Phase 4** | **7–10 Wochen** | |

---

## 9. Akzeptanzkriterien

### Minimal (Plan-B-Ergebnis)

1. `mvn -P native-collector clean package` baut auf Linux ein Native-Binary des Collectors.
2. Das Binary startet in unter 100 ms.
3. End-to-End-Test gegen H2 und ClickHouse läuft grün.
4. Distribution enthält das Binary plus Standard-Konfigurationen.

### Vollausbau

5. `mvn -P native-awb-linux clean package` baut ein Native-Binary des AWB auf Linux (analog macOS/Windows).
6. AWB startet, lädt mindestens H2 und ClickHouse als Repositories, führt einen CSV-Import durch, rendert Charts.
7. Performance-Vergleichstabelle (Cold-Start, Heap, Bundle-Größe) JVM vs. Native dokumentiert.

---

## 10. Offene Fragen

1. **Solr-Cloud-Client (SolrJ-only) im Native-Build**: lohnt sich der Aufwand, das vom embedded-Solr abzukoppeln und SolrJ als Remote-Client native-fähig zu machen? Oder bleibt Solr im Native-Build komplett außen vor?
2. **GraalVM-Version**: 21 LTS oder 24 LTS? Hat Auswirkungen auf Avaje-Inject-Kompatibilität.
3. **Cross-Compile**: gibt es eine Linux-zentrische CI, die Linux-Native baut, plus separate macOS-/Windows-Runner? Oder reicht erstmal Linux?
4. **Distribution-Modell**: Native-Binary plus separater Installer (Notarization, Code-Signing) — wer macht das, in welcher Phase? Aktuell nicht im Plan.
5. **Auto-Update für Native-Binary**: ohne JVM-Self-Update-Mechanismus muss ein eigener Update-Pfad definiert werden. Vermutlich erst für ein Folge-Release relevant.
6. **AWB-Native-ROI**: lohnt der 2–3-Wochen-Aufwand für Phase 4, wenn der jlink-Build bereits ein akzeptables Distribution-Ergebnis liefert? Vor Phase 4 explizit Stakeholder-Entscheidung einholen.

---

## 11. Was dieser Plan nicht abdeckt

- **CDI-/Avaje-Migration** — Vorbedingung, siehe [migration-plan-avaje.md](migration-plan-avaje.md).
- **JPMS-Vollausbau** — wird in Release A so weit getrieben, wie für Avaje und Native-Image nötig. Kein Selbstzweck hier.
- **Architektur-Cleanup** — siehe [architecture-cleanup-plan.md](architecture-cleanup-plan.md).
- **Solr-Refactoring** zu SolrJ-Remote-Only — wäre eigenes Mini-Projekt, ggf. als Vorarbeit für Solr-im-Native-Build.
- **Performance-Tuning** des Native-Binaries jenseits der Build-Defaults — eigene Aktivität nach Stabilisierung.
