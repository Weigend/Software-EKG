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
package io.softwareecg.collector.ui.controls;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 */
public class ListItemFactory extends HBox {

    /**
     * bulids an HBox wich 3 Labels one for the picture and the other two for
     * Name and Space
     *
     * @param name        Name of the left Label
     * @param space       Text for the right Label
     * @param isDirection true if the FILE is an direction
     * @return HBox with Both Labels
     */
    public static HBox getHBoxItem(String name, String space, boolean isDirection) {
        HBox box = new HBox(20);
//Picture selection
        Label img = new Label("O");
        if (isDirection) {
            img.getStyleClass().addAll("folderImage");
        } else {
            img.getStyleClass().addAll("fileImage");
        }

        Label left = new Label(name);
        left.getStyleClass().addAll("label", "list-label");

        Label right = new Label(space);
        right.getStyleClass().addAll("label", "list-label");
        right.setAlignment(Pos.CENTER_RIGHT);

        box.getChildren().addAll(img, left, right);
        return box;
    }

    /**
     * For the style switcher
     *
     * @param text Name of Label
     * @return Lable with text as Name
     */
    public static Label getLabelItem(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("label", "list-label");
        return l;
    }
}
