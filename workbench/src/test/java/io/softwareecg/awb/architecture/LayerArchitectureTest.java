package io.softwareecg.awb.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.function.BiPredicate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
                            "io.softwareecg.awb.metadata.model..",
                            "io.softwareecg.awb.projectcontext.model..",
                            "io.softwareecg.awb.repository.model..",
                            "io.softwareecg.awb.repository.schema..",
                            "io.softwareecg.awb.repository.spi..",
                            "io.softwareecg.awb.timeseries..",
                            "io.softwareecg.awb..port..",
                            "io.softwareecg.awb.query..")
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
                            "io.softwareecg.awb.query.api..",
                            "io.softwareecg.awb.repository.port..")
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
                            "io.softwareecg.awb.repository.adapter..")
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
                            "io.softwareecg.awb.repository.adapter..")
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

    /**
     * Alles zu einer Repository-Technologie liegt in einem Paket unter repository.adapter.
     */
    @ArchTest
    static final ArchRule repository_technologies_must_not_know_each_other =
            slices().matching("io.softwareecg.awb.repository.adapter.(*)..").should().notDependOnEachOther()
                    .because("eine Technologie muss sich hinzufuegen und entfernen lassen, ohne eine andere anzufassen");

    /**
     * api: von anderen aufgerufen, von der eigenen Komponente implementiert. Ein Proxy reicht nur
     * weiter und implementiert die Schnittstelle deshalb nicht im eigentlichen Sinn.
     */
    @ArchTest
    static final ArchRule an_api_is_implemented_by_its_own_component =
            classes().that().areInterfaces().and().resideInAPackage("io.softwareecg.awb.*.api..")
                    .should(beImplementedOnlyIn("der eigenen Komponente", (type, implementation) ->
                            componentOf(implementation).equals(componentOf(type))
                                    || implementation.getSimpleName().endsWith("Proxy")))
                    .because("was andere Komponenten implementieren, gehoert in spi");

    /**
     * spi: von anderen oder von Erweiterungen implementiert. Hier steht nur der Vertrag.
     */
    @ArchTest
    static final ArchRule an_spi_holds_contracts_only =
            classes().that().resideInAPackage("io.softwareecg.awb.*.spi..").and().areTopLevelClasses()
                    .should().beInterfaces().orShould().beRecords().orShould().beEnums()
                    .because("Implementierungen und Registries gehoeren nicht in spi");

    /**
     * port: die Schnittstelle zu einer Technik, implementiert von den Adaptern der Komponente.
     */
    @ArchTest
    static final ArchRule a_port_is_implemented_by_adapters =
            classes().that().areInterfaces().and().resideInAPackage("io.softwareecg.awb.*.port")
                    .should(beImplementedOnlyIn("den Adaptern der eigenen Komponente", (type, implementation) ->
                            implementation.getPackageName().startsWith(
                                    "io.softwareecg.awb." + componentOf(type) + ".adapter")))
                    .because("ein port trennt die Komponente von einer Technik");

    private static ArchCondition<JavaClass> beImplementedOnlyIn(String place, BiPredicate<JavaClass, JavaClass> allowed) {
        return new ArchCondition<>("nur implementiert sein in " + place) {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                for (JavaClass implementation : type.getAllSubclasses()) {
                    if (!allowed.test(type, implementation)) {
                        events.add(SimpleConditionEvent.violated(type,
                                type.getName() + " wird implementiert von " + implementation.getName()));
                    }
                }
            }
        };
    }

    private static String componentOf(JavaClass type) {
        String rest = type.getPackageName().substring("io.softwareecg.awb".length());
        return rest.isEmpty() ? "" : rest.substring(1).split("\\.")[0];
    }

    /**
     * Wie ein Repository verbunden ist - Adresse, Zugangsdaten, Client - steht in EkgRepository und
     * bleibt in der Komponente. Die anderen benutzen Repository. datasource sucht noch selbst im
     * eingebetteten Repository, wo eine Serie endet.
     */
    @ArchTest
    static final ArchRule only_repository_knows_how_a_repository_is_connected =
            noClasses().that().resideOutsideOfPackages(
                            "io.softwareecg.awb.repository..",
                            "io.softwareecg.awb.datasource")
                    .and().doNotHaveSimpleName("RepositorySeriesEnds")
                    .and().areNotAssignableTo(io.avaje.inject.spi.AvajeModule.class)
                    .should().dependOnClassesThat().haveFullyQualifiedName(
                            "io.softwareecg.awb.repository.api.EkgRepository")
                    .because("Ansichten brauchen einen Zugang zum Repository, nicht seine Verbindung");

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
                    .resideInAPackage("io.softwareecg.awb.repository.adapter..")
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
