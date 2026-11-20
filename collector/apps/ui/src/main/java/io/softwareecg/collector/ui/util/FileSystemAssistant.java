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
import io.softwareecg.collector.ui.controls.ListItemFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public abstract class FileSystemAssistant {

    /**
     * Determines whether the running OS is unix-based
     */
    protected static final boolean isUnix = !System.getProperty("os.name").contains("Windows");
    protected static final String fileSeperator = System.getProperty("file.separator");

    /**
     * @return the isUnix
     */
    public static boolean isUnix() {
        return isUnix;
    }

    /**
     * Current position in the file system
     */
    private File currentFile;

    /**
     * Gets the current file-object
     *
     * @return the file
     */
    protected File getCurrentFile() {
        return currentFile;
    }

    /**
     * Sets the parameter file as current file
     *
     * @param file File, which is to be managed by the Object
     */
    protected void setFile(File file) {
        currentFile = file;
    }

    /**
     * Creates a new instance of FileSystemAssistant
     */
    public FileSystemAssistant() {

        if (isUnix) {
            currentFile = new File("/media/");
        } else {
            currentFile = new File("R:\\codebase\\collector\\collector\\collector-5.1\\");
        }
    }

    /**
     * Constructor for child classes
     */
    public FileSystemAssistant(String path) {
        currentFile = new File(path);
        if (!currentFile.exists()) {
            throw new RuntimeException("File/Dir at: " + path + " must exist.");
        }
    }

    /**
     * Accepts a child-name as parameter and sets this child as new File if its
     * correct
     *
     * @param clicked A child of the current path
     */
    public void setPath(String clicked) {
        if (clicked.equals("...")) {
            if (!isRoot()) {
                goUp();
            }
            return;
        }
        File selectedFile = new File(getCurrentFile(), clicked);
        if (!selectedFile.exists()) {
            throw new RuntimeException("File does not exist");
        } else if (!selectedFile.canRead()) {
            throw new RuntimeException("Cannot read file");
        } else if (selectedFile.isDirectory() || selectedFile.isFile()) {
            setFile(selectedFile);
        }
    }

    /**
     * Determines files and folders of the current directory
     *
     * @return A list of Items
     */
    public ObservableList<HBox> getFiles() {
        ObservableList<HBox> list = FXCollections.observableArrayList();
        for (File element : getFilteredFileChildren()) {
            if (element.isHidden()) {
            } else if (element.isDirectory()) {
                long size = FileUtils.sizeOfDirectory(element);
                list.add(ListItemFactory.getHBoxItem(element.getName(),
                        getSizeString(size), true));
            } else {
                list.add(ListItemFactory.getHBoxItem(element.getName(),
                        getSizeString(element.length()), false));
            }
        }
        Logger.getLogger(StartViewController.class
                .getName()).log(Level.INFO, "Aktueller Pfad:{0}", currentFile.getPath());
        if (!isRoot()) {
            list.add(0, ListItemFactory.getHBoxItem("...", "", true));
        }

        return list;
    }

    /**
     * As only some files and folders are relevant, the list of files gets
     * filtered
     *
     * @return a filtered list of elements
     */
    protected abstract File[] getFilteredFileChildren();

    /**
     * Replaces the current Fileobject with a new one pointing to its parent
     */
    public void goUp() {
        currentFile = new File(currentFile.getParent());
    }

    /**
     * @return true if root or /media
     */
    public boolean isRoot() {
        //TODO reimplement this method
        return currentFile.getParent() == null || currentFile.getAbsolutePath().equals("/media") || currentFile.getAbsolutePath().equals("R:\\codebase\\collector\\collector\\collector-5.1");
    }

    /**
     * Not used because its not platform independent
     *
     * @param dir
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private long theUnixCommand(String dir) throws IOException, InterruptedException {
        // du -c -h /home/pi/ | grep total

        StringBuilder sb = new StringBuilder();

        String[] cmd = {
                "/bin/sh",
                "-c",
                "du -c -h " + dir + " | grep total"
        };

        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        String result = sb.toString();
        result = result.split("\t")[0];

        return Long.valueOf(result);
    }

    /**
     * Determines the appropriate filesize and returns it in a String with the
     * appropriate unit
     *
     * @param zahl Size of the file
     * @return Filesize in an appropiate unit
     */
    private String getSizeString(long zahl) {
        // System.out.println(zahl);
        if (zahl < 1024) {
            return zahl + "B";
        } else if (zahl < 1024 * 1024) {
            return zahl / 1024 + "KB";
        } else {
            return zahl / 1024 / 1024 + "MB";
        }
    }

    /**
     * Returns if the current path can be the destination path
     *
     * @return
     */
    public abstract boolean isFileSystemDestinationReached();

    /**
     * Determines the view which should be called next
     *
     * @return Filename of the view which is to be called
     */
    public abstract String getTarget();

    /**
     * Executes the file operation
     *
     * @throws IOException If the reading/writing of the file fails
     */
    public abstract void confirmFileOperation() throws IOException;

    @Override
    public String toString() {
        return currentFile.toString();
    }
}
