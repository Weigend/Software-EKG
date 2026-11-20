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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 */
public class ImportAssistant extends FileSystemAssistant {

    /**
     * True if the import was successful
     */
    private boolean isOperationConfirmed;

    /**
     * Creates a new instance of ImportAssistant
     */
    public ImportAssistant() {
        this(getStartDirectory());
    }

    /**
     * Creates a new instance of ImportAssistant
     *
     * @param directory Directory, that will be managed by FileSystemAssistant
     */
    public ImportAssistant(String directory) {
        super(directory);
        isOperationConfirmed = false;
    }

    /**
     * Determines start directory and returns it as String
     * This must be done in order to guarantee platform independence.
     * <p>
     * Currently this is only used for testing convenience
     *
     * @return the start directory
     */
    private static String getStartDirectory() {
        //TODO Insert correct paths
        if (isUnix) {
            return "/media/";
        } else {
            return "R:\\codebase\\collector\\collector\\collector-5.1\\";
        }
    }

    /**
     * @return true if isFile and canRead
     */
    @Override
    public boolean isFileSystemDestinationReached() {
        return getCurrentFile().isFile() && getCurrentFile().canRead();
    }

    @Override
    public String getTarget() {
        if (isOperationConfirmed) {
            return "EinstellungsdialogView.fxml";
        } else if (isFileSystemDestinationReached()) {
            return "ConfirmOperationView.fxml";
        }
        throw new RuntimeException("Cannot return target because its not a possible destination");
    }

    /**
     * Copys the config file into the config direcory
     *
     * @throws java.io.IOException
     */
    @Override
    public void confirmFileOperation() throws IOException {
        //TODO Replace testdirectory with correct directory
        /* This should be used on the final release
         Files.copy(getCurrentFile().toPath(), Paths.get("../config/current/config.yaml"), REPLACE_EXISTING);
         */
        Logger.getLogger(ImportAssistant.class.getName()).log(Level.INFO, getCurrentFile().getAbsolutePath());

        Files.createDirectory(new File(getCurrentFile().getParent() + fileSeperator + "testdir").toPath());
        Files.copy(getCurrentFile().toPath(), new File(getCurrentFile().getParent() + fileSeperator + "testdir" + fileSeperator + "config.yaml").toPath(), REPLACE_EXISTING);
        isOperationConfirmed = true;
    }

    /**
     * As only configuration files and folders are relevant, when importing, the list of files gets filtered
     *
     * @return a filtered list of elements
     */
    @Override
    protected File[] getFilteredFileChildren() {
        File[] content = getCurrentFile().listFiles((File pathname) ->
                pathname.exists() && pathname.canRead() &&
                        (pathname.getName().endsWith(".yaml") || pathname.getName().endsWith(".yml") || pathname.isDirectory()));

        Arrays.sort(content, (File o1, File o2) -> {
            if (o1.isDirectory() == o2.isDirectory()) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return o1.isDirectory() ? -1 : 1;
            }
        });
        return content;
    }

}
