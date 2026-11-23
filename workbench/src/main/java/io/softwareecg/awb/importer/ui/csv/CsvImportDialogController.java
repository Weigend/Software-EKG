//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.importer.ui.csv;


import io.softwareecg.awb.importer.ui.dialogs.fileimport.FileImportDialogController;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML CsvImportDialogController class
 *
 */
public class CsvImportDialogController extends FileImportDialogController {

    //================================================================================================================
    //  Initializable interface implementation
    //================================================================================================================

    /* (non-Javadoc)
     * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        initExtViewComponents();
    }

    //================================================================================================================
    //  helper logic to handle the state/behavior of this component
    //================================================================================================================

    /**
     * Initializes additional view components that are not part of the default FileImportDialog
     * and reconfigure the default view.
     */
    private void initExtViewComponents() {
        defaultFileSelector.setLabelText("Path Collector Logs:");
    }
}
