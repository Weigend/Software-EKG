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
package io.softwareecg.collector.common;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class used to archive folders as .zip archives
 *
 */
public final class ZipUtil {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Zip file type
     */
    private static final String ZIP = ".zip";

    /**
     * Utility class.
     */
    private ZipUtil() {

    }

    /**
     * Zip a directory without compression. This is useful if the content of a directory is already compressed.
     * The method is not recursive.
     *
     * @param directory a directory.
     */
    public static void zipDirectoryFastest(File directory) {

        if (!directory.isDirectory()) {
            throw new IllegalStateException("Not a directory: " + directory.getAbsolutePath());
        }

        String nameOfZipFile = directory.getPath() + ZIP;


        try (FileOutputStream fileOutputStream = new FileOutputStream(nameOfZipFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
             // no compression: Only archive
            zipOutputStream.setLevel(ZipOutputStream.STORED);

            File[] files = directory.listFiles();
            if (files == null) {
                throw new IllegalStateException("File.listFiles() returned null for " + directory.getAbsolutePath());
            }
            for (File f : files) {
                if (!f.isFile()) {
                    // not recursive
                    continue;
                }

                try (FileInputStream fileInputStream = new FileInputStream(f)) {
                    zipOutputStream.putNextEntry(new ZipEntry(f.getName()));
                    byte[] buffer = new byte[1024];
                    for (int len = fileInputStream.read(buffer); len != -1; len = fileInputStream.read(buffer)) {
                        zipOutputStream.write(buffer, 0, len);
                    }
                    zipOutputStream.closeEntry();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception while creating .zip archive from folder " + directory.getPath() + " " + e, e);
            throw new IllegalStateException(e);
        }
    }
}
