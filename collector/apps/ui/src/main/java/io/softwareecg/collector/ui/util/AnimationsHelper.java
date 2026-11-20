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

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.util.Duration;

/**
 */
public class AnimationsHelper {

    /**
     * Int to set the duration of the animations in ms
     */
    private static final int duration = 200;

    /**
     * @param parent Needs A Node witch will be animatieted
     * @param event  What to do after the animation is finished
     */
    public static void animation(Parent parent, EventHandler<ActionEvent> event) {
        parent.setCacheHint(CacheHint.SPEED);
        FadeTransition fade = new FadeTransition(Duration.millis(duration), parent);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(event);
        fade.play();
    }

    /**
     * @param parent Needs A Node witch will be animatieted
     */
    public static void animation(Parent parent) {
        parent.setCacheHint(CacheHint.SPEED);
        FadeTransition fade = new FadeTransition(Duration.millis(duration), parent);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}
