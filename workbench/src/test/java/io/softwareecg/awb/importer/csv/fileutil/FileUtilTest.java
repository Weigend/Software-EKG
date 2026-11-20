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

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Class FileUtilTest.
 *
 */
public class FileUtilTest {

    /**
     * Accesses the class' resources and tries to return a file. Although asserts that the file exists.
     *
     * @param resourceString the path in the resource folder
     * @return a File
     */
    private File getFileFromResource(String resourceString) {
        URL url = getClass().getResource("FileUtilTestFolders/" + resourceString);
        File file = new File(url.getFile());
        assertTrue(file.exists());

        return file;
    }

    /**
     * Test of findFiles with no valid extension.
     */
    @Test
    public void testFindFilesWithNoValidExtension() {
        File dir = getFileFromResource("");
        int numberOfFilesInDir = FileUtil.findFiles(dir, "NO_VALID_EXTENSION").size();

        // There is no file with that extension
        assertEquals(0, numberOfFilesInDir);
    }

    /**
     * Test of findFiles with the test extension.
     */
    @Test
    public void testFindFilesTestExtension() {
        File dir = getFileFromResource("");
        int numberOfFilesInDir = FileUtil.findFiles(dir, "test").size();

        // all files in all sub folders with the extension test
        assertEquals(5, numberOfFilesInDir);
    }

    /**
     * Test of findFiles with the no extension (so sub folders must be looked at as well).
     */
    @Test
    public void testFindFilesWithoutExtension() {
        File dir = getFileFromResource("");
        int numberOfFilesInDir = FileUtil.findFiles(dir, "").size();

        // Including sub folder and files in that folder
        assertEquals(7, numberOfFilesInDir);
    }

    /**
     * Test of findFiles with a file instead of folder.
     */
    @Test
    public void testFindFilesWithoutFolder() {
        File dir = getFileFromResource("FileUtilTest_file1.test");
        int numberOfFilesInDir = FileUtil.findFiles(dir, "").size();

        assertEquals(1, numberOfFilesInDir);
    }

    /**
     * Tests the unCompressFile method
     */
    @Test
    public void testUnCompressFile() throws IOException {
        File compressed = getFileFromResource("FileUtilTest_compressed.gz");
        File uncompressed = FileUtil.unCompressFile(compressed);

        assertTrue(uncompressed.exists());
        assertEquals("FileUtilTest_compressed", uncompressed.getName());
    }

    /**
     * Test that the constructor is private.
     *
     * @throws Exception due to reflection there are some exceptions possible
     */
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<FileUtil> constructor = FileUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
