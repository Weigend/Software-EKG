# Architektur-Konsolidierungsplan AWB (Software-EKG 7)

> Hinweis: Reine Analyse und Soll-Konzept. Es wurden keine pom.xml- oder Java-Dateien geändert. Reviewbasis für die Diskussion vor jeder Migration.

---

## 1. Executive Summary

Die AWB-Code-Basis hat 26 aktive Maven-Module unter dem Reactor-Root `awb/`. Nominell wird eine vierschichtige Architektur **UI → Logik → SDK → Utils** angestrebt, faktisch ist die Schichtung in mindestens neun Punkten verletzt:

1. Der Reactor-`<dependencies>`-Block in [awb/pom.xml](awb/pom.xml) zwingt **JavaFX, Weld, CDI, Logback** in JEDES Sub-Modul — auch in reine "Utils"- und "API"-Module.
2. `awb-repository-api` (das einzige sauber benannte API-Modul) zieht `javafx.stage.Window` in eine API-Event-Klasse (`RepositoryModifyEvent`).
3. `awb-aichat-bl` erbt von `javafx.concurrent.Task` (`AgentTask`) — JavaFX in Business-Logik.
4. `awb-da-h2` zieht JavaFX wegen eines eingebetteten H2-Browser-Tools direkt in eine DA-Schicht.
5. `awb-repository-bl` hängt direkt an einer Implementierung (`awb-da-h2`) statt an einer DA-API.
6. Feature-UI-Module (`awb-project`, `awb-repository-ui`, `awb-metricanalyzer-ui`) hängen Code-seitig an `awb-application-base` (`EkgApplicationController`, `BeforeRestoreDefaultLayoutEvent`) — klassische Layer-Inversion (Feature → Application-Bundle).
7. `awb-commons` zieht Weld-SE-Core, WFX-Lookup, Jakarta CDI und Arquillian rein — also massive Framework-Tiefe in einer angeblichen "Utils"-Library (ein einziger Verursacher: `BeanProvider.java`).
8. `awb-explorer` und `awb-project` sind Feature-Module ohne `-api`/`-bl`/`-ui`-Tiering. Beide enthalten intern die drei Schichten in EINEM Artefakt; bei `awb-project` existieren bereits Packages `api/`, `bl/`, `ui/` — sie sind nur nicht in eigene Module separiert.
9. Der `awb-importer`-Parent fügt das SDK als `<scope>provided</scope>` ein, was im finalen Build-Bundle dazu führt, dass das SDK aus den Importer-Jars verschwindet — inkonsistent zur Reactor-pom (compile).

Hinzu kommen zwei Geist-Module auf Disk (`awb-metadata-api`, `awb-project-api`), die nur `target/`-Verzeichnisse enthalten und nicht mehr im Reactor stehen — vermutlich Reste eines früheren Refactorings.

---

## 2. Ist-Analyse

### 2.1 Modul-Inventar

| Kategorie | Modul | Pom-Datei | Packaging |
|---|---|---|---|
| Reactor-Root | `awb` | `awb/pom.xml` | pom |
| Application | `awb-application` | `awb/awb-application/pom.xml` | pom |
| Application | `awb-application-base` | `awb/awb-application/awb-application-base/pom.xml` | jar |
| Application | `awb-application-linux` | `awb/awb-application/awb-application-linux/pom.xml` | pom |
| Application | `awb-application-osx` | `awb/awb-application/awb-application-osx/pom.xml` | pom |
| Application | `awb-application-win` | `awb/awb-application/awb-application-win/pom.xml` | pom |
| Common-UI | `awb-common-ui` | `awb/awb-common-ui/pom.xml` | pom |
| Common-UI | `awb-common-ui-misc` | `awb/awb-common-ui/awb-common-ui-misc/pom.xml` | jar |
| Common-UI | `awb-common-ui-chartng` | `awb/awb-common-ui/awb-common-ui-chartng/pom.xml` | jar |
| Common-UI | `awb-common-ui-explorer` | `awb/awb-common-ui/awb-common-ui-explorer/pom.xml` | jar |
| Utils | `awb-commons` | `awb/awb-commons/pom.xml` | jar |
| DA | `awb-da` | `awb/awb-da/pom.xml` | pom |
| DA | `awb-da-h2` | `awb/awb-da/awb-da-h2/pom.xml` | jar |
| DA | `awb-da-solr` | `awb/awb-da/awb-da-solr/pom.xml` | jar |
| DA | `awb-da-elasticsearch` | `awb/awb-da/awb-da-elasticsearch/pom.xml` | jar |
| DA | `awb-da-clickhouse` | `awb/awb-da/awb-da-clickhouse/pom.xml` | jar |
| Feature (sauber) | `awb-repository` | `awb/awb-repository/pom.xml` | pom |
| Feature (sauber) | `awb-repository-api` | `awb/awb-repository/awb-repository-api/pom.xml` | jar |
| Feature (sauber) | `awb-repository-bl` | `awb/awb-repository/awb-repository-bl/pom.xml` | jar |
| Feature (sauber) | `awb-repository-ui` | `awb/awb-repository/awb-repository-ui/pom.xml` | jar |
| Feature (degeneriert) | `awb-project` | `awb/awb-project/pom.xml` | jar |
| Feature (degeneriert) | `awb-explorer` | `awb/awb-explorer/pom.xml` | jar |
| Feature (teil-tiered) | `awb-metricanalyzer` | `awb/awb-metricanalyzer/pom.xml` | pom |
| Feature (teil-tiered) | `awb-metricanalyzer-bl` | `awb/awb-metricanalyzer/awb-metricanalyzer-bl/pom.xml` | jar |
| Feature (teil-tiered) | `awb-metricanalyzer-ui` | `awb/awb-metricanalyzer/awb-metricanalyzer-ui/pom.xml` | jar |
| Feature (teil-tiered) | `awb-aichat` | `awb/awb-aichat/pom.xml` | pom |
| Feature (teil-tiered) | `awb-aichat-bl` | `awb/awb-aichat/awb-aichat-bl/pom.xml` | jar |
| Feature (teil-tiered) | `awb-aichat-ui` | `awb/awb-aichat/awb-aichat-ui/pom.xml` | jar |
| Importer | `awb-importer` | `awb/awb-importer/pom.xml` | pom |
| Importer | `awb-importer-csv` | `awb/awb-importer/awb-importer-csv/pom.xml` | jar |
| Importer | `awb-importer-binance` | `awb/awb-importer/awb-importer-binance/pom.xml` | jar |
| Importer | `awb-importer-prometheus` | `awb/awb-importer/awb-importer-prometheus/pom.xml` | jar |
| Importer | `awb-importer-yfinance` | `awb/awb-importer/awb-importer-yfinance/pom.xml` | jar |
| Importer | `awb-importer-owid-covid-online` | `awb/awb-importer/awb-importer-owid-covid-online/pom.xml` | jar |
| Sandbox | `sandbox` | `awb/sandbox/pom.xml` | jar |

**Geist-Module ohne pom (nur target/)**: `awb/awb-metadata-api/`, `awb/awb-project-api/` — Aufräumen.

### 2.2 Zielschichten-Mapping (ist)

```
[UI]   awb-application-*, awb-common-ui-*, awb-explorer, awb-project (ui-Anteil),
       awb-repository-ui, awb-metricanalyzer-ui, awb-aichat-ui

[BL]   awb-repository-bl, awb-metricanalyzer-bl, awb-aichat-bl,
       awb-project (bl-Anteil), awb-da-* (DA ist faktisch BL für Persistenz)

[API]  awb-repository-api, awb-project (api-Anteil),
       (fehlt explizit für: explorer, metricanalyzer, aichat, da, application)

[SDK]  awb-sdk (extern)

[Utils] awb-commons (verschmutzt), wfx-* (extern)
```

### 2.3 Modul-Abhängigkeitsmatrix (Compile-Scope, nur reactor-interne Deps)

Legende: `X` = direkte Maven-Abhängigkeit. Spalten = "depends on", Zeilen = "from".

| from \ to | commons | cu-misc | cu-chartng | cu-explorer | da-h2 | da-solr | da-es | da-ch | repo-api | repo-bl | repo-ui | proj | expl | ma-bl | ma-ui | ai-bl | ai-ui | app-base |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| commons | – | | | | | | | | | | | | | | | | | |
| cu-misc | X | – | | | | | | | | | | | | | | | | |
| cu-chartng | X | X | – | | | | | | | | | | | | | | | |
| cu-explorer | X | X | | – | | | | | | | | | | | | | | |
| da-h2 | X | | | | – | | | | X | | | | | | | | | |
| da-solr | X | | | | | – | | | X | | | | | | | | | |
| da-es | | | | | | | – | | X | | | | | | | | | |
| da-ch | X | | | | | | | – | X | | | | | | | | | |
| repo-api | X | | | | | | | | – | | | | | | | | | |
| repo-bl | | | | | X | | | | X | – | | | | | | | | |
| repo-ui | | X | | X | | | | | | X | – | | | | | | | X |
| proj | X | X | | X | X | | | | X | | X | – | | | | | | X |
| expl | X | | | X | | | | | X | | | X | – | | | | | |
| ma-bl | X | | | | X | | | | X | | | X | | – | | | | |
| ma-ui | X | X | X | X | | | | | | | | | X | X | – | | | X |
| ai-bl | X | | | | | | | | | | | | | X | | – | | |
| ai-ui | X | X | | | | | | | | | | | | | X | X | – | |
| app-base | X | X | | | | | | | | | | | | | | | | – |

Wichtige Beobachtungen aus dieser Matrix:

1. **DA → Repository-API ist normal** (Adapter braucht das Interface), aber der Reactor setzt damit die Reihenfolge `repo-api` muss vor `da-*` gebaut sein.
2. **`repo-bl → da-h2`** ist eine Verletzung: BL hängt an DA-Implementierung statt an einer DA-API.
3. **`proj → da-h2`** ist analog die gleiche Verletzung in einem Feature-Modul.
4. **`ma-bl → da-h2`** ist analog die gleiche Verletzung.
5. **`proj → repo-ui`** ist eine Cross-Feature-Abhängigkeit auf `-ui`-Ebene — sollte über `-api` gehen.
6. **`repo-ui, proj, ma-ui → app-base`** ist die Layer-Inversion (Feature → App-Bundle).
7. **`expl → proj`** zeigt, dass `awb-explorer` ein Feature-Modul ist, das vom `awb-project`-Feature abhängt; korrekt (sofern nur API).

### 2.4 Konkrete Schichtverletzungen (mit Pfad und Zeile)

| Nr | Verletzung | Datei | Zeile | Bemerkung |
|---|---|---|---|---|
| V1 | JavaFX in Repository-API | [awb/awb-repository/awb-repository-api/src/main/java/io/softwareecg/awb/repository/api/events/RepositoryModifyEvent.java](awb/awb-repository/awb-repository-api/src/main/java/io/softwareecg/awb/repository/api/events/RepositoryModifyEvent.java) | 5 | `import javafx.stage.Window;` — API-Event hält UI-Referenz |
| V2 | JavaFX in BL | [awb/awb-aichat/awb-aichat-bl/src/main/java/io/softwareecg/awb/aichat/bl/agent/AgentTask.java](awb/awb-aichat/awb-aichat-bl/src/main/java/io/softwareecg/awb/aichat/bl/agent/AgentTask.java) | 3, 9 | `extends javafx.concurrent.Task<String>` |
| V3 | JavaFX in DA | [awb/awb-da/awb-da-h2/src/main/java/io/softwareecg/awb/da/h2/browser/H2BrowserApp.java](awb/awb-da/awb-da-h2/src/main/java/io/softwareecg/awb/da/h2/browser/H2BrowserApp.java) | 3-15 | Eigenständiges JavaFX-Tool im DA-Modul + JavaFX-Dep in `awb-da-h2/pom.xml:44-48` |
| V4 | BL → DA-Impl statt DA-API | [awb/awb-repository/awb-repository-bl/pom.xml](awb/awb-repository/awb-repository-bl/pom.xml) | 32-36 | `<artifactId>awb-da-h2</artifactId>` |
| V5 | Feature → App-Bundle | [awb/awb-project/src/main/java/io/softwareecg/awb/project/ui/EkgProjectModule.java](awb/awb-project/src/main/java/io/softwareecg/awb/project/ui/EkgProjectModule.java) | 3 | `import io.softwareecg.awb.application.base.impl.EkgApplicationController;` |
| V5b | Feature → App-Bundle | [awb/awb-repository/awb-repository-ui/src/main/java/io/softwareecg/awb/repository/ui/EkgRepositoryAdminModule.java](awb/awb-repository/awb-repository-ui/src/main/java/io/softwareecg/awb/repository/ui/EkgRepositoryAdminModule.java) | 3 | dito |
| V5c | Feature → App-Bundle | [awb/awb-metricanalyzer/awb-metricanalyzer-ui/src/main/java/io/softwareecg/awb/metricanalyzer/ui/EkgMetricAnalyzerModule.java](awb/awb-metricanalyzer/awb-metricanalyzer-ui/src/main/java/io/softwareecg/awb/metricanalyzer/ui/EkgMetricAnalyzerModule.java) | 3-4 | dito + `BeforeRestoreDefaultLayoutEvent` |
| V6 | Utils-Modul mit CDI/WFX-Tiefe | [awb/awb-commons/src/main/java/io/softwareecg/awb/commons/beans/BeanProvider.java](awb/awb-commons/src/main/java/io/softwareecg/awb/commons/beans/BeanProvider.java) | 1-5 | Einzige Klasse die Weld+WFX-Lookup nutzt; verseucht ganzes Modul |
| V7 | Feature ohne Tiering | [awb/awb-project/pom.xml](awb/awb-project/pom.xml) + `awb/awb-project/src/main/java/io/softwareecg/awb/project/{api,bl,ui}` | – | Drei Schichten in einem Artefakt |
| V8 | Feature ohne Tiering | [awb/awb-explorer/pom.xml](awb/awb-explorer/pom.xml) + `awb/awb-explorer/src/main/java/io/softwareecg/awb/explorer/ui/*` | – | Nur "ui"-Package, aber das Modul ist für andere Features Plattform-API |
| V9 | Globale `<dependencies>` im Reactor | [awb/pom.xml](awb/pom.xml) | 585-677 | JavaFX, Logback, Weld, Jakarta-EE in JEDES Modul |
| V10 | Cross-Feature über UI | [awb/awb-project/pom.xml](awb/awb-project/pom.xml) | 49-53 | `awb-repository-ui` Dep statt `awb-repository-api` |
| V11 | Inkonsistenter SDK-Scope | [awb/awb-importer/pom.xml](awb/awb-importer/pom.xml) | 32-37 | `provided`, vs. global `compile` im Reactor |
| V12 | Geist-Module | `awb/awb-metadata-api/`, `awb/awb-project-api/` | – | Verzeichnis ohne pom, nur `target/` |
| V13 | DA-Module hängen an Repository-Domain | `awb/awb-da/awb-da-h2/pom.xml`, `-solr`, `-es`, `-ch` | je `awb-repository-api` Dep | DA hängt am Repository-API. Korrekt nur, wenn DA-API in `repo-api` lebt — sonst sollte eine eigene `awb-da-api` her |

### 2.5 Symptome aus dem Top-Level-Pom

[awb/pom.xml:585-677](awb/pom.xml#L585-L677) (`<dependencies>` direkt im Reactor-Pom) ist die Wurzel der Schichten-Erosion. Jedes Submodul erbt automatisch:

- `awb-sdk` (ok)
- `jakarta.enterprise.cdi-api`, `jakarta.annotation-api`, `jakarta.activation-api` (nur für BL+ sinnvoll)
- `javafx-fxml`, `javafx-controls`, `javafx-swing` (NUR für UI)
- `logback-classic`, `logback-core` (Bindung der Logging-Implementierung gehört nur ins App-Bundle)
- `slf4j-api` (ok)
- `commons-lang3` (ok in fast allen Modulen)
- Test: `hamcrest`, `junit`, `mockito-core`

Konsequenz: Auch wenn ein Modul-pom keinerlei JavaFX-Deps deklariert, hat es trotzdem JavaFX im Klassenpfad. Das ist der Hauptgrund dafür, dass JavaFX-Verstöße nie auffallen (Maven schreit nicht). Es macht zudem die DependencyConvergence-Erkennung über Enforcer wirkungslos.

---

## 3. Soll-Architektur und Konsistenzregeln

### 3.1 Schichten-Definition

```
+-------------------------------------------------------------------------------------+
| Distribution-Layer:    awb-application-{linux,osx,win}                              |
|   reine Assembly, keine Java-Klassen                                                |
+-------------------------------------------------------------------------------------+
| Application-Layer:     awb-application-base (Bootstrap, Controller, Default-Layout) |
|   Plus: awb-application-api  (NEU, fuer Module-Hooks wie EkgApplicationController-IF)|
+-------------------------------------------------------------------------------------+
| Feature-Layer:                                                                      |
|   Pro Feature genau drei Module:                                                    |
|     awb-<feature>-api  : Interfaces, DTOs, Events. Nur SDK + commons-utils + JDK.   |
|     awb-<feature>-bl   : Service-Implementierungen. Darf api + andere -api. NICHT UI|
|     awb-<feature>-ui   : JavaFX-Code. Darf eigenes -bl, -api, fremde -api, common-ui|
+-------------------------------------------------------------------------------------+
| Common-UI-Layer:       awb-common-ui-{misc,chartng,explorer}                        |
|   Wiederverwendbare UI-Bausteine. Darf JavaFX, controlsfx, common-utils, SDK.       |
+-------------------------------------------------------------------------------------+
| Data-Access-Layer:                                                                  |
|   awb-da-api           (NEU, optional): das Interface, das DA-Adapter implementieren|
|   awb-da-{h2,solr,es,ch}: Adapter-Implementierungen. Nur DA-API + commons-utils +SDK|
+-------------------------------------------------------------------------------------+
| SDK-Layer:             awb-sdk (extern). Stabile API, von allem importiert.         |
+-------------------------------------------------------------------------------------+
| Utils-Layer:                                                                        |
|   awb-commons-utils    : NEU - JDK + SLF4J + Apache Commons. NICHTS sonst.          |
|   awb-commons-cdi      : NEU/aufgesplittet - Weld/WFX/CDI-Glue.                     |
+-------------------------------------------------------------------------------------+
```

### 3.2 Verbindliche Konsistenzregeln (DURCHGESETZT VIA TOOLING)

**R1 (Utils-Reinheit)**
`awb-commons-utils` darf nur abhängen von: JDK, `org.slf4j:slf4j-api`, `org.apache.commons:commons-lang3`, `org.apache.commons:commons-collections4`, `commons-io:commons-io`, `commons-validator:commons-validator`. Keine CDI-, JavaFX-, WFX-, Weld-, Jakarta-EE-Imports.

**R2 (API-Reinheit)**
Jedes `*-api`-Modul darf nur abhängen von: `awb-sdk`, `awb-commons-utils`, `jakarta.inject-api`, `jakarta.validation-api` (DTO-Validation), `jakarta.enterprise.cdi-api` (nur für `@Qualifier`/`@Event`-Annotationen ohne Container), Jackson-annotations (für Serialisierungs-Annotationen). KEIN JavaFX, KEIN WFX, KEIN Weld-Impl, KEINE BL- oder UI-Module.

**R3 (BL-Reinheit)**
Jedes `*-bl`-Modul darf JavaFX nicht importieren. Erlaubt: eigenes `*-api`, fremdes `*-api`, `awb-commons-utils`, `awb-commons-cdi`, SDK, DA-API, Jackson, Apache Commons, jakarta-EE. KEIN `awb-application-*`, KEIN `*-ui`-Modul.

**R4 (UI-Cross-Feature)**
`*-ui`-Module dürfen nur eigenes `*-bl` + eigenes `*-api` + fremde `*-api` + `awb-common-ui-*` + `awb-application-api` referenzieren. KEINE fremden `*-ui`- oder `*-bl`-Module.

**R5 (DA-Reinheit)**
DA-Module dürfen nur `awb-da-api` (Interfaces), `awb-commons-utils`, SDK und ihre konkreten DB-Treiber nutzen. KEIN JavaFX, KEIN `awb-repository-*`, KEINE Feature-Module.

**R6 (Application-Bundle frei von Feature-API-Tipps)**
`awb-application-base` darf Features nur via `awb-application-api`-definierte Hooks (CDI-Discovery + Event-Bus) integrieren. Features ihrerseits importieren nur `awb-application-api`, niemals `awb-application-base`.

**R7 (Common-UI-Reinheit)**
`awb-common-ui-*` darf NICHT von Feature- oder Repository-Code abhängen. Kein `awb-repository-api`, kein `awb-project`, kein `awb-da-*`. Erlaubt: JavaFX, controlsfx, jfxtras, FranzXaver, WFX-API, `awb-commons-utils`, SDK.

**R8 (Keine globalen UI/CDI/Logback im Reactor)**
`awb/pom.xml` darf nur SDK und Test-Frameworks als globale `<dependencies>` haben. JavaFX, Weld, Jakarta-EE, Logback gehören nur per `dependencyManagement` (Versionspflege) und müssen in jedem Modul explizit deklariert werden.

**R9 (Kein zyklischer Cross-Feature-Code)**
Kein Feature darf direkt das `*-bl` oder `*-ui` eines anderen Features importieren. Feature-zu-Feature-Kommunikation NUR via `*-api` (Events + Service-Interfaces).

**R10 (Single-Source-of-Truth pro Funktion)**
Eine Klasse lebt in genau einem Modul. Insbesondere darf der `awb-application-base.EkgApplicationController` nicht von Features benutzt werden — statt dessen ein `WindowManagerHookApi` in `awb-application-api`.

**R11 (Konsistenter SDK-Scope)**
Das SDK ist überall `compile`. Der `provided`-Scope in `awb-importer/pom.xml:32-37` wird auf `compile` geändert, ODER das Importer-Setup wird als deployment-spezifisches Plugin-Bundle behandelt (dann konsequent in allen Importern).

**R12 (Tooling)**
Die Regeln werden via Maven-Enforcer (`banned-dependencies` + `dependencyConvergence`) und ArchUnit-Tests in einem zentralen Modul `awb-architecture-tests` erzwungen.

---

## 4. Migrationsplan (Schritte, in Reihenfolge)

Jeder Schritt ist so groß, dass er als ein einzelner PR mit grünem Build durchläuft. Reihenfolge ist gewählt, damit nach jedem Schritt der `mvn clean install -P dist` weiterhin baut.

### Schritt 0 — Hygiene (PR S0)

- Lösche die Geist-Verzeichnisse `awb/awb-metadata-api/` und `awb/awb-project-api/` (jeweils mit `git rm -r`, da nur `target/` enthalten).
- Aktualisiere `.gitignore`, falls nötig.
- Risiko: minimal. Dieser PR berührt keine pom.xml.

### Schritt 1 — Reactor-Pom entrümpeln (PR S1)

Ziel: `awb/pom.xml` enthält keine `<dependencies>`-Einträge mehr, die UI/CDI/Logback global einziehen. Alle Versionen bleiben in `dependencyManagement`. Jedes Modul deklariert künftig explizit, was es braucht.

Schritte:

1. Verschiebe alle Einträge aus `awb/pom.xml:585-677` in eine Liste.
2. In jeder modul-pom (jar-Packaging) ergänze die Deps, die das Modul aus dem `mvn dependency:list` heraus tatsächlich braucht. Stützpunkt: vorher `mvn dependency:tree -Dverbose` pro Modul ausführen.
3. SDK + slf4j-api bleiben global, weil sie in jedem Modul gebraucht werden.
4. Logback-Bindings (`logback-classic`, `logback-core`) wandern NUR in `awb-application-base`.
5. JaXB (Zeilen 597-606): nur in den Modulen, die XML serialisieren (Importer + ggf. App-Base) — nicht global.

Risiko: Hoch. Build kann pro Modul scheitern, weil Klassen plötzlich fehlen. Empfehlung: Schritt in zwei sub-PRs.

- S1a: Move JavaFX out of global, fix pro Modul.
- S1b: Move Logback/CDI out of global, fix pro Modul.

Betroffene Pfade: `awb/pom.xml` + JEDE modul-pom (28 Stück).

### Schritt 2 — Geist-Module endgültig + Application-API einführen (PR S2)

Ziel: Brechen der Layer-Inversion `Feature → awb-application-base`.

1. Lege neues Modul `awb-application/awb-application-api` an, packaging jar.
2. Verschiebe `EkgApplicationController` (interface-fähigen Teil), `BeforeRestoreDefaultLayoutEvent` und alle weiteren Typen, die Features als Hook brauchen, dort hin.
   - Konkrete Pfade:
     - `awb/awb-application/awb-application-base/src/main/java/io/softwareecg/awb/application/base/impl/EkgApplicationController.java` → Interface in api, Impl in base.
     - `awb/awb-application/awb-application-base/src/main/java/io/softwareecg/awb/application/base/events/BeforeRestoreDefaultLayoutEvent.java` → api.
3. `awb-application-base` deklariert dependency auf `awb-application-api`.
4. In `awb-project/pom.xml`, `awb-repository/awb-repository-ui/pom.xml`, `awb-metricanalyzer/awb-metricanalyzer-ui/pom.xml` ersetze die Dependency `awb-application-base` durch `awb-application-api`.
5. Passe Imports an: `import io.softwareecg.awb.application.base.impl.EkgApplicationController` → `io.softwareecg.awb.application.api.EkgApplicationController` (in den drei Feature-`Module`-Klassen).

Betroffene Dateien:
- `awb/awb-application/awb-application-base/pom.xml`
- `awb/awb-application/pom.xml` (`<modules>`-Liste erweitern)
- `awb/awb-project/src/main/java/io/softwareecg/awb/project/ui/EkgProjectModule.java`
- `awb/awb-repository/awb-repository-ui/src/main/java/io/softwareecg/awb/repository/ui/EkgRepositoryAdminModule.java`
- `awb/awb-metricanalyzer/awb-metricanalyzer-ui/src/main/java/io/softwareecg/awb/metricanalyzer/ui/EkgMetricAnalyzerModule.java`
- pom-Dateien der drei Feature-UI-Module
- neuer Pfad `awb/awb-application/awb-application-api/`

Risiko: Mittel. Reihenfolge: zuerst api-Modul anlegen + Interfaces extrahieren, dann Imports tauschen, dann alte Dep entfernen.

### Schritt 3 — JavaFX aus repository-api entfernen (PR S3)

Ziel: V1 beheben.

1. Datei `awb/awb-repository/awb-repository-api/src/main/java/io/softwareecg/awb/repository/api/events/RepositoryModifyEvent.java`: `javafx.stage.Window` raus. Statt einer Window-Referenz im Event nimm einen abstrakten `Object source` oder `String windowId`. Falls die UI tatsächlich die Window-Instanz braucht, hält der UI-Empfänger sie selbst (z.B. in einem Singleton).
2. Sicher: Alle Konsumenten (grep nach `RepositoryModifyEvent`) anpassen.

Betroffene Dateien:
- `awb/awb-repository/awb-repository-api/src/main/java/io/softwareecg/awb/repository/api/events/RepositoryModifyEvent.java`
- alle Producer/Consumer (grep ergibt voraussichtlich Treffer in `awb-repository-ui` und `awb-application-base`).

Risiko: Niedrig.

### Schritt 4 — JavaFX aus aichat-bl entfernen (PR S4)

Ziel: V2 beheben (`AgentTask extends javafx.concurrent.Task<String>`).

1. Definiere im `awb-aichat-bl` ein eigenes Service-Interface `AsyncAgentExecutor`, das ein `CompletableFuture<String>` zurückgibt (oder ein eigenes `Progress`-Callback).
2. Die JavaFX-Task-Wrapper-Klasse wandert in `awb-aichat-ui` als `JavaFxAgentTask`.
3. Konsumenten in `awb-aichat-ui` auf den Wrapper umstellen.

Betroffene Dateien:
- `awb/awb-aichat/awb-aichat-bl/src/main/java/io/softwareecg/awb/aichat/bl/agent/AgentTask.java`
- neu: `awb/awb-aichat/awb-aichat-ui/src/main/java/io/softwareecg/awb/aichat/ui/agent/JavaFxAgentTask.java`
- alle Aufrufer (vermutlich in `awb-aichat-ui`).

Risiko: Niedrig. Möglicherweise nutzt der ReAct-Agenten-Loop direkt `Task.updateMessage(...)` — dann muss eine eigene Progress-Schnittstelle her.

### Schritt 5 — awb-da-api einführen + DA-Module entkoppeln (PR S5)

Ziel: V13 beheben (DA hängt nicht mehr direkt an Repository-Domain) und sauberen DA-Layer schaffen.

1. Lege Modul `awb/awb-da/awb-da-api` an. Packaging jar.
2. Identifiziere die Interfaces, die ALLE DA-Module heute aus `awb-repository-api` importieren (basierend auf der Analyse: `EkgRepository`, `RepositoryClient`, `RepositoryClientFactory`, `RepositoryService`, `RepositoryClientAware`, Schema-Klassen, `EkgRepositoryDbType`, Search/Sort/Expression DTOs, `RepositoryChangeEvent`).
3. Entscheide pro Typ:
   - Persistenz-Vertrag (Read/Write) → `awb-da-api`.
   - Domain-Events fuer Repositories → verbleibt in `awb-repository-api`, wird abhängig von `awb-da-api`.
4. DA-Module hängen künftig nur an `awb-da-api`, nicht an `awb-repository-api`.
5. `awb-repository-api` hängt zusätzlich an `awb-da-api`.
6. `awb-repository-bl` bleibt der einzige Konsument konkreter DA-Module — optional via Service-Discovery (CDI), so dass auch `repo-bl` nur `awb-da-api` zur Compile-Zeit sieht.

Risiko: Hoch. Drei sub-PRs:
- S5a: Modul anlegen, leer.
- S5b: Move ausgewählter Interfaces.
- S5c: DA-Module umstellen, alte Dep entfernen.

Betroffene poms: `awb/awb-da/pom.xml`, `awb/awb-da/awb-da-{h2,solr,elasticsearch,clickhouse}/pom.xml`, `awb/awb-repository/awb-repository-api/pom.xml`, `awb/awb-repository/awb-repository-bl/pom.xml`.

### Schritt 6 — JavaFX aus awb-da-h2 entfernen (PR S6)

Ziel: V3 beheben.

1. Verschiebe das gesamte `browser/`-Package (`awb/awb-da/awb-da-h2/src/main/java/io/softwareecg/awb/da/h2/browser/H2BrowserApp.java`) in ein neues, eigenständiges Modul `awb-tools/awb-h2-browser` (oder ein Submodul von `awb-application/`). Das ist ein UI-Tool, kein DA-Modul.
2. Entferne den JavaFX-Eintrag in `awb/awb-da/awb-da-h2/pom.xml:44-48`.
3. `run-h2-browser.sh` (im Repo-Root) muss auf das neue Modul zeigen.

Risiko: Niedrig. Tool ist isoliert.

### Schritt 7 — awb-repository-bl entkoppeln von awb-da-h2 (PR S7)

Ziel: V4 beheben (BL hängt an DA-Impl).

1. Stelle sicher, dass `awb-repository-bl` ausschließlich `awb-da-api` braucht.
2. Falls noch direkte H2-spezifische Logik vorhanden ist: wandere sie nach `awb-da-h2` als Implementierung der DA-API.
3. Entferne `<artifactId>awb-da-h2</artifactId>` aus `awb/awb-repository/awb-repository-bl/pom.xml:32-36`.
4. Test: `awb-repository-bl` darf jetzt OHNE `awb-da-h2` im Klassenpfad bauen UND mit (über CDI-Discovery zur Laufzeit).

Voraussetzung: Schritt 5 ist abgeschlossen.

### Schritt 8 — awb-project tieren (PR S8)

Ziel: V7 beheben. `awb-project` aufsplitten in `awb-project-api`, `awb-project-bl`, `awb-project-ui`.

1. Lege Sub-Module an:
   - `awb/awb-project/awb-project-api/pom.xml`
   - `awb/awb-project/awb-project-bl/pom.xml`
   - `awb/awb-project/awb-project-ui/pom.xml`
   `awb/awb-project/pom.xml` wird zum pom-aggregator.
2. Verschiebe Code:
   - `io/softwareecg/awb/project/api/**` → awb-project-api/src/main/java/...
   - `io/softwareecg/awb/project/bl/**` → awb-project-bl/src/main/java/...
   - `io/softwareecg/awb/project/ui/**` → awb-project-ui/src/main/java/...
   - `src/main/resources/io/softwareecg/awb/project/ui/**` → awb-project-ui/src/main/resources/...
3. `awb-project-bl` hängt an `awb-project-api` + `awb-da-api`. KEIN `awb-da-h2` mehr (Discovery via CDI).
4. `awb-project-ui` hängt an `awb-project-bl`, `awb-project-api`, `awb-application-api`, `awb-common-ui-*`, `awb-repository-api` (statt `awb-repository-ui`).
5. Alle Konsumenten von `awb-project` ändern ihre Dep auf das jeweils richtige Sub-Modul (`awb-project-api` reicht meistens).

Risiko: Hoch. Größter Schritt im Plan.

Betroffene Konsumenten (basierend auf Matrix): `awb-explorer` (api), `awb-metricanalyzer-bl` (api), `awb-application-{linux,osx,win}` (ui).

### Schritt 9 — awb-explorer tieren oder eingliedern (PR S9)

`awb-explorer` enthält nur `ui`-Code. Zwei Optionen, je nach Diskussion:

Option A — eigenes Tiering:
1. Anlegen `awb-explorer/awb-explorer-api` (für ItemBuilder-Interface, Events) und `awb-explorer/awb-explorer-ui`.
2. `awb-explorer-api` hängt nur an SDK + commons-utils.

Option B — Einsortieren in `awb-common-ui-explorer`:
1. Da `awb-common-ui-explorer` der "generische Tree" ist und `awb-explorer` der EKG-spezifische Aufsatz, fusioniere oder rename: `awb-common-ui-explorer` bleibt generisch, `awb-explorer` wird zu `awb-explorer-ui` (klar als Feature gekennzeichnet).

Empfehlung: Option A, weil `awb-metricanalyzer-ui` bereits `awb-explorer` als API für ItemBuilder-Registrierung nutzt — das ist eine API-Beziehung.

Betroffene poms: `awb/awb-explorer/pom.xml` + alle Konsumenten.

### Schritt 10 — awb-commons aufsplitten (PR S10)

Ziel: V6 beheben (CDI/WFX in "Utils").

1. Neues Modul `awb-commons-utils` (rein JDK + slf4j + Apache Commons + commons-io). Erbe-`<dependencies>` aus `awb-commons` minus Weld + WFX.
2. Neues Modul `awb-commons-cdi` (CDI/Lookup-Glue: `BeanProvider.java` + `module/`-Package falls CDI-affin).
3. `awb-commons-ssh` als Optional-Modul, weil `sshd-core` + `eddsa` nur von Aichat-BL gebraucht werden (siehe `aichat-bl/pom.xml:48-57`).
4. Alle Konsumenten passen die Dep entsprechend an.

Falls Aufwand zu groß: Mindestens den `BeanProvider` aus `awb-commons` herausziehen und entweder in `awb-application-api` (wenn nur dort gebraucht) oder neuen `awb-commons-cdi` schieben — dann kann der Rest von `awb-commons` die Weld+WFX-Deps verlieren.

Risiko: Mittel. Viele Module hängen an `awb-commons`. Zwei sub-PRs:
- S10a: Code-Move + neues Modul anlegen.
- S10b: Aufrufer auf splittete Deps umstellen.

### Schritt 11 — Cross-Feature-Bereinigung (PR S11)

Ziel: V10 beheben.

1. `awb-project/pom.xml:49-53`: Dep auf `awb-repository-ui` entfernen, durch `awb-repository-api` ersetzen.
2. Code in `awb-project-ui` analysieren: welche UI-Klassen aus `awb-repository-ui` werden genutzt? Falls "Repository-Auswahl-Dialog" — der gehört NICHT nach repository-ui sondern nach project-ui (oder als geteilte Komponente in `awb-common-ui-misc`).

Risiko: Mittel. Analysefenster vor Implementierung notwendig.

### Schritt 12 — Importer-SDK-Scope harmonisieren (PR S12)

Ziel: V11 beheben.

1. Entscheidung: bleiben Importer extern-deployable Plugins (provided) oder gebundlte Module (compile)?
2. Konsequente Umstellung in `awb/awb-importer/pom.xml:32-37`.
3. Falls compile: in den `awb-application-{linux,osx,win}` keine zusätzliche Action notwendig (Transitive). Falls provided: braucht Anti-Verifier dass Importer trotzdem im Bundle landen (assembly-config).

### Schritt 13 — Tooling: Maven Enforcer + ArchUnit (PR S13)

Ziel: R12 implementieren.

1. Im Reactor-Pom `<pluginManagement>`: `maven-enforcer-plugin:3.5.0` für alle Module aktivieren mit Regeln:
   - `dependencyConvergence` global.
   - `requireMavenVersion`/`requireJavaVersion` pin.
   - Pro Modulart eigene `bannedDependencies` (siehe Beispiel im Abschnitt 5).
2. Neues Modul `awb-architecture-tests` mit ArchUnit (`com.tngtech.archunit:archunit-junit5:1.3.0`):
   - Regel "BL packages must not depend on javafx..".
   - Regel "API packages must not depend on `*-bl` or `*-ui` packages".
   - Regel "DA packages must not depend on Feature-BL packages".
3. CI-Pipeline (sofern vorhanden): `mvn verify` muss diese Tests einschließen.

Risiko: Niedrig. Inkrementell aktivierbar.

### Schritt 14 — module-info (optional, langfristig) (PR S14+)

Ziel: Java-Module-System nutzen, um Layer-Bruch zur Compile-Zeit unmöglich zu machen.

1. Pro Modul ein `module-info.java`, das nur `requires` deklariert, was wirklich gebraucht wird.
2. JavaFX kann dann nur in UI-Modulen `requires javafx.controls` haben.

Aufwand: hoch wegen automatic-modules (Weld-SE, Solr u.a. sind keine echten JPMS-Module). Daher ans Ende.

---

## 5. Tooling-Beispiele

### 5.1 Maven Enforcer — bannedDependencies pro Modul-Klassifikation

Template für ein `*-api`-Modul (zu integrieren via `<pluginManagement>` + Aktivierung pro Modul):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>banned-deps-api</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <bannedDependencies>
                        <excludes>
                            <exclude>org.openjfx:*</exclude>
                            <exclude>org.controlsfx:*</exclude>
                            <exclude>org.jfxtras:*</exclude>
                            <exclude>org.jboss.weld.se:*</exclude>
                            <exclude>io.softwareecg.wfx:*</exclude>
                            <exclude>io.softwareecg.awb:awb-*-bl</exclude>
                            <exclude>io.softwareecg.awb:awb-*-ui</exclude>
                            <exclude>io.softwareecg.awb:awb-application-base</exclude>
                            <exclude>ch.qos.logback:*</exclude>
                        </excludes>
                    </bannedDependencies>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Ähnlich für `*-bl` (banned: javafx, controlsfx, application-base) und `awb-commons-utils` (banned: alles außer JDK/SLF4J/Apache).

### 5.2 ArchUnit-Regelbeispiel

```java
@AnalyzeClasses(packages = "io.softwareecg.awb")
class LayerArchitectureTest {
    @ArchTest
    static final ArchRule bl_must_not_depend_on_javafx =
        noClasses().that().resideInAPackage("..bl..")
            .should().dependOnClassesThat().resideInAnyPackage("javafx..");

    @ArchTest
    static final ArchRule api_must_not_depend_on_bl_or_ui =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage("..bl..", "..ui..");

    @ArchTest
    static final ArchRule features_must_not_depend_on_application_base =
        noClasses().that().resideOutsideOfPackages("io.softwareecg.awb.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("io.softwareecg.awb.application.base.impl..");
}
```

### 5.3 Optional: jdeps in CI

`jdeps --multi-release 21 -summary -recursive` pro Modul-jar in CI — findet auch Reflection-Verstöße, die Maven nicht sieht.

---

## 6. Risiko-Übersicht

| Schritt | Risiko | Hauptursache | Mitigation |
|---|---|---|---|
| S0 | Niedrig | – | Manuelle Kontrolle |
| S1 | Hoch | Klassenpfad-Verluste | In zwei sub-PRs aufteilen, pro Modul `mvn dependency:list` vorher loggen |
| S2 | Mittel | Interfaces falsch geschnitten | Nur Klassen mit nachweislicher Verwendung extrahieren |
| S3 | Niedrig | Wenige Konsumenten | grep + manueller Refactor |
| S4 | Niedrig | Lokal | – |
| S5 | Hoch | Falsche Schichten-Zuordnung der Interfaces | Drei sub-PRs, ausführliche Code-Review |
| S6 | Niedrig | Lokal | Skript anpassen |
| S7 | Mittel | CDI-Discovery muss funktionieren | Smoke-Test mit jedem DA-Adapter im Bundle |
| S8 | Hoch | Größter Code-Move | Drei-stufiger PR (Module anlegen, Code move, Konsumenten) |
| S9 | Mittel | Konsumenten | Vorher Diskussion Option A vs B |
| S10 | Mittel | Viele Konsumenten | Sub-PRs |
| S11 | Mittel | Zyklen-Gefahr | Vorab Code-Inspektion |
| S12 | Niedrig | Build-Konfig | Smoke-Test mit `dist`-Profil |
| S13 | Niedrig | Keine Code-Änderung | – |

---

## 7. Reihenfolge-Begründung

S0 → S1 trennt zuerst die globalen Top-Level-Verseuchungen. Erst dann werden in S2-S4 die einzelnen Code-Verstöße gefixt — die wären ohne S1 schwer zu sehen, weil JavaFX überall im Klassenpfad steckt.

S5 vor S7 ist Pflicht: erst die DA-API muss existieren, bevor die BL ihre direkte Impl-Dep verlieren kann.

S8 spät, weil größter Move; muss auf S2 (App-API) und S5 (DA-API) aufbauen, sonst kollidieren die Tier-Schnitte.

S13 (Tooling) ganz zum Schluss, weil die Regeln rote Builds erzeugen würden, solange Verstöße bestehen. Sobald S2-S12 grün sind, kann das Tooling den Status quo einfrieren.

---

## 8. Offene Fragen für die Diskussion

1. **Sandbox-Modul**: bleibt es im Reactor? Es zieht heute `awb-da-solr`, `awb-repository-bl`, `awb-project` rein — alle "harten" Module. Vorschlag: aus dem Default-Reactor in ein optionales Profil verschieben.
2. **`awb-application-api`**: lieber `awb-application-spi`? Beide Namen sind vertretbar; SPI signalisiert klarer "Plugin-Punkt für Features".
3. **DA-API-Schnitt**: Welche Klassen wandern aus `awb-repository-api` nach `awb-da-api`? Vorgehen: Pärchenweise reviewen mit Domain-Expertise. Default-Annahme: alle `RepositoryClient*`/`RepositoryService`-Interfaces + Search/Sort/Expression-DTOs.
4. **`awb-explorer` Option A vs B**: Tiering oder Eingliederung in common-ui? Hängt davon ab, wie viele andere Features die Explorer-API erweitern werden.
5. **`awb-importer`-SDK-Scope**: Plugin-Konzept (provided) oder bundle-compile? Hat Auswirkungen auf das Distributions-Assembly.
6. **`awb-commons` Aufsplittung**: Drei Module (utils, cdi, ssh) oder nur zwei (utils, ekg-runtime)? Drittes ist sauberer, aber mehr Wartung.
7. **module-info**: lohnt sich der Aufwand, oder reicht Enforcer + ArchUnit für die nächsten 12 Monate? Im AWB-Kontext (Desktop-App) wahrscheinlich nicht zwingend.

---

## 9. Schichtkonformität — Soll-Matrix (Wunschzustand nach Migration)

| from \ to | utils | cdi-glue | sdk | da-api | da-impl | repo-api | repo-bl | repo-ui | proj-api | proj-bl | proj-ui | expl-api | expl-ui | ma-api | ma-bl | ma-ui | ai-api | ai-bl | ai-ui | app-api | app-base |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| utils | – | | | | | | | | | | | | | | | | | | | | |
| cdi-glue | X | – | | | | | | | | | | | | | | | | | | | |
| sdk | X | | – | | | | | | | | | | | | | | | | | | |
| da-api | X | | X | – | | | | | | | | | | | | | | | | | |
| da-impl | X | | X | X | – | | | | | | | | | | | | | | | | |
| repo-api | X | | X | X | | – | | | | | | | | | | | | | | | |
| repo-bl | X | X | X | X | | X | – | | | | | | | | | | | | | | |
| repo-ui | X | | X | | | X | X | – | | | | | | | | | | | | X | |
| proj-api | X | | X | | | X | | | – | | | | | | | | | | | | |
| proj-bl | X | X | X | X | | X | | | X | – | | | | | | | | | | | |
| proj-ui | X | | X | | | X | | | X | X | – | | | | | | | | | X | |
| expl-api | X | | X | | | X | | | X | | | – | | | | | | | | | |
| expl-ui | X | | X | | | X | | | X | | | X | – | | | | | | | X | |
| ma-api | X | | X | | | X | | | X | | | | | – | | | | | | | |
| ma-bl | X | X | X | X | | X | | | X | X | | | | X | – | | | | | | |
| ma-ui | X | | X | | | X | | | X | | | X | X | X | X | – | | | | X | |
| ai-api | X | | X | | | | | | | | | | | X | | | – | | | | |
| ai-bl | X | X | X | | | X | | | | | | | | X | X | | X | – | | | |
| ai-ui | X | | X | | | | | | | | | | | | | X | X | X | – | X | |
| app-api | X | | X | | | | | | | | | | | | | | | | | – | |
| app-base | X | X | X | X | X | X | X | X | X | X | X | X | X | X | X | X | X | X | X | X | – |

Wichtige Eigenschaften der Soll-Matrix:
- Kein `*-api` hat eine Abhängigkeit auf ein `*-bl` oder `*-ui` (R2).
- Kein `*-bl` hat eine Abhängigkeit auf ein `*-ui` (R3).
- Cross-Feature-Beziehungen laufen IMMER nur über `*-api` (R9).
- Nur `app-base` hat das Recht, alle anderen Module einzubinden (es ist das Bootstrap-Bundle).

---

## 10. Aufwand-Schätzung (grob, in Personentagen)

| Schritt | Aufwand |
|---|---|
| S0 | 0.5 |
| S1 | 3 |
| S2 | 2 |
| S3 | 0.5 |
| S4 | 1 |
| S5 | 4 |
| S6 | 0.5 |
| S7 | 2 |
| S8 | 4 |
| S9 | 2 |
| S10 | 3 |
| S11 | 1 |
| S12 | 0.5 |
| S13 | 2 |
| S14 | 5+ |
| **Summe (ohne S14)** | **26** |

---

### Kritische Dateien für die Umsetzung

- [awb/pom.xml](awb/pom.xml) — Wurzel der globalen Verseuchung; entrümpeln in S1.
- [awb/awb-application/awb-application-base/src/main/java/io/softwareecg/awb/application/base/impl/EkgApplicationController.java](awb/awb-application/awb-application-base/src/main/java/io/softwareecg/awb/application/base/impl/EkgApplicationController.java) — Quelle der Layer-Inversion; teilen in S2.
- [awb/awb-repository/awb-repository-api/src/main/java/io/softwareecg/awb/repository/api/events/RepositoryModifyEvent.java](awb/awb-repository/awb-repository-api/src/main/java/io/softwareecg/awb/repository/api/events/RepositoryModifyEvent.java) — JavaFX-Verstoß in API; bereinigen in S3.
- [awb/awb-repository/awb-repository-bl/pom.xml](awb/awb-repository/awb-repository-bl/pom.xml) — BL hängt an DA-Impl; bereinigen in S5+S7.
- [awb/awb-project/pom.xml](awb/awb-project/pom.xml) und [awb/awb-project/src/main/java/io/softwareecg/awb/project/](awb/awb-project/src/main/java/io/softwareecg/awb/project/) — größter strukturierter Move in S8.
