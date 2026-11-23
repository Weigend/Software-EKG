# Add a source to New Analysis

[Handbook home](README.md) · [Extensions](extensions.md)

The entry point is `io.softwareecg.awb.datasource.spi.AnalysisSourceProvider`. A provider owns a
stable source ID and the topics under which the source appears. `AnalysisSourceRegistry` validates
the contributions and `NewAnalysisDialog` renders its options; do not edit `AnalysisTopic` or add
a source-specific branch merely to make a tile visible.

1. Implement `AnalysisSourceProvider` and return one immutable `AnalysisSourceDefinition`.
2. Map every supported `AnalysisTopic` to an `AnalysisSourceOption` with user-facing help text.
3. Implement `AnalysisSourceWizardPage`. Its stable `sourceId()` must match the definition; it
   supplies the source-specific page, validation, help text and the final `CollectionPlan`.
4. Register both contributions with the application lookup. `AnalysisSourceRegistry.standard()`
   discovers providers and `NewAnalysisDialog` discovers wizard pages. Built-ins currently live in
   `BuiltinAnalysisSourceProviders`; tests can pass both registries explicitly.
5. Implement discovery and collection behind the datasource ports. Keep network/protocol classes
   in `datasource.adapter`; keep JavaFX pages in `datasource.ui`.
6. Add a registry contract test for discovery, topic visibility and duplicate IDs, plus adapter
   tests for credentials, cancellation and unavailable endpoints.

IDs are persisted integration identities: never derive them from display text and never rename
them without a migration. An unavailable source may stay registered with `available=false`; this
keeps roadmap entries visible without pretending the flow works.

The dialog orders arbitrary source IDs and never needs a new step enum value. The architectural
test enforces that the logical `datasource.spi` contract cannot depend on source adapters or UI;
only the separate wizard-page contribution uses JavaFX.
