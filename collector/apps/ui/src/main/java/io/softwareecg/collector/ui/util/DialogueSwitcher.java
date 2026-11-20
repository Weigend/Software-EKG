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

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class DialogueSwitcher {

    /**
     * Path for xml files
     */
    public static final String FXML_LOCATION = "/io/softwareecg/collector/ui/res/fxml/";

    /**
     * Loads an object hierarchy from a xml-file
     *
     * @param clazz Class used to get the resource
     * @param fxml  Filename
     * @return The loaded object hierarchy
     */
    public static Parent getParent(Class clazz, String fxml) {

        try {
            URL url = clazz.getResource(fxml);
            return FXMLLoader.load(url);
        } catch (IOException ex) {
            Logger.getLogger(DialogueSwitcher.class.getName()).log(Level.SEVERE, "IOEXception caught, class:" + clazz, ex);
            System.err.println(fxml + "Klasse: " + clazz);
        } catch (NullPointerException e) {
            Logger.getLogger(DialogueSwitcher.class.getName()).log(Level.SEVERE, "NullPointerException aber richtig", e);
            System.err.println(fxml + "Klasse: " + clazz);
        }

        return null;
    }

    /**
     * @param clazz
     * @param fxml
     * @param res   A ResourceBundle which is passed to the new controller
     * @return
     */
    public static Parent getParent(Class clazz, String fxml, ResourceBundle res) {
        try {
            URL url = clazz.getResource(fxml);
            return FXMLLoader.load(url, res);
        } catch (IOException ex) {
            Logger.getLogger(DialogueSwitcher.class.getName()).log(Level.SEVERE, "IOEXception caught, class:" + clazz + " fxml: " + fxml, ex);
        } catch (NullPointerException e) {
            Logger.getLogger(DialogueSwitcher.class.getName()).log(Level.SEVERE, "NullPointerException aber richtig " + e.getMessage(), e);
        }
        return null;
    }
}
