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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 */
public class ExportAssistant extends FileSystemAssistant {

    private enum ExportProgress {

        SELECTFILE, SELECTDESTINATION, CONFIRM
    }

    private File exportPath;

    private final ExportProgress progress;

    /**
     * Creates a new instance of ExportAssistant
     */
    public ExportAssistant() {
        super();
        progress = ExportProgress.SELECTFILE;
    }

    @Override
    public boolean isFileSystemDestinationReached() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTarget() {
        if (progress.equals(ExportProgress.SELECTFILE) || progress.equals(ExportProgress.SELECTDESTINATION)) {
            return "ExplorerView.fxml";
        } else {
            return "ConfirmOperationView.fxml";
        }
    }

    @Override
    public void confirmFileOperation() throws IOException {
        Logger.getLogger(ImportAssistant.class.getName()).log(Level.INFO, getCurrentFile().getAbsolutePath());
        Files.copy(getCurrentFile().toPath(), exportPath.toPath(), REPLACE_EXISTING);
    }

    @Override
    protected File[] getFilteredFileChildren() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
