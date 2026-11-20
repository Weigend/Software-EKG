//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.importer.csv.fileutil;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Helper class for file access.
 *
 */
public final class FileUtil {

    /**
     * Should not be instantiated.
     */
    private FileUtil() {
    }

    /**
     * Find all files of a given directory and subdirectories by a given extension.
     *
     * @param dir       the root dir.
     * @param extension the file extension to find for.
     * @return all csv files of the specified dir.
     */
    public static List<File> findFiles(File dir, final String extension) {
        if (!dir.isDirectory()) {
            List<File> result = new ArrayList<>();
            result.add(dir);
            return result;
        }

        // FileFilter Lambda-Expression
        File[] files = dir.listFiles(pathname -> pathname.isDirectory() || pathname.getName().endsWith(extension));

        List<File> result = new ArrayList<>();
        if (files == null) {
            return new ArrayList<>();
        }
        for (File file : files) {
            if (file.isDirectory()) {
                List<File> subFiles = findFiles(file, extension);
                result.addAll(subFiles);
            } else {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Used for uncompressing a file.
     *
     * @param file a file (.gz)
     * @return a file which points to the uncompressed data.
     * @throws IOException error when working with Streams
     */
    public static File unCompressFile(File file) throws IOException {

        File temp = File.createTempFile("ekg", "ekg");

        try (FileOutputStream fileOutStream = new FileOutputStream(temp);
             GZIPInputStream gzipInStream = new GZIPInputStream(new FileInputStream(file))) {

            IOUtils.copy(gzipInStream, fileOutStream);
        }

        // remove .gz
        String absolutePath = file.getAbsolutePath();
        String uncompressedFileName = absolutePath.substring(0, absolutePath.length() - ".gz".length());

        try (FileInputStream tempInputStream = new FileInputStream(temp);
             FileOutputStream fileOutStream = new FileOutputStream(uncompressedFileName)) {

            IOUtils.copy(tempInputStream, fileOutStream);
        }

        return new File(uncompressedFileName);
    }
}