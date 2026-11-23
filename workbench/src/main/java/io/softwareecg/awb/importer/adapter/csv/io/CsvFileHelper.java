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
package io.softwareecg.awb.importer.adapter.csv.io;

import io.softwareecg.awb.runtime.logging.EkgLogger;
import org.slf4j.Logger;

import java.io.*;

/**
 * Helper functions for csv access.
 *
 */
public final class CsvFileHelper {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = EkgLogger.get();

    /**
     * Should not be instantiated.
     */
    private CsvFileHelper() {
    }

    /**
     * Helper returns the last line of a csv file.
     * We don't want to scan the file from the front -
     * if we have to scan big files, this would kill
     * the performance.
     * Note:
     * We scan the last complete line,
     * which consists of the CSV Columns of the file
     * AND(!) one line-break.
     * Formal: {csv-column} \n
     *
     * @param file the file.
     * @return the last line or null if the file is empty
     */
    public static String getLastLine(File file) {

        try (RandomAccessFile fileHandler = new RandomAccessFile(file, "r")) {
            long fileLength = fileHandler.length();
            
            // Handle empty files
            if (fileLength == 0) {
                return null;
            }
            
            fileLength--; // Start from the last byte
            StringBuilder sb = new StringBuilder();

            for (long filePointer = fileLength; filePointer != -1; filePointer--) {
                fileHandler.seek(filePointer);
                int readByte = fileHandler.readByte();

                if (readByte == 0xA) {
                    if (filePointer == fileLength) {
                        continue;
                    }
                    break;
                } else if (readByte == 0xD) {
                    if (filePointer == fileLength - 1) {
                        continue;
                    }
                    break;
                }

                sb.append((char) readByte);
            }

            String result = sb.reverse().toString();
            return result.isEmpty() ? null : result;
        } catch (IOException e) {
            LOGGER.warn("Cannot read last line of file: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * Gets the first line.
     *
     * @param file the file
     * @return the first line
     */
    public static String getFirstLine(File file) {
        String result = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
             result = reader.readLine();
        } catch (IOException e) {
            LOGGER.warn("Can not get first line of file.", e);
        }
        return result;
    }
}
