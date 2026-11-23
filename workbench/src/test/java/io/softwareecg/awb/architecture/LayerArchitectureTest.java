package io.softwareecg.awb.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architektur-Regeln, die die Schichtung UI -> Logik -> API durchsetzen.
 * <p>
 * Die Workbench ist ein einziges Maven-Modul; ihre Schichten sind Pakete. Was frueher eine
 * Modulgrenze beim Kompilieren verhindert hat, verhindert jetzt nur noch dieser Test. Er ist
 * damit kein Beiwerk, sondern die Stelle, an der die Schichtung der Workbench festgehalten ist.
 * <p>
 * Faellt ein Test, sind die zugehoerigen Verstoesse in der Test-Ausgabe mit Klassenname und
 * Begruendung gelistet.
 */
@AnalyzeClasses(packages = "io.softwareecg.awb",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class LayerArchitectureTest {

    /**
     * R3 (BL-Reinheit): Business-Logic-Pakete duerfen kein JavaFX importieren.
     * JavaFX gehoert ausschliesslich in UI-Pakete.
     */
    @ArchTest
    static final ArchRule domain_and_service_code_must_not_depend_on_javafx =
            noClasses().that().resideInAnyPackage(
                            "io.softwareecg.awb.datasource.model..",
                            "io.softwareecg.awb.investigation.model..",
                            "io.softwareecg.awb.metadata.model..",
                            "io.softwareecg.awb.projectcontext.model..",
                            "io.softwareecg.awb.repository.model..",
                            "io.softwareecg.awb.timeseries..",
                            "io.softwareecg.awb..port..",
                            "io.softwareecg.awb.queryengine..")
                    .should().dependOnClassesThat().resideInAnyPackage("javafx..")
                    .because("BL darf nicht auf JavaFX hoeren - JavaFX nur in UI-Paketen");

    /**
     * R2 (API-Reinheit): Die designierten API-Pakete duerfen nicht auf BL- oder UI-Code hoeren.
     *
     * Die Regel benennt die Pakete explizit. Pfade wie "..ui.api.." oder "..bl.api.." sind
     * KEINE API-Pakete - das sind Unterpakete innerhalb von BL/UI, die historisch ".api" im
     * Namen haben (Naming-Schuld).
     */
    @ArchTest
    static final ArchRule api_packages_must_not_depend_on_bl_or_ui =
            noClasses().that().resideInAnyPackage(
                            "io.softwareecg.awb.application.api..",
                            "io.softwareecg.awb.repository.api..",
                            "io.softwareecg.awb.projectcontext.api..",
                            "io.softwareecg.awb.investigation.api..",
                            "io.softwareecg.awb.timeseries.port..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.softwareecg.awb..bl..",
                            "io.softwareecg.awb..ui..")
                    .because("API darf nicht auf BL/UI hoeren - sonst zyklische oder verkehrte Schichtung");

    /**
     * R6 (Application-Bundle frei von Feature-Zugriffen): Features duerfen nur ueber
     * application.api einklinken; application.base.impl ist kein oeffentlicher Hook-Punkt.
     */
    @ArchTest
    static final ArchRule features_must_not_depend_on_application_internals =
            noClasses().that().resideOutsideOfPackages("io.softwareecg.awb.application..")
                    // the generated injection module wires every bean and so knows all of them
                    .and().areNotAssignableTo(io.avaje.inject.spi.AvajeModule.class)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "io.softwareecg.awb.application.about..",
                            "io.softwareecg.awb.application.composition..")
                    .because("Features muessen via application.api einklinken, nicht via Application-Interna");

    /**
     * R5 (DA-Reinheit): DA-Adapter duerfen kein JavaFX nutzen.
     */
    @ArchTest
    static final ArchRule persistence_adapters_must_not_depend_on_javafx =
            noClasses().that().resideInAnyPackage(
                            "io.softwareecg.awb.metadata.adapter..",
                            "io.softwareecg.awb.timeseries.adapter..")
                    .should().dependOnClassesThat().resideInAnyPackage("javafx..")
                    .because("DA-Adapter sind reine Persistenz-Schichten und duerfen nicht JavaFX nutzen");

    /**
     * R7 (DA unten): DA-Adapter kennen weder die Oberflaeche noch die Logik der Features.
     * Seit alles gemeinsam kompiliert wird, waere ein solcher Zugriff ohne diese Regel moeglich.
     */
    @ArchTest
    static final ArchRule persistence_adapters_must_not_depend_on_ui =
            noClasses().that().resideInAnyPackage(
                            "io.softwareecg.awb.metadata.adapter..",
                            "io.softwareecg.awb.timeseries.adapter..")
                    .should().dependOnClassesThat().resideInAPackage("io.softwareecg.awb..ui..")
                    .because("Persistenz darf nicht auf die Oberflaeche hoeren");

    /**
     * R12 (Avaje-Migration, Backslide-Schutz): Produktivcode darf nicht mehr
     * auf CDI-spezifische Typen hoeren. Avaje nutzt nur {@code jakarta.inject}
     * (JSR-330); CDI-APIs bleiben aus dem AWB-Code raus.
     */
    @ArchTest
    static final ArchRule code_must_not_depend_on_cdi =
            noClasses().that().resideInAPackage("io.softwareecg.awb..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.enterprise..")
                    .because("AWB-Code muss frei von CDI-spezifischen Typen sein "
                            + "(jakarta.inject reicht; jakarta.enterprise ist CDI-only)");

    /**
     * Die Pakete direkt unter {@code io.softwareecg.awb} sind die Komponenten der Workbench. Sie
     * bilden Schichten: keine zwei duerfen, auch nicht ueber andere, voneinander abhaengen.
     */
    @ArchTest
    static final ArchRule components_must_be_free_of_cycles =
            slices().matching("io.softwareecg.awb.(*)..").should().beFreeOfCycles()
                    .because("Komponenten, die sich gegenseitig brauchen, sind in Wahrheit eine");

    @ArchTest
    static final ArchRule metadata_clients_must_use_ports_instead_of_h2 =
            noClasses().that().resideOutsideOfPackages(
                            "io.softwareecg.awb.metadata.adapter.h2..",
                            "io.softwareecg.awb.application.composition..")
                    .and().areNotAssignableTo(io.avaje.inject.spi.AvajeModule.class)
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.softwareecg.awb.metadata.adapter.h2..")
                    .because("H2 ist ein austauschbarer Adapter; Nutzer muessen metadata.port verwenden");

    @ArchTest
    static final ArchRule repository_ui_must_not_know_repository_adapters =
            noClasses().that().resideInAPackage("io.softwareecg.awb.repository.ui..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.softwareecg.awb.timeseries.adapter..")
                    .because("Repository-Typen werden ueber TimeSeriesRepositoryProvider beschrieben");

    @ArchTest
    static final ArchRule datasource_spi_must_not_know_ui_or_adapters =
            noClasses().that().resideInAPackage("io.softwareecg.awb.datasource.spi..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.softwareecg.awb.datasource.ui..",
                            "io.softwareecg.awb.datasource.adapter..")
                    .because("New-Analysis-Erweiterungen beginnen an einem stabilen, UI-neutralen Vertrag");

    @ArchTest
    static final ArchRule metric_view_must_use_chart_and_query_editor_ports =
            noClasses().that().resideInAPackage("io.softwareecg.awb.metricview..")
                    .and().resideOutsideOfPackage("io.softwareecg.awb.metricview.queryeditor..")
                    .should().dependOnClassesThat().haveFullyQualifiedName(
                            "io.softwareecg.awb.metricview.queryeditor.ChartHeaderController")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(
                            "io.softwareecg.awb.uitoolkit.chart.legend.StackedChartLegend")
                    .because("MetricView koordiniert Header und Legende nur ueber deren Ports");
}
