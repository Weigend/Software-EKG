# Add a source to New Analysis

[Handbook home](README.md) · [Extensions](extensions.md)

A source in New Analysis is a way the data of a topic comes into an analysis: "Linux hosts through
a Prometheus server" or "Linux hosts read directly with `top`". Adding one takes four parts. The
first three live in the Workbench, the fourth in the Collector:

| Part | Contract | Where |
| --- | --- | --- |
| The source and the topics it is offered for | `AnalysisSourceProvider` | `datasource.spi` |
| The step of the wizard that asks for its settings | `AnalysisSourceWizardPage` | `datasource.ui` |
| What the user decided, as a Collector configuration | `CollectionPlan` | `datasource.model`, implemented in `datasource.adapter.<source>` |
| The code that reads the values | `CounterSource` | a Collector source module |

The Workbench does not collect by itself. A plan compiles to a Collector configuration, and the
Collector embedded in the Workbench runs it. A source without a Collector source module behind it
can be shown but cannot deliver a value.

## Steps

1. **Have a Collector source.** Build it as described under
   [Build a Collector source](extensions.md#build-a-collector-source), or use an existing one.
   Add its module as a dependency of `workbench/pom.xml`, next to `collector-source-prometheus`,
   `collector-source-unix` and `collector-source-windows`. Its ID is what the plan names as
   `sourceType()`.
2. **Describe the source.** Implement `AnalysisSourceProvider` as a `@Singleton` class in the
   Workbench module. `definition()` returns one `AnalysisSourceDefinition`: a stable ID, the title
   on the tile, whether one address stands for many systems (`aggregating`), and whether the
   source can be chosen (`available`). `topics()` maps every `AnalysisTopic` the source serves to
   an `AnalysisSourceOption` with the sentence the user reads when choosing.
3. **Write the plan.** Implement `CollectionPlan` in `datasource.adapter.<source>`.
   `toCollectorConfig` returns the Collector YAML; `SshPlan` with `SshConfigCompiler` is the
   example to follow. The configuration never contains the password or token: write
   `CollectorConfigCompiler.SECRET_PLACEHOLDER` instead and return the value from `secret()`. The
   Workbench keeps it in the secret store. `firstHostGroup()` and `firstMetrics()` say which chart
   opens when the first values arrive.
4. **Write the page.** Implement `AnalysisSourceWizardPage` as a `@Singleton` class in
   `datasource.ui`. Its `sourceId()` is the ID of the definition. `node()` is the content of the
   step, `title` and `hint` are its heading, `isComplete()` says whether the analysis can start,
   and `plan(projectName)` returns the plan from step 3.
5. **Connect the page to the dialog** in `attach(analysisName, nameIsFixed, changed)`:
   - Show a field for the name of the analysis and bind it to `analysisName`. The analysis has
     one name, asked on the last step, and the page may be that step. Disable the field when
     `nameIsFixed` is true: the source is then added to an existing analysis.
   - Call `changed` whenever the answer of `isComplete()` may have changed. Without it the dialog
     never enables its start button.
   - `attach` is called once for every dialog that opens. A singleton page is reused, so reset
     its fields there.
6. **Test it.** `NewAnalysisDialogTest.aContributedSourceIsOfferedShownAndStarted` is the
   template: it registers a provider and a page, walks through the wizard and checks the plan the
   analysis starts with. Add tests for the plan's YAML, credentials, cancellation and an endpoint
   that cannot be reached.

Both singletons are found through the application's injection scan. `AnalysisSourceRegistry`
collects the providers, and `NewAnalysisDialog` collects the pages. Nothing in the dialog is
edited. The built-in sources are listed in `BuiltinAnalysisSourceProviders`.

## Rules the registry and the dialog enforce

- **Two sources with the same ID** are rejected when the registry is built.
- **An available source without a page** stops the dialog from opening, for every source and every
  user. Register provider and page together, or set `available` to false until the page exists.
- **A source that is not available** stays visible and greyed out. Use it for what is planned.
- **The Collector source ID is persisted.** What `sourceType()` returns is stored with every data
  source of an analysis, together with the compiled configuration. Renaming it needs a migration.
  The ID of the definition is not stored; it names the step and the controls of the dialog.

## Limits

- **Topics are a fixed list.** `AnalysisTopic` is an enum, and a topic's tile can be chosen only
  if the enum marks the topic as available. Kubernetes, Databases and Services are switched off
  there today. A provider can offer a source for such a topic, but the user cannot reach it until
  the flag in the enum is changed. A new topic is a new enum value.
- **Choosing no topic** leads to the Prometheus page. That path is built into the dialog.
- **The contract and the page are separated.** `datasource.spi` has no JavaFX and knows no
  adapter; `LayerArchitectureTest` enforces this. Only the wizard page uses JavaFX.
