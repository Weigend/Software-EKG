//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Weigend AM
//       creation date: 2009 - 2013
//       changed by:    $Author$
//       change date:   $Date$
//       revision:      $Revision$
//       description:   Collector UI
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.ui.util;

import io.softwareecg.collector.ui.controller.StartViewController;
import javafx.scene.Parent;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class StyleSwitcher {

    public enum Style {

        main

    }

    private static Style currentStyle = Style.main;

    public static void setCssFile(Parent parentNode) {
        parentNode.getStylesheets().clear();
        parentNode.getStylesheets().add("/io/softwareecg/collector/ui/res/css/" + currentStyle.name() + ".css");
    }

    public static void changeStyle(Style style) {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Desing zu {0} geaendert", style.name());
        StyleSwitcher.currentStyle = style;

    }

    public static Style getcurrentStyle() {
        return currentStyle;
    }
}
