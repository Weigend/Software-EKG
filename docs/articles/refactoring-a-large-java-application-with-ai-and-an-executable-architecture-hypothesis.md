# We Built the Target Architecture Before Refactoring the Code

## How a user handbook, an AI coding partner, a synthetic JAR, and Structure202 turned a large Java refactoring into a visual, testable conversation

Large refactorings usually begin with the code. We inspect packages, draw boxes around what is already there, rename a few layers, and gradually produce a cleaner version of the existing structure. This is useful, but it has a built-in bias: the current implementation becomes the vocabulary of the future architecture.

For the Software-EKG workbench, we tried a different route.

We began with the product as a user experiences it: the handbook, screenshots, dialogs, workflows, and extension scenarios. From that evidence, we developed a hypothesis about the internal components the application should have. Then we made that hypothesis executable as a synthetic Java JAR and opened it in Structure202 (S202).

Only after the target structure was visible, discussable, and reasonably stable did we start moving production code. We have since completed that package-level refactoring and built the real application JAR from the target structure.

This changed the role of AI in the project. The AI was not asked to “clean up the architecture.” It helped extract a model, generate a complete refactoring map, create an executable skeleton, and revise hypotheses. S202 provided the visual feedback that allowed a human architect to evaluate the result.

The combination was much more effective than either tool alone.

> **Image for the Medium version:** The starting Software-EKG workbench in S202, before the refactoring. Caption: “The implementation is buildable and functional, but its historical packages dominate the architectural picture.”

## Starting with the handbook instead of the package tree

Software-EKG is a desktop workbench for comparing time series from software systems. An analyst creates a project, connects or imports data, explores available metrics, places selected series into charts, adjusts queries, stores bookmarks, and exports evidence.

The application has grown for years. It supports several repository technologies, multiple import formats, live data sources, a JavaFX user interface, metadata storage, and an AI assistant. Its code still contained historical layer names such as `api`, `bl`, `ui`, `sdk`, and `da`, as well as older module boundaries that had survived after modules were consolidated into one workbench.

Those names described implementation history. They did not always describe the product.

The first question was therefore not “Where should this class move?” It was:

> If we designed the workbench today from its user-visible responsibilities, what components would we expect to find inside it?

The handbook and screenshots exposed a clearer set of responsibilities than the source tree did:

- **Project context** is the data envelope of an investigation: the selected project, its configuration, and its active context.
- **Repository** is the local or remote data store. It is not a subtopic of project management.
- **Data source** owns the “New Analysis” workflow and must be extensible through ports and adapters.
- **Importer** contains import strategies such as CSV, Prometheus, OWID, Binance, and market data.
- **Time series** owns storage/query contracts and technology adapters for Solr, Elasticsearch, and ClickHouse.
- **Metadata** owns persisted projects, repository configuration, collection sources, aliases, and other H2-backed state—not just bookmarks.
- **Explorer**, **query catalog**, **query engine**, **investigation**, **metric view**, and **bookmark** are related but distinct responsibilities in the analysis workflow.
- **Runtime**, **UI toolkit**, **secrets**, and **infrastructure** are lower-level capabilities used by several features.

This analysis also exposed extension points. Anything that already had multiple implementations—or was likely to gain them—needed a visible contract and representative adapters in the target model.

The handbook did not give us a class diagram. It gave us something more important: a product vocabulary.

## Turning the hypothesis into an executable skeleton

A drawing would have been too easy to make internally consistent.

Boxes and arrows do not need to compile. They do not force every existing class to have a destination. They rarely reveal that a supposedly low-level port imports a high-level feature type.

So we generated a Java skeleton.

For every Java source type in the workbench, we created a target fully qualified class name. The mapping covered 579 real source types; `package-info.java` files were excluded because they are not classes. Three oversized source classes were explicitly marked as splits, producing 582 target classes.

Examples included:

```text
sdk.awbapi.repository.SeriesImportService
    -> timeseries.port.TimeSeriesWriteService

project.ui.repositoryselector.RepositorySelectorPane
    -> repository.ui.RepositorySelectorPane

datasource.bl.ProjectSeriesOutput
    -> datasource.adapter.collector.ProjectSeriesOutput

sdk.awbapi.explorer.ExplorerUpdateEvent
    -> runtime.events.ExplorerUpdateEvent
```

The generated classes were deliberately small, but they were not empty. A flat JAR full of unrelated classes would only produce a flat picture.

Each relevant dependency became a real bytecode method call. This mattered because S202 distinguishes a mere type reference from a structural call dependency when calculating architectural levels. The synthetic code did not need to execute, but its bytecode had to express the intended direction of use.

We generated two variants:

1. **A filtered target hypothesis.** Dependencies that violated the proposed layering or closed a cycle were omitted. This showed the architecture we were trying to reach.
2. **An all-edges quality-assurance variant.** Every dependency inferred from the current code was retained, including dependencies that contradicted the target. This showed what the clean picture was hiding.

The second variant was essential. At one point the filtered model looked convincing while its ranking rule silently removed 271 cross-component class dependencies. Some were genuine violations; others revealed that the ranking rule itself was incomplete.

An architecture hypothesis should be falsifiable. The all-edges JAR made ours falsifiable.

## The picture became our shared language

The first skeleton was technically complete and visually disappointing. It was too flat.

The generated classes contained dependency fields, but S202 correctly treated those as type usage rather than structural calls. Once the generator emitted actual method calls, the layers appeared.

The next version had the opposite problem: 4,425 dependency edges and fan-outs as high as 64. The generator had connected every class matching one package prefix to every contract matching another prefix. Every metric-view class depended on every query-engine class. Every time-series adapter depended on every port.

The picture made this mistake obvious.

We replaced prefix cross-products with role-specific relationships and preserved direct dependencies inferred from the real sources. The result dropped to roughly 1,850 structural edges and became readable.

This became the recurring loop:

1. Form a hypothesis in conversation.
2. Encode it in the mapping and generator.
3. Build the JAR.
4. Inspect it in S202.
5. Challenge surprising edges.
6. Decide whether the problem was in the code, the mapping, or the hypothesis.

Without the images, many discussions would have stayed ambiguous. “Repository belongs below project” can mean containment, dependency direction, UI ownership, or persistence. A diagram showing `projectmanagement.repository` immediately exposed the semantic mistake: concrete Solr and ClickHouse repositories had been placed inside a project-management feature.

The visualization did not replace architectural judgment. It made that judgment precise enough to communicate.

> **Image for the Medium version:** The refined skeleton in S202. Caption: “Components that use another component are placed above it. Ports, adapters, features, and infrastructure become visible as one navigable dependency structure.”

## What the skeleton taught us

### Project and repository are different concepts

A project is the data envelope of an investigation. A repository is a local or remote data store. A project can refer to a repository, but repository technology does not belong inside project management.

The target gained separate `projectcontext` and `repository` responsibilities. Project-management dialogs became UI within `projectcontext`, while concrete repository technologies moved into explicit `repository.adapter` packages. Their time-series clients live separately below `timeseries.adapter`.

### The repository write service is a time-series port

The existing `SeriesImportService` was used by importers and data sources and implemented by repository code. Initially we placed it in the importer, then in the repository. Both choices created upward dependencies.

The picture showed the better ownership: writing time-series data is a port of the time-series subsystem. The interface became `timeseries.port.TimeSeriesWriteService`; repository code implements it.

### Shared application helpers were not application features

Types such as `DiInjection`, `BeanProvider`, `ServiceDiscovery`, module contracts, and generic events were used across the application. Keeping them under `application` made “application” both a low-level library and the top-level composition root.

They moved to `runtime`. Application startup and composition remained at the top under `application`, while reusable dependency-injection, discovery, event, and module infrastructure settled below it in `runtime`.

### Split classes require explicit semantics

Three historical classes carried multiple responsibilities. We marked them as `[SPLIT]` rather than pretending a rename was sufficient.

That introduced a generator problem: every user of the old class initially received dependencies on every split result. One split produced thirty artificial dependencies into `metricview` from five components.

The generator was changed to direct existing users to the primary split result only. During the production refactoring, the behavior was then allocated explicitly:

- `QueryFilterParams` became the investigation-owned `QuerySpec`, with editable query state represented by `metricview.queryeditor.QueryDraft`;
- `MetricQueryServiceImpl` became `queryengine.QueryRunner`, while combination, smoothing, simplification, and related transformations moved into `queryengine.pipeline.SeriesTransformPipeline`;
- `ChartPanelController` became `metricview.MetricViewController`, with background investigation state in `investigation.InvestigationStore` and chart-layer composition in `metricview.LayerComposer`.

This was an important boundary of the method: the skeleton identified that classes had to split, but allocating real behavior still required reading and changing the production implementation.

## AI was a model builder, not the architect of record

The AI coding partner could work across several representations at once:

- read handbook text and relate it to screenshots;
- inspect hundreds of Java sources;
- generate and validate a complete source-to-target mapping;
- create compilable dummy classes;
- preserve and classify dependencies;
- build JARs repeatedly;
- compare the model with the current implementation;
- carry accepted corrections back into the generator;
- perform mechanical package moves in the real code;
- compile and run the test suite after each step.

But the most important architectural corrections came from human interpretation:

- “Metadata is not just bookmarks; H2 also stores projects and repository configuration.”
- “Repository and project are completely different topics.”
- “That output type is a collector adapter, not a model.”
- “This provider is implemented by importers, so the extension direction matters.”
- “The image is technically layered but too dense to communicate.”

The productive loop was not human versus AI. It was:

> Human product knowledge → AI-generated executable hypothesis → S202 visualization → human architectural judgment → revised code and model.

S202 acted as the shared external memory of that loop.

## Moving from hypothesis to production code

Once the theoretical model stabilized, we stopped refining it indefinitely.

We created a Git branch from a clean baseline and began at the bottom of the dependency graph. The first production commit moved eight shared types into the new runtime structure:

- `BeanProvider` and `DiInjection` to `runtime.di`;
- `ServiceDiscovery` to `runtime.discovery`;
- generic events to `runtime.events`;
- module contracts to `runtime.module`.

All imports and tests were updated mechanically. That intermediate workbench compiled with its 592 original Java sources, and the test run completed with 306 workbench tests passing and five regularly skipped, plus the required collector tests.

When the real JAR was loaded into S202, the impact of this small first step was already visible. `runtime` appeared at level zero. The composition root stayed at the top. Historical areas such as `sdk`, `common`, and `da` remained, but they were now obvious candidates for the next iteration.

> **Image for the Medium version:** The real application after the first runtime refactoring. Caption: “A small, verified code change immediately changes the architectural picture. Runtime services settle at the bottom; the composition root remains at the top.”

We then applied the complete mapping to production code rather than stopping at the convincing skeleton. The final migration changed or moved 906 files. It removed all production classes from the historical `sdk`, `metricanalyzer`, `da`, `common`, and old `project` packages, migrated FXML, CSS, images, ServiceLoader registrations, distribution scripts, tests, and test data, and implemented the three planned splits as real collaborators.

The resulting workbench contains 593 Java source files. The apparent increase from the 592-file baseline is deliberate: three split results were added while duplicate `package-info.java` files were consolidated. Excluding the remaining 11 `package-info.java` files, the application now contains the expected 582 production types.

A clean reactor test run compiled the refactored application together with the required collector modules. All 306 workbench tests passed, with five regularly skipped. The final application JAR contained no class in any of the retired production packages. Loading that real JAR into S202 produced the architecture we had first explored as a hypothesis—not an identical picture, because real dependencies are richer, but the same understandable component vocabulary and direction.

> **Image for the Medium version:** The fully refactored production JAR in S202. Caption: “The executable hypothesis has become the real package structure. Remaining dependency violations are now visible as concrete follow-up work instead of being hidden by historical package names.”

## A repeatable method for large refactorings

The approach is not specific to Software-EKG or Java:

1. **Recover the product model.** Read user documentation, screenshots, workflows, extension guides, and operational scenarios before treating package names as architecture.
2. **Create a complete class mapping.** Every production type needs a target or an explicit “unclear” marker.
3. **Represent uncertainty honestly.** Use split markers and unresolved decisions instead of forcing one-to-one renames.
4. **Build an executable skeleton.** Generate target packages and encode dependencies in a form the analysis tool recognizes.
5. **Maintain a target view and an all-edges view.** One communicates intent; the other prevents filters from hiding inconvenient facts.
6. **Use visualization as a design review.** Look for unexpected height, fan-out, technology in feature packages, reverse dependencies, cycles, and suspiciously empty components.
7. **Refactor from stable foundations upward.** Move shared contracts and low-level capabilities before features. Keep every step buildable and tested.
8. **Re-analyze the real artifact after every stage.** The skeleton is a hypothesis. The production JAR is the evidence.

## What made this approach different

The unusual part was not using AI to rename Java packages. Coding agents are already good at mechanical refactorings.

The unusual part was building a visual, executable target architecture before touching production code—and deriving that architecture from product evidence rather than from the current package tree.

The skeleton gave the AI something concrete to maintain. S202 gave the human something concrete to judge. Git, compilation, and tests kept the transition honest. The final comparison of the production JAR with the skeleton also prevented the model from becoming a design artifact that was never implemented.

That combination reduced a large refactoring from one intimidating rewrite to a sequence of visible architectural moves.

We did not ask AI to decide what the system means.

We used AI to make our hypotheses complete and executable, used visualization to decide whether those hypotheses made sense, and then used compilation and tests to carry the accepted model into production code.

That is a promising pattern for refactoring large systems: do not begin by moving code. First build a model you can see, challenge, and compile—then replace the hypothesis with a tested real artifact and compare the two.
