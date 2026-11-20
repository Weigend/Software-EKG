# Migrationsplan: CDI/Weld → Avaje Inject (Release A)

> **Stand 2026-04-28**: Der Code-Migrationspfad ist vollständig. Avaje läuft parallel zu CDI als zweiter, end-to-end-tauglicher Runtime; der finale Production-Switch ist noch ausstehend (manueller Smoke-Test) und drei Cleanup-Items danach. Siehe Abschnitt 11.
>
> Verwandte Dokumente:
> - [architecture-cleanup-plan.md](architecture-cleanup-plan.md) — Schichten-Cleanup (S0–S13 weitgehend abgeschlossen, S14 module-info offen)
> - [migration-plan-graalvm.md](migration-plan-graalvm.md) — Folgeprojekt; setzt diesen Plan voraus

---

## 1. Executive Summary

Ziel: Ablösen von Weld/Jakarta-CDI durch **Avaje Inject** mit JSR-330-Annotationen (`jakarta.inject`) und **ServiceLoader** für Cross-Module-Discovery. Resultat:

- Kein Container-Boot, keine Klassenpfad-Scans, keine Runtime-Reflection für DI.
- JPMS-tauglich ohne `opens`-Klauseln (Avaje generiert Plain-Code zur Compile-Zeit).
- Bootstrap signifikant schneller (kein Bean-Discovery-Lauf).
- Vorbedingung für Release B (GraalVM Native-Image).

Tangiert sind **WFX** (3 Module mit echter CDI-SPI-Tiefe), **awb-sdk** (eine Hilfsklasse) und alle 24 **AWB-Module**. Der **Collector** nutzt heute keine CDI — keine Migration dort nötig.

Aufwand realistisch: **5–7 Wochen Mannarbeit**. Tatsächlich verbraucht in dieser Session: **~1 Tag** Code-Arbeit (Plus die Recherche/Diskussion).

---

## 2. Bestand

### 2.1 CDI-Footprint AWB

| Feature | Vorkommen | Komplexität |
|---|---|---|
| `@Inject` | 44× | trivial |
| `@Singleton` | 34× | trivial |
| `@ApplicationScoped` | 6× | trivial (eager) |
| `@Default` / `@Qualifier` | 8 + 5× | mittel (Resolution) |
| `@Priority` + `@Alternative` | 6 + 2× | mittel |
| `@Specializes` | 2× | mittel |
| `@Produces` | 3× | trivial → Avaje `@Factory`/`@Bean` |
| `@Disposes` | 1× | trivial → Avaje `destroyMethod` |
| `@Observes` | 2× | siehe unten — ersatzlos |
| `@PostConstruct` / `@PreDestroy` | je 3× | trivial |
| `Event<T>.fire()` | 2× | siehe unten |
| `Instance<T>.select()` | 6× | mittel — wird zu `BeanScope.list()` oder `ServiceLoader` |
| `BeanManager` + SPI | 2× | beide tun dasselbe (Field-Injection in Non-Beans) |

Der einzige `@Observes BookmarkEvent` lebt in [ChartPanelAdapter.java](awb/awb-metricanalyzer/awb-metricanalyzer-ui/src/main/java/io/softwareecg/awb/metricanalyzer/ui/adapter/ChartPanelAdapter.java) und kann durch den schon migrierten WFX-EventBus ersetzt werden (Commit `29f3c3b "Migrate AWB callers from EkgEventBus to the WFX EventBus"`).

`Instance<T>` wird vor allem in den DA-Modulen genutzt, um alle implementierenden Beans (`SolrCloudRepository`, `ElasticSearchRepository`, …) zu sammeln — eine bewusste Plugin-API. Wird zu `ServiceLoader.load(T.class)` oder `BeanScope.list(T.class)`.

### 2.2 CDI-Footprint WFX

| Feature | Vorkommen | Anmerkung |
|---|---|---|
| `@Inject` | 16× | |
| `@Singleton` | 14× | |
| `@ApplicationScoped` | 3× | |
| `@Priority` | 5× | |
| `@PostConstruct` | 5× | |
| `@Produces` | 4× | u.a. `EventBusProducer`, `FXMLLoaderProducer` |
| `@Observes` | 3× | im EventBus-Modul selbst |
| `@Default` | 3× | |
| `@Alternative` | 1× | |

Drei nicht-triviale Stellen:

1. **`wfx-modules/lookup-cdi`** — eine `LookupStrategy`-Implementierung, die `BeanManager.getBeans(...).select()` aufruft. Pendant `ServiceLoaderLookupStrategy` existiert bereits parallel im `lookup`-Modul, das Lookup-Konzept ist also schon container-agnostisch.
2. **`wfx-modules/extensions/cdi-contexts`** — echte CDI-Extensions: `EagerExtension`, `ViewScoped`-Custom-Scope (`AbstractJfxContext`, `ScopeHandler`, `ViewContextExtension`) und `FXMLLoaderProducer`. Custom-Scopes sind der komplexeste SPI-Bereich von CDI.
3. **`wfx-modules/platform-core/.../EventBusProducer`** — Producer-Methode für `SimpleEventBus`. Direkt durch Avaje-Factory ersetzbar.

### 2.3 CDI-Footprint awb-sdk

Genau eine Stelle: [awb-sdk/awb-sdk/src/main/java/io/softwareecg/awb/sdk/core/cdi/CdiInjection.java](awb-sdk/awb-sdk/src/main/java/io/softwareecg/awb/sdk/core/cdi/CdiInjection.java) — ~25 Zeilen Field-Injection in Non-Bean-Instanzen (FXML-Controller). Wird `AvajeInjection.injectFields(t)` mit gleicher Signatur.

### 2.4 CDI-Footprint Collector

Keine. Migration entfällt. Optional in Phase 4.

---

## 3. Ziel-Architektur

```
jakarta.inject (JSR-330)         (Annotationen, bleibt)
        │
        ▼
Avaje Inject @Factory / BeanScope  (Compile-time-DI, ersetzt Weld)
        │
        ▼
WFX Lookup mit AvajeLookupStrategy  (ersetzt CDILookupStrategy)
        │
        ▼
ServiceLoader                       (Cross-JAR-Discovery, JPMS-nativ)
```

**Annotation-Mapping**:

| CDI | Avaje |
|---|---|
| `@ApplicationScoped` | `@Singleton` |
| `@Singleton` | `@Singleton` |
| `@Inject` (Field/Constructor) | `@Inject` (Field/Constructor) — gleiche Annotation aus `jakarta.inject` |
| `@Produces` | Klasse mit `@Factory`, Methode mit `@Bean` |
| `@Disposes` | `@Bean(destroyMethod="close")` |
| `@Qualifier` | `@Qualifier` (gleiche `jakarta.inject.Qualifier`) |
| `@Priority` + `@Alternative` | `@Primary` oder `@Secondary`, oder Avaje-`@Order` |
| `@Specializes` | nicht direkt — wird durch explizites `@Replaces` oder Test-Bean-Override gelöst |
| `@PostConstruct` / `@PreDestroy` | identisch (Avaje unterstützt sie) |
| `Instance<T>` | `BeanScope.list(T.class)` oder `ServiceLoader.load(T.class)` |
| `Event<T>` + `@Observes` | WFX-EventBus oder direktes Methodenaufruf-Pattern |
| `BeanManager.createInjectionTarget` | `BeanScope.inject(instance)` |

**Wichtige Avaje-Constraint (verifiziert in S0.1)**: Avaje generiert Plain-Java zur Compile-Zeit, **keine Reflection**. Konsequenz: `@Inject` auf **privaten Feldern funktioniert nicht** — Avaje erzeugt direkten Zugriff (`target.field = scope.get(...)`), der Java-Sichtbarkeit unterliegt. Lösung: Felder müssen entweder package-private sein oder — empfohlen — auf **Constructor-Injection** umgestellt werden (final-Felder, `@Inject` am Konstruktor).

**Verteilung im Codebase** (gezählt am 2026-04-28):
- AWB: **23 private `@Inject`-Felder** — alle müssen refactort werden
- WFX: **3 private `@Inject`-Felder** + 2 Constructor-Injections schon vorhanden
- Insgesamt ~26 mechanische Refactorings über S1 (WFX) und S3 (AWB) verteilt

**Module entfallen** nach Migration:
- `wfx-modules/lookup-cdi` (ersetzt durch `lookup-avaje`)
- `wfx-modules/extensions/cdi-contexts` (Custom-Scopes neu in WFX-Avaje-Glue oder gestrichen)
- `awb-commons/awb-commons-cdi` → umbenennen in `awb-commons-di`

**Dependencies entfallen** im Reactor-Pom:
- `org.jboss.weld.se:weld-se-core`
- `jakarta.enterprise:jakarta.enterprise.cdi-api`
- `org.jboss.arquillian.testng:arquillian-weld-embedded`

**Dependencies bleiben**:
- `jakarta.inject:jakarta.inject-api` (Standard-Annotationen)
- `jakarta.annotation:jakarta.annotation-api` (`@PostConstruct`, `@PreDestroy`, `@Priority`)

**Dependencies neu**:
- `io.avaje:avaje-inject` (Runtime, ~150 KB)
- `io.avaje:avaje-inject-generator` (Annotation-Processor, nur `provided`/Test-Scope)

---

## 4. Migrationsplan

### Phase 0 — Spike & Discovery (1 Woche)

Ziel: drei harte Risiken vor Code-Migration ausräumen.

#### S0.1 — Avaje-Spike auf `awb-da-h2` ✅ ABGESCHLOSSEN (2026-04-28)
Throw-away-Branch `spike/avaje-da-h2` im awb-Submodule. Erkenntnisse:

- ✅ **Annotation-Processor läuft sauber** mit `maven-compiler-plugin:3.13.0` + `avaje-inject-generator:11.4`. Generiert je Bean ein `*$DI.java` plus zentrales `H2Module.java`. ServiceLoader-Datei `META-INF/services/io.avaje.inject.spi.InjectExtension` wird automatisch erzeugt.
- ✅ **`BeanScope.builder().build()`** verdrahtet alle 5 H2-Beans in 489 ms; Test grün.
- ✅ **`@PostConstruct` und `@PreDestroy`** feuern korrekt aus `jakarta.annotation`.
- ✅ **JSR-330-Annotationen** (`@Singleton`, `@Inject`) bleiben unverändert — kein Code-Touch ausser Field→Constructor.
- ⚠️ **Field-Injection auf private Feldern schlägt fehl** (siehe Abschnitt 3). Workaround: Constructor-Injection für alle 4 Repository-Klassen — ~5 Zeilen pro Klasse, mechanisch.
- ⚠️ **Altes `META-INF/beans.xml`** wird vom Build mitkopiert; muss in S3 explizit gelöscht werden (war nicht im ursprünglichen Plan).
- 🆕 **Avaje 11.4** als Zielversion festgelegt; Java 21 OK. Im Reactor-Pom ein zentrales Property `<avaje.version>` einführen.

**Aufwand bisher**: 0,5 Tage (statt 2). Spike-Branch bleibt als Referenz.

#### S0.2 — `cdi-contexts`-Audit
Im AWB-Code grep nach `@ViewScoped`, `@Eager`, `ScopeHandler`, `ViewContextExtension`. Ergebnis bestimmt S1.3:
- **Treffer leer**: WFX-Modul `cdi-contexts` ersatzlos streichen, nur `FXMLLoaderProducer` als Avaje-Factory neu schreiben (~50 Zeilen).
- **Treffer da**: Custom-Scope-Implementierung in Avaje neu bauen via `BeanScope`-Hierarchie (parent-scope `@Singleton`, child-scope pro View).

**Aufwand**: 0,5 Tag.

#### S0.3 — WFX-Lookup-API-Stabilität prüfen
[CDILookupStrategy.java](wfx/wfx-modules/lookup-cdi/src/main/java/io/softwareecg/wfx/lookup/cdi/CDILookupStrategy.java) genau lesen. Welche `BeanManager`-Methoden werden konkret benutzt? Lassen die sich 1:1 auf `BeanScope` mappen?

**Aufwand**: 0,5 Tag.

#### S0.4 — Avaje-Inject-Build-Setup verifizieren
Annotation-Processor-Konfiguration in `maven-compiler-plugin` für ein Multi-Module-Reactor durchspielen. Sicherstellen, dass jedes Modul seinen eigenen `BeanScope`-Generator-Output bekommt und die Composition über `provides io.avaje.inject.spi.Module with ...` in `module-info.java` (oder `META-INF/services` ohne JPMS) funktioniert.

**Aufwand**: 1 Tag.

#### S0.5 — Backout-Plan ✅ ABGESCHLOSSEN

**Branch-Topologie:**
- **Outer-Repo** (software-ekg-7): `feature/release-a-avaje-migration` ← entwickelt gegen `main`. Enthält Plan-Dokument und Submodule-Pointer-Bumps.
- **awb-Submodule**: alle Migrations-Commits auf `feature/release-a-avaje-migration` (gleicher Name, eigene Historie). Spike-Erkenntnisse stehen separat auf `spike/avaje-da-h2` (Commit `8688a5b`) — wird **nicht gemerged**, bleibt als Referenz erhalten.
- **wfx-Submodule**: eigener Branch `feature/release-a-avaje-migration` für Phase-1-Arbeit.

**Merge-Strategie:** Erst wenn alle Phasen 1–4 grün durchgelaufen sind und die Akzeptanzkriterien (Abschnitt 7) erfüllt, mergen wir die Submodule-Branches auf deren `master`/`main` und bumpen den Submodule-Pointer im Outer-Repo. Bis dahin bleibt `main` im Outer-Repo komplett unberührt.

**Backout-Verfahren bei Misserfolg:**
1. Submodule-Branches verwerfen (oder als historische Referenz tagged stehen lassen): `git push origin --delete feature/release-a-avaje-migration`.
2. Outer-Repo: `git branch -D feature/release-a-avaje-migration`.
3. `main` bleibt in beiden Ebenen unverändert. Reactor baut weiterhin auf CDI/Weld.

**Pro Phase ein Stop-Loss:**
- Phase 1 hängt → kein Schaden, WFX-`master` ist intakt.
- Phase 2 hängt → SDK-`master` intakt.
- Phase 3 hängt → AWB-Migration kann inkrementell pro Modul rückwärts gehen, aber nur falls die Phase-1/2-Submodule schon gemerged wären — daher: **Phase 1+2 erst mergen, wenn Phase 3 vollständig grün ist**.

Spike-Branch `spike/avaje-da-h2` ist live im awb-Submodule und reproduzierbar (`mvn -pl awb-da/awb-da-h2 -am clean test`).

---

### Phase 1 — WFX-Migration (2–3 Wochen)

WFX ist die Foundation. Solange WFX an Weld hängt, kann AWB nicht migrieren.

#### S1.1 — Neues Modul `wfx-modules/lookup-avaje`
Spiegelbild zu `lookup-cdi`. Implementiert `LookupStrategy` gegen `BeanScope`. WFX' öffentliche `Lookup`-API bleibt unverändert. AWB merkt nichts davon.

**Betroffene Pfade**:
- neu: `wfx/wfx-modules/lookup-avaje/`
- `wfx/wfx-modules/lookup/src/main/java/io/softwareecg/wfx/lookup/Lookup.java` — keine Änderung
- `wfx/pom.xml` — Modul-Liste

**Aufwand**: 2 Tage.

#### S1.2 — `EventBusProducer` durch Avaje-Factory ersetzen
[EventBusProducer.java](wfx/wfx-modules/platform-core/src/main/java/io/softwareecg/wfx/platform/impl/eventbus/EventBusProducer.java) wird `EventBusFactory` mit `@Factory` + `@Bean`-Methode. Die 3 internen `@Observes`-Stellen im `SimpleEventBus`-Umfeld werden auf direkte Subscription-API umgestellt — der WFX-EventBus hat das schon, war nur historisch zusätzlich an CDI gebunden.

**Aufwand**: 1 Tag.

#### S1.3 — `cdi-contexts`-Modul migrieren oder streichen
Abhängig von S0.2:

**Variante A (kein `@ViewScoped` in AWB genutzt)**:
- Modul `wfx-modules/extensions/cdi-contexts` löschen.
- `FXMLLoaderProducer` als kleine Avaje-Factory in `wfx-modules/platform-core` (oder neu `extensions/avaje-fxml`) neu schreiben.
- `EagerExtension` wird obsolet — Avaje-`@Singleton` ist standardmäßig eager beim ersten `BeanScope`-Build.

**Aufwand**: 2 Tage.

**Variante B (`@ViewScoped` genutzt)**:
- `BeanScope`-Hierarchie nutzen: Application-Scope als Parent, View-Scope als Child mit eigenem Lifecycle.
- View-Erzeugung erzeugt einen Child-Scope, View-Destroy schließt ihn (entsorgt alle View-Beans).
- API-Annotation `@ViewScoped` bleibt erhalten, Implementierung wechselt von CDI-Context auf Avaje-Child-Scope.

**Aufwand**: 1–1,5 Wochen.

#### S1.4 — `module-info.java` für jedes WFX-Modul (verschoben hinter S1.6)
Avaje verlangt kein `opens`. `module-info.java` wird strikt mit `exports`-only.

**Vorgehen-Anpassung (2026-04-28)**: Während CDI/Weld parallel zu Avaje läuft (S1.1–S1.6), müssten alle Module mit `module-info.java` zusätzlich `opens`-Klauseln für Weld-Reflection bekommen, die nach S1.6 wieder zurückgebaut werden müssten. Doppelarbeit. Saubere Lösung: S1.4 nach hinten ziehen — erst nach S1.6, wenn CDI/Weld weg ist, in einem koordinierten Sweep über alle Module.

**Betroffen**: alle 8 WFX-Module unter `wfx/wfx-modules/` + Automatic-Module-Name-Pflege in MANIFESTs.

**Aufwand**: 3 Tage.

#### S1.5 — WFX-Tests von Arquillian auf Avaje portieren ✅ N/A (2026-04-28)

**Audit-Ergebnis**: Die einzigen 4 Arquillian-Tests in WFX leben alle in `wfx-modules/extensions/cdi-contexts` und werden mit dem Modul in S1.6 gelöscht. Kein Migrations-Aufwand in Phase 1.

AWB hat 3 Arquillian-Tests (`awb-da-solr`, `awb-commons-cdi`); die fallen in Phase 3 an, nicht hier.

**Aufwand**: 0 Tage (statt 2).

#### S1.6 — `lookup-cdi` und `cdi-contexts` final entfernen (blockiert bis Phase 3 done)
Nachdem keine WFX- oder AWB-Konsumenten mehr darauf verweisen, Module aus dem Reactor streichen.

**Reihenfolge-Klarstellung (2026-04-28)**: AWB hängt noch an `lookup-cdi` (4 Test-Klassen) und implizit an `cdi-contexts` (FXMLLoaderProducer für `@Inject FXMLLoader`). Diese Konsumenten werden erst in Phase 3 (AWB-Migration) auf die parallelen Avaje-Pfade umgestellt. S1.6 wird daher **nach** Phase 3 ausgeführt und schließt Phase 1 erst dann formal ab.

**Aufwand**: 0,5 Tag.

**Phase-1-Aufwand gesamt**: **2–3 Wochen** (Spreizung wegen S1.3-Variante).

---

### Phase 2 — awb-sdk-Migration (3 Tage)

#### S2.1 — `CdiInjection` durch `AvajeInjection` ersetzen
[CdiInjection.java](awb-sdk/awb-sdk/src/main/java/io/softwareecg/awb/sdk/core/cdi/CdiInjection.java) → neue Klasse `awb-sdk/awb-sdk/src/main/java/io/softwareecg/awb/sdk/core/di/AvajeInjection.java`. Signatur `<T> T injectFields(T)` bleibt; Aufrufer bleiben unverändert.

Package-Move `awb.sdk.core.cdi` → `awb.sdk.core.di`. Alte Klasse als `@Deprecated`-Wrapper für eine Übergangsversion stehen lassen, damit AWB-Module nicht in einem Big-Bang umgestellt werden müssen.

**Aufwand**: 1 Tag.

#### S2.2 — `module-info.java` für SDK
SDK ist Pflicht-Dependency aller AWB-Module. Sauber `exports` definieren, keine `opens`.

**Aufwand**: 2 Tage.

---

### Phase 3 — AWB-Module-Migration (1,5–2 Wochen)

In strikter Reihenfolge gemäß Build-Topologie.

#### S3.1 — `awb-commons-cdi` → `awb-commons-di` (1 Tag)
- Modul-Rename.
- [BeanProvider.java](awb/awb-commons/awb-commons-cdi/src/main/java/io/softwareecg/awb/commons/cdi/BeanProvider.java) entfällt (nur Wrapper um `awb-sdk:CdiInjection`, also überflüssig sobald SDK migriert ist).
- Konsumenten passen Imports an.

#### S3.2 — DA-Module (3 Tage)
Für jedes von `awb-da-{h2,solr,clickhouse,elasticsearch}`:
- `@ApplicationScoped` → `@Singleton`.
- `Instance<T>` → `BeanScope.list(T.class)` oder direkter `ServiceLoader.load(T.class)`.
- `CoreContainerProducer` (Solr) mit `@Disposes` → Avaje `@Bean(destroyMethod="close")`.
- `META-INF/beans.xml` löschen.
- Eigene `module-info.java`.

**Reihenfolge**: H2 zuerst (wurde in Phase 0 schon migriert), dann ClickHouse, ES, Solr.

#### S3.3 — Repository / Project / Metricanalyzer / Aichat (4–5 Tage)
Mechanisches Refactoring:
- Annotation-Mapping aus Tabelle 3.
- `@Produces`-Klassen → `@Factory`-Klassen (3 Stellen im Codebase).
- Der einzelne `@Observes BookmarkEvent` → WFX-EventBus-Subscription.
- `module-info.java` pro Modul.

**Reihenfolge**: api-Module zuerst (sauber), dann bl, dann ui.

#### S3.4 — Importer ✅ N/A (2026-04-28)

**Audit-Ergebnis**: Die Importer-Module nutzen **keine CDI-Annotationen** (verifiziert per Grep über alle 5 Sub-Module). Discovery läuft schon über SPI/ServiceLoader. Keine DI-Migration nötig.

Die JavaFX-`Image`-Verstöße in `CsvImportModule`, `PrometheusImportModule`, `PrometheusLocalImportStrategy` sind architektonisches Cleanup (gehört zum Architektur-Cleanup-Plan, nicht zu Release A). Defer als eigenes Mini-Refactoring.

**Aufwand**: 0 Tage (statt 2).

#### S3.5 — Application-Bundles + Bootstrap (2 Tage)
[EkgCdiApplication.java](awb/awb-application/awb-application-base/src/main/java/io/softwareecg/awb/application/base/EkgCdiApplication.java) → `EkgApplication`. Bootstrap-Logik:
1. `BeanScope` bauen (`BeanScope.builder().build()`).
2. WFX-`Lookup` auf `AvajeLookupStrategy` registrieren.
3. JavaFX-Application-Lifecycle starten.

Code wird ~50 Zeilen, deutlich kürzer als der bisherige Weld-Bootstrap.

#### S3.6 — Reactor-Pom + Architektur-Tests (1 Tag)
- Aus [awb/pom.xml](awb/pom.xml) entfernen: `weld-se-core`, `cdi-api`, `cdi-contexts`, `lookup-cdi`, `arquillian-weld-embedded`.
- Hinzufügen: `avaje-inject` (compile), `avaje-inject-generator` (provided).
- ArchUnit-Regel ergänzen in [LayerArchitectureTest.java](awb/awb-architecture-tests/src/test/java/io/softwareecg/awb/architecture/LayerArchitectureTest.java): kein Modul darf `jakarta.enterprise.*` importieren. Verhindert Backslide.

---

### Phase 4 — Collector-Konsistenz (optional, 2–3 Tage)

Der Collector hat keine CDI-Schuld. Optional kann man in der Gelegenheit:
- Auf `module-info.java` umstellen (lohnt sich für saubere Build-Topologie).
- Plugin-Discovery (Source/Repository-Adapter) auf `provides`-Direktive in `module-info.java` heben statt impliziter Klassenpfad-Discovery.
- Optional Avaje als DI auch im Collector einziehen, falls die manuelle Wiring im Bootstrap zu mühsam wird.

Ohne diese Phase bleibt der Collector stabil weiterlaufen — die DI-Migration zwingt ihn nicht.

---

## 5. Risiken und Mitigation

| Risiko | Wahrscheinlichkeit | Impact | Mitigation |
|---|---|---|---|
| `Instance<T>` / `BeanScope.list()`-Mapping liefert nicht alle Beans | mittel | hoch (DA-Plugin-Discovery bricht) | Phase-0-Spike auf `awb-da-h2` |
| `@ViewScoped` in WFX wirklich genutzt | gering (Audit) | mittel (1 Woche extra) | S0.2 deckt das vorab auf |
| WFX-Lookup-Konsumenten außerhalb AWB | unbekannt | mittel | grep im Suchpfad, ggf. `lookup-cdi` als deprecated mitschleppen |
| Avaje-Annotation-Processor + Maven-Multi-Module fragil | gering | mittel | S0.4, dokumentierte Referenzkonfiguration |
| `@Specializes`-Semantik nicht 1:1 abbildbar | gering | gering (2 Stellen) | Beide Stellen manuell auflösen, kein Container-Trick |
| Tests brechen durch Arquillian-Removal | mittel | gering | Avaje-Test-Support, Mockito-Bridges |
| Bootstrap-Reihenfolge anders als in CDI | mittel | mittel | Eager-Init in Avaje verifizieren, ggf. expliziter Order |

---

## 6. Aufwand-Übersicht

| Phase | Aufwand | Risiko |
|---|---|---|
| 0 — Spike | 1 Woche | – |
| 1 — WFX | 2–3 Wochen | mittel |
| 2 — SDK | 3 Tage | niedrig |
| 3 — AWB | 1,5–2 Wochen | niedrig (mechanisch) |
| 4 — Collector (optional) | 2–3 Tage | niedrig |
| **Summe ohne Phase 4** | **5–6,5 Wochen** | |
| **Summe mit Phase 4** | **5,5–7 Wochen** | |

---

## 7. Akzeptanzkriterien

Release A gilt als abgeschlossen, wenn:

1. `mvn clean install -P dist` baut grün.
2. Im Reactor existiert keine Dependency mehr auf:
   - `org.jboss.weld.se:weld-se-core`
   - `jakarta.enterprise:jakarta.enterprise.cdi-api`
   - `org.jboss.arquillian.*:*`
3. Es existieren keine `META-INF/beans.xml`-Dateien mehr.
4. ArchUnit-Test "kein Modul importiert `jakarta.enterprise.*`" ist grün.
5. AWB startet, lädt mindestens drei Repositories (H2, ClickHouse, Elasticsearch), führt einen Import durch (CSV).
6. Bootstrap-Zeit gemessen: AWB-Cold-Start auf Referenzhardware deutlich unter dem CDI-Baseline-Wert (Erwartung: -30 % oder besser).
7. Pro AWB-Modul existiert ein `module-info.java`, kein `opens`-Eintrag außer für FXML-Loader-Pakete (`opens ... to javafx.fxml`).

---

## 8. Offene Fragen

1. **Phase 4 Collector-Konsistenz**: einbeziehen oder als separates Mini-Projekt? Empfehlung: einbeziehen, weil JPMS-Konsistenz über das gesamte EKG hinweg sinnvoll ist.
2. **`lookup-cdi`-Mitnehmen**: gibt es WFX-Nutzer außerhalb des AWB-Reactor (Drittprojekte), die `CDILookupStrategy` brauchen? Falls ja: Modul deprecated lassen, nicht löschen.
3. **`@ViewScoped`-Audit**: Ergebnis von S0.2 entscheidet, ob Phase 1 in der 2- oder 3-Wochen-Variante landet.
4. **Bootstrap-Reihenfolge**: Avaje hat keinen `@Initialized(ApplicationScoped.class)`-Equivalent. Falls AWB irgendwo auf eine bestimmte Bean-Init-Reihenfolge angewiesen ist, muss die explizit modelliert werden.
5. **PMD/SpotBugs-Regeln**: Müssen ergänzt werden, um unbeabsichtigte CDI-Imports zu blocken? Oder reicht ArchUnit?

---

## 9. Reihenfolge-Begründung

Phase 0 vor Phase 1, weil die drei Spikes Show-Stopper früh sichtbar machen.

Phase 1 (WFX) vor Phase 2/3 (AWB), weil AWB an WFX hängt — solange WFX-Lookup CDI-only ist, kann AWB nicht weg von Weld.

Phase 2 (SDK) vor Phase 3 (AWB), weil alle AWB-Module am SDK hängen.

Phase 3 in Topologie-Reihenfolge: api → bl → ui → application-bundles. Pro Schicht ist die Migration mechanisch, die Testbarkeit pro Schicht hoch.

Phase 4 ans Ende, weil Collector unabhängig ist und nicht blockiert.

---

## 10. Was dieser Plan nicht abdeckt

- **GraalVM Native-Image** — siehe [migration-plan-graalvm.md](migration-plan-graalvm.md). Ist die natürliche Folge dieses Plans, aber inhaltlich getrennt.
- **JPMS-Vollausbau in BL/UI-Modulen mit Reflection-Bibliotheken (Jackson, FXML)** — wird hier nur soweit gemacht, wie Avaje es zwingend braucht. Ein vollständiger JPMS-Ausbau bleibt eigenes Thema.
- **Solr-Strategie für native-Builds** — ist Thema von Release B.
- **Architektur-Cleanup** — siehe [architecture-cleanup-plan.md](architecture-cleanup-plan.md), weitgehend abgeschlossen.

---

## 11. Stand 2026-04-28

**Code-Migration komplett, Production noch nicht geflippt.**

Beide DI-Container koexistieren parallel: jeder Build erzeugt sowohl die CDI- als auch die Avaje-Pfade. Welcher Runtime aktiv ist, entscheidet die Launcher-Klasse beim Start.

### ✅ Abgeschlossen (Code im Repo, Build grün)

| Schritt | Was | Branches/Commits |
|---|---|---|
| S0 (Spike + Discovery) | awb-da-h2 spike grün, cdi-contexts-Audit, Lookup-API-Stabilität, Avaje 11.4 als Zielversion, Backout-Plan | wfx/awb spike, awb cherry-pick |
| S1.1 | wfx-modules/lookup-avaje (5 Tests) | wfx d6de046 |
| S1.2 | EventBusFactory + SimpleEventBusHolder (2 Tests) | wfx d3fadc4 |
| S1.3 | FXMLLoaderFactory + Reflection-Fallback für legacy controllers | wfx c75a3f8, aeb8bdc |
| S1.5 | N/A — Arquillian-Tests nur in cdi-contexts | (S1.6) |
| S2-prep | AvajeLookupStrategy registriert BeanScope für Lookup | wfx 9ca9a91 |
| S2.1 | awb-sdk: AvajeInjection (4 Tests) | sdk fe74244 |
| S3.2 | Alle 4 DA-Module (H2/Solr/ClickHouse/ElasticSearch) dual CDI+Avaje | awb 8b9d9db, 3342d86 |
| S3.3 | BookmarkEvent (CDI Event<>) → WFX EventBus | awb a5f9782 |
| S3.4 | N/A — Importer ohne CDI | – |
| S3.5 (scaffold) | EkgAvajeApplication + AvajeMain als Parallel-Pfad | wfx 09b9c0e, awb e6cc967 |
| S3.5-flip a | 4 BL-Klassen (RepositoryDataAccessServiceImpl, MetricQueryServiceImpl, RepositoryManagerModule, ChartCommandProcessor) Field→Constructor | awb 6d3289f |
| S3.5-flip b | 5 Repository-Klassen (Solr Classic/Cloud/Embedded, ClickHouse Cloud, ES) — Instance<T> raus, Constructor-Injection mit @External @Qualifier | awb 7bd3b35 |
| S3.5-flip c | BeanProvider zum container-agnostischen Dispatcher umgebaut (CDI BeanManager ODER Avaje BeanScope) — keine Call-Site-Edits nötig | awb d547f7c |
| S3.5-flip d | run-dev.sh per `EKG_LAUNCHER` env-var switchbar | awb ad0f495 |
| S3.6 (lite) | avaje.version zentralisiert in Reactor-Pom + dependencyManagement; ArchUnit-Regel R12 (API-Module dürfen kein jakarta.enterprise importieren) | awb 0073ca4 |

### ⏳ Verbleibend (manueller Smoke-Test + Cleanup)

1. **Production-Smoke-Test (manuell)**
   ```bash
   EKG_LAUNCHER=io.softwareecg.awb.application.base.EkgAvajeApplication ./run-dev.sh
   ```
   Verifiziert das Avaje-Bootstrap end-to-end. Falls grün: weiter mit Schritt 2.

2. **Production-Flip (5 Pointer)**
   - `awb-application-base/pom.xml:143` mainClass → `EkgAvajeApplication`
   - `awb-application-osx/.../start-softwareecg.sh:10`
   - `awb-application-linux/.../start-softwareecg.sh:11`
   - `awb-application-win/pom.xml:205` mainClass
   - `run-dev.sh` Default umstellen

3. **S1.6 — CDI-Stack endgültig entfernen**
   - WFX: `lookup-cdi`, `cdi-contexts`-Module aus dem Reactor löschen
   - WFX: `CDIMain` löschen, platform-runner-pom: `lookup-cdi` Dep raus
   - AWB: `EkgCdiApplication` löschen
   - Reactor-Pom: `weld-se-core`, `cdi-api`, `arquillian-weld-embedded` Deps raus
   - BeanProvider: CDI-Pfad löschen, nur Avaje-Pfad behalten

4. **S3.1 — `awb-commons-cdi` → `awb-commons-di` umbenennen**
   - Pom-rename, Package-rename `commons.cdi` → `commons.di`
   - Konsumenten-Imports anpassen (~13 Call-Sites)

5. **S1.4 — `module-info.java` pro WFX-Modul**
   - Nach Weld-Removal: kein `opens` mehr nötig wegen CDI-Reflection
   - Strikt mit `exports`-only

**Dauer für Verbleibendes**: realistisch **0,5–1 Woche**, primär Cleanup-Arbeit.
