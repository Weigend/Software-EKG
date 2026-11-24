package io.softwareecg.awb.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
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
                            "io.softwareecg.awb.metadata.api..",
                            "io.softwareecg.awb.projectcontext.api..",
                            "io.softwareecg.awb.projectcontext.store..",
                            "io.softwareecg.awb.repository.connection..",
                            "io.softwareecg.awb.repository.persistence..",
                            "io.softwareecg.awb.repository.series..",
                            "io.softwareecg.awb.timeseries..",
                            "io.softwareecg.awb.query..")
                    // a control of projectcontext that others put into their dialogs
                    .and().doNotHaveFullyQualifiedName("io.softwareecg.awb.projectcontext.api.ProjectSelector")
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
                            "io.softwareecg.awb.repository.persistence..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.softwareecg.awb..bl..",
                            "io.softwareecg.awb..ui..")
                    .because("API darf nicht auf BL/UI hoeren - sonst zyklische oder verkehrte Schichtung");

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
     * api: von anderen aufgerufen, von der eigenen Komponente implementiert.
     */
    @ArchTest
    static final ArchRule an_api_is_implemented_by_its_own_component =
            classes().that().areInterfaces().and().resideInAPackage("io.softwareecg.awb.*.api..")
                    .should(beImplementedOnlyIn("der eigenen Komponente", (type, implementation) ->
                            componentOf(implementation).equals(componentOf(type))))
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
     * api und spi kommen mit sich selbst aus: wer sie benutzt, haengt sonst auch an der
     * Implementierung dahinter. Meist ist es ein Parameter oder ein Rueckgabewert, der sie verraet.
     */
    @ArchTest
    static final ArchRule an_api_or_spi_does_not_expose_the_implementation =
            classes().that().resideInAnyPackage("io.softwareecg.awb.*.api..", "io.softwareecg.awb.*.spi..")
                    .should(dependOnNothingInsideTheirComponent())
                    .because("was innen ist, muss sich aendern lassen, ohne die anderen anzufassen");

    private static ArchCondition<JavaClass> dependOnNothingInsideTheirComponent() {
        return new ArchCondition<>("in ihrer Komponente nur api und spi kennen") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                for (Dependency dependency : type.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    if (target.getPackageName().startsWith("io.softwareecg.awb")
                            && componentOf(target).equals(componentOf(type))
                            && !resideInAnyPackage("io.softwareecg.awb.*.api..", "io.softwareecg.awb.*.spi..").test(target)) {
                        events.add(SimpleConditionEvent.violated(type, dependency.getDescription()));
                    }
                }
            }
        };
    }

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
     * Die Aussenkante von repository ist api. Alles andere - Verbindung, Speicher, Suche, Techniken -
     * bleibt in der Komponente. Die zwei Bedienelemente, die andere in ihre Dialoge setzen, kennen
     * sie als Schnittstelle.
     */
    @ArchTest
    static final ArchRule only_the_api_of_repository_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("repository");

    /**
     * Die Aussenkante von bookmark ist api. Wie ein Bookmark gespeichert ist und die Dialoge bleiben
     * in der Komponente.
     */
    @ArchTest
    static final ArchRule only_the_api_of_bookmark_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("bookmark");

    /**
     * Die Aussenkante von query ist api, und spi fuer den, der die Werte entfernter Projekte liefert.
     * Wie eine Abfrage laeuft und die Serien umgeformt werden, bleibt in der Komponente.
     */
    @ArchTest
    static final ArchRule only_the_api_of_query_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("query");

    /**
     * Die Aussenkante von evidence ist api. Dialoge und Dateiformate bleiben in der Komponente.
     */
    @ArchTest
    static final ArchRule only_the_api_of_evidence_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("evidence");

    /**
     * Die Aussenkante von secrets ist api. Wo die Geheimnisse liegen, bleibt in der Komponente.
     */
    @ArchTest
    static final ArchRule only_the_api_of_secrets_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("secrets");

    /**
     * Die Aussenkante von metricview ist api. Wie ein Diagramm gesteuert wird, bleibt in der Komponente.
     */
    @ArchTest
    static final ArchRule only_the_api_of_metricview_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("metricview");

    /**
     * Die Aussenkante von metadata ist api. H2 ist ein austauschbarer Adapter.
     */
    @ArchTest
    static final ArchRule only_the_api_of_metadata_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("metadata");

    /**
     * Die Aussenkante von projectcontext ist api, und spi fuer Importer und Datenquellen.
     */
    @ArchTest
    static final ArchRule only_the_api_of_projectcontext_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("projectcontext");

    /**
     * Wer sich in das Fenster der Anwendung einklinkt, tut es ueber api.
     */
    @ArchTest
    static final ArchRule only_the_api_of_application_is_used_from_outside =
            onlyTheApiIsUsedFromOutside("application");

    /**
     * Diese Komponenten hat niemand zu benutzen: sie haben eine Oberflaeche und hoeren auf Ereignisse.
     * Was sie an Vertraegen fuer Erweiterungen haben, steht in spi.
     */
    @ArchTest
    static final ArchRule explorer_is_not_used_from_outside = onlyTheApiIsUsedFromOutside("explorer");

    @ArchTest
    static final ArchRule importer_is_not_used_from_outside = onlyTheApiIsUsedFromOutside("importer");

    @ArchTest
    static final ArchRule datasource_is_not_used_from_outside = onlyTheApiIsUsedFromOutside("datasource");

    @ArchTest
    static final ArchRule aichat_is_not_used_from_outside = onlyTheApiIsUsedFromOutside("aichat");

    /**
     * In keinem Paket brauchen sich seine Unterpakete und seine Klassen gegenseitig im Kreis. Ein
     * Kreis aus lauter Klassen eines Pakets ist erlaubt: sie gehoeren zusammen.
     */
    @ArchTest
    static void packages_must_be_free_of_cycles_on_every_level(JavaClasses classes) {
        Map<String, Map<String, Set<String>>> graphs = new TreeMap<>();
        for (JavaClass type : classes) {
            if (belongTo(assignableTo(io.avaje.inject.spi.AvajeModule.class)).test(type)) {
                continue;
            }
            for (Dependency dependency : type.getDirectDependenciesFromSelf()) {
                addEdge(graphs, topLevel(type), topLevel(dependency.getTargetClass()));
            }
        }
        List<String> cycles = new ArrayList<>();
        graphs.forEach((parent, graph) -> graph.keySet().forEach(node -> {
            // a package, not a class: its name begins with a small letter
            if (Character.isLowerCase(node.charAt(0)) && reaches(graph, node, node, new HashSet<>())) {
                cycles.add(parent + "." + node);
            }
        }));
        if (!cycles.isEmpty()) {
            throw new AssertionError("Pakete, die sich im Kreis brauchen, sind in Wahrheit eines: " + cycles);
        }
    }

    /** Die Abhaengigkeit gehoert zu dem Paket, in dem sich die Wege zu beiden Klassen trennen. */
    private static void addEdge(Map<String, Map<String, Set<String>>> graphs, String from, String to) {
        if (from.equals(to) || !to.startsWith("io.softwareecg.awb.")) {
            return;
        }
        String[] source = from.split("\\.");
        String[] target = to.split("\\.");
        int level = 0;
        while (source[level].equals(target[level])) {
            level++;
        }
        graphs.computeIfAbsent(String.join(".", Arrays.copyOf(source, level)), parent -> new TreeMap<>())
                .computeIfAbsent(source[level], node -> new TreeSet<>()).add(target[level]);
    }

    private static boolean reaches(Map<String, Set<String>> graph, String from, String to, Set<String> seen) {
        for (String next : graph.getOrDefault(from, Set.of())) {
            if (next.equals(to) || seen.add(next) && reaches(graph, next, to, seen)) {
                return true;
            }
        }
        return false;
    }

    private static String topLevel(JavaClass type) {
        return type.getBaseComponentType().getName().replaceAll("\\$.*", "");
    }

    /**
     * api wird von anderen benutzt, spi von anderen implementiert: beides darf von aussen importiert
     * werden, sonst nichts.
     */
    private static ArchRule onlyTheApiIsUsedFromOutside(String component) {
        String root = "io.softwareecg.awb." + component;
        DescribedPredicate<JavaClass> inside = resideInAPackage(root + "..")
                .and(not(resideInAnyPackage(root + ".api..", root + ".spi..")));
        return noClasses().that().resideOutsideOfPackage(root + "..")
                // the generated injection module wires every bean and so knows all of them, as do the
                // classes in it
                .and(not(belongTo(assignableTo(io.avaje.inject.spi.AvajeModule.class))))
                .should().dependOnClassesThat(inside.as("liegen in " + component + ", aber nicht in seiner api oder spi"))
                .because("was innen ist, muss sich aendern lassen, ohne die anderen anzufassen");
    }

    @ArchTest
    static final ArchRule repository_ui_must_not_know_repository_adapters =
            noClasses().that().resideInAPackage("io.softwareecg.awb.repository.ui..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.softwareecg.awb.repository.adapter..")
                    .because("Repository-Typen werden ueber TimeSeriesRepositoryProvider beschrieben");

    /**
     * Jede Art von Datenquelle hat ihr Paket unter datasource.adapter. Was sie gemeinsam haben, steht
     * in model oder im Paket des Collectors.
     */
    @ArchTest
    static final ArchRule kinds_of_data_sources_must_not_know_each_other =
            slices().matching("io.softwareecg.awb.datasource.adapter.(*)..").should().notDependOnEachOther()
                    .ignoreDependency(resideInAPackage("io.softwareecg.awb.datasource.adapter.collector.."), alwaysTrue())
                    .ignoreDependency(alwaysTrue(), resideInAPackage("io.softwareecg.awb.datasource.adapter.collector.."))
                    .because("eine Art von Datenquelle muss sich hinzufuegen und entfernen lassen, ohne eine andere anzufassen");

    @ArchTest
    static final ArchRule evidence_must_not_know_charts =
            noClasses().that().resideInAPackage("io.softwareecg.awb.evidence..")
                    .should().dependOnClassesThat().resideInAPackage("io.softwareecg.awb.uitoolkit.chart..")
                    .because("ein Export braucht ein Bild und Werte, kein Diagramm");

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
