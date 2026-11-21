package io.softwareecg.awb.application.api;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

/**
 * Hook-Interface, ueber das Features in das Application-Shell einklinken.
 *
 * Features holen die Implementierung via {@code Lookup.lookup(EkgApplicationController.class)}
 * und registrieren ihre UI-Bausteine. Die konkrete Implementierung lebt in
 * awb-application-base; Features muessen das Bundle selbst nicht kennen.
 */
public interface EkgApplicationController {

    /**
     * Haengt einen Node oben in den Shell-Container (ueber dem Fenster-Bereich,
     * unterhalb der Menubar). Wird z.B. von der ProjectBar genutzt.
     */
    void addTopNode(Node node);

    /**
     * Fuegt ein MenuItem ins Analysis-Menue ein, oberhalb des "Exit"-Eintrags.
     */
    void addAnalysisMenuItem(MenuItem menuItem);

    /**
     * Fuegt ein MenuItem ans Ende des Repository-Menues an.
     */
    void addRepositoryMenuItem(MenuItem menuItem);

    /**
     * Adds a MenuItem at the top of the Repository menu: for what manages the repositories
     * themselves, which comes before what is done with one of them, whichever module starts first.
     */
    void addRepositoryMenuItemFirst(MenuItem menuItem);

    /**
     * Fuegt ein MenuItem ins Window-Menue ein, oberhalb von "Restore Default Layout".
     */
    void addWindowMenuItem(MenuItem menuItem);

    /**
     * Liefert das Haupt-Stage des Application-Shells, z.B. um eigene Dialoge per
     * {@code initOwner(...)} korrekt zu verankern.
     */
    Stage getStage();
}
