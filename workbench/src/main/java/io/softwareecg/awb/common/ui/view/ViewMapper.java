//______________________________________________________________________________
//
//                  ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.common.ui.view;

import io.softwareecg.wfx.windowmanager.api.FXMLView;
import io.softwareecg.wfx.windowmanager.api.Position;
import io.softwareecg.wfx.windowmanager.api.ViewKind;

/**
 * Mapping of the EkgView to FXMLView
 *
 * @param <CONTROLLER> controller
 */
public class ViewMapper<CONTROLLER> {

    /**
     * Converts a EkgView to a FXMLView, taking the viewAreaSize from the EkgView.
     *
     * @param ekgView ekg view
     * @param position position for fxml view
     * @return fxml view
     */
    public FXMLView<CONTROLLER> convert(EkgView<CONTROLLER> ekgView, Position position) {
        return convert(ekgView, position, ViewKind.TOOL);
    }

    /**
     * Converts a EkgView to a FXMLView of the given kind.
     *
     * @param ekgView ekg view
     * @param position position for fxml view
     * @param kind whether the view is a lasting panel of the application or a document that is
     *             bound to its content and gone for good once its tab is closed
     * @return fxml view
     */
    public FXMLView<CONTROLLER> convert(EkgView<CONTROLLER> ekgView, Position position, ViewKind kind) {
        return new FXMLView.Builder<CONTROLLER>()
                .withKind(kind)
                .withId(ekgView.getViewId())
                .withTitle(ekgView.getTitle())
                .withPos(position)
                .withToolTipInfo(ekgView.getToolTipInfo())
                .withViewAreaSize(ekgView.getViewAreaSize())
                .withViewImage(ekgView.getViewImagePath())
                .withRootPane(ekgView.getRootNode())
                .withController(ekgView.getController())
                .clone();
    }
}
