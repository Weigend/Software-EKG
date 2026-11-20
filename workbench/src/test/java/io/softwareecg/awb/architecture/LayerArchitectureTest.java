package io.softwareecg.awb.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
    static final ArchRule bl_must_not_depend_on_javafx =
            noClasses().that().resideInAPackage("..bl..")
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
                            "io.softwareecg.awb.da.api..",
                            "io.softwareecg.awb.repository.api..",
                            "io.softwareecg.awb.project.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.softwareecg.awb..bl..",
                            "io.softwareecg.awb..ui..")
                    .because("API darf nicht auf BL/UI hoeren - sonst zyklische oder verkehrte Schichtung");

    /**
     * R6 (Application-Bundle frei von Feature-Zugriffen): Features duerfen nur ueber
     * application.api einklinken; application.base.impl ist kein oeffentlicher Hook-Punkt.
     */
    @ArchTest
    static final ArchRule features_must_not_depend_on_application_base_impl =
            noClasses().that().resideOutsideOfPackages("io.softwareecg.awb.application..")
                    // the generated injection module wires every bean and so knows all of them
                    .and().areNotAssignableTo(io.avaje.inject.spi.AvajeModule.class)
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.softwareecg.awb.application.base.impl..")
                    .because("Features muessen via application.api einklinken, nicht via base.impl");

    /**
     * R5 (DA-Reinheit): DA-Adapter duerfen kein JavaFX nutzen.
     */
    @ArchTest
    static final ArchRule da_must_not_depend_on_javafx =
            noClasses().that().resideInAPackage("io.softwareecg.awb.da..")
                    .should().dependOnClassesThat().resideInAnyPackage("javafx..")
                    .because("DA-Adapter sind reine Persistenz-Schichten und duerfen nicht JavaFX nutzen");

    /**
     * R7 (DA unten): DA-Adapter kennen weder die Oberflaeche noch die Logik der Features.
     * Seit alles gemeinsam kompiliert wird, waere ein solcher Zugriff ohne diese Regel moeglich.
     */
    @ArchTest
    static final ArchRule da_must_not_depend_on_ui =
            noClasses().that().resideInAPackage("io.softwareecg.awb.da..")
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
}
