package io.softwareecg.collector.source.log;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The command a reading over SSH consists of is run here by the shell of this machine, and its
 * answer is read as the session delivers it.
 */
public class SshLogFileReaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path file;

    @Before
    public void createFile() throws IOException {
        Assume.assumeTrue("needs a shell", new File("/bin/sh").canExecute());
        file = folder.newFile("it's an application.log").toPath();   // a name a shell stumbles over
    }

    // ---------------------------------------------------------------------------------------------
    // the command
    // ---------------------------------------------------------------------------------------------

    @Test
    public void theFirstReadingBeginsAtTheEndOrAtTheStart() throws Exception {
        Files.writeString(file, "one\ntwo\n");

        LogFileReader.Part atEnd = read(null, 0, true, 1_000);
        assertEquals(8, atEnd.size());
        assertEquals(8, atEnd.start());
        assertEquals(8, atEnd.end());
        assertEquals("", atEnd.text());
        assertFalse(atEnd.hasMore());

        LogFileReader.Part atStart = read(null, 0, false, 1_000);
        assertEquals(0, atStart.start());
        assertEquals(8, atStart.end());
        assertEquals("one\ntwo\n", atStart.text());
        assertEquals("the same file", atEnd.fileKey(), atStart.fileKey());
    }

    @Test
    public void aReadingContinuesWhereTheLastOneEnded() throws Exception {
        Files.writeString(file, "one\ntwo\n");
        LogFileReader.Part first = read(null, 0, false, 1_000);

        Files.writeString(file, "three\nfo", StandardOpenOption.APPEND);
        LogFileReader.Part second = read(first.fileKey(), first.end(), false, 1_000);

        assertEquals(8, second.start());
        assertEquals("three\nfo", second.text());
        assertFalse("a line that is not complete does not end with a line feed", second.text().endsWith("\n"));

        LogFileReader.Part third = read(second.fileKey(), second.end(), false, 1_000);
        assertEquals("", third.text());
    }

    @Test
    public void notMoreIsReadThanAskedFor() throws Exception {
        Files.writeString(file, "0123456789\nabcdefghij\n");

        LogFileReader.Part first = read(null, 0, false, 15);
        assertEquals("0123456789\nabcd", first.text());
        assertEquals(15, first.end());
        assertTrue(first.hasMore());

        LogFileReader.Part second = read(first.fileKey(), first.end(), false, 15);
        assertEquals("efghij\n", second.text());
        assertFalse(second.hasMore());

        LogFileReader.Part nothing = read(first.fileKey(), 0, false, 0);
        assertEquals("asking for nothing tells whether the file can be read", "", nothing.text());
    }

    @Test
    public void aFileThatWasReplacedOrEmptiedIsReadFromItsBeginning() throws Exception {
        Files.writeString(file, "one\ntwo\nthree\n");
        LogFileReader.Part first = read(null, 0, false, 1_000);

        Files.move(file, file.resolveSibling("application.log.1"));
        Files.writeString(file, "a much longer new file than the one before\n");
        LogFileReader.Part replaced = read(first.fileKey(), first.end(), false, 1_000);
        assertNotEquals(first.fileKey(), replaced.fileKey());
        assertEquals(0, replaced.start());
        assertEquals("a much longer new file than the one before\n", replaced.text());

        Files.writeString(file, "new\n", StandardOpenOption.TRUNCATE_EXISTING);
        LogFileReader.Part emptied = read(replaced.fileKey(), replaced.end(), false, 1_000);
        assertEquals(0, emptied.start());
        assertEquals("new\n", emptied.text());
    }

    @Test
    public void aFileThatIsNotThereOrADirectoryIsNoFile() throws Exception {
        Files.delete(file);
        assertNull(read(null, 0, false, 1_000));

        Files.createDirectory(file);
        assertNull(read(null, 0, false, 1_000));
    }

    @Test
    public void aFileInAnotherCharacterSetIsConverted() throws Exception {
        Assume.assumeTrue("needs iconv", new File("/usr/bin/iconv").canExecute() || new File("/bin/iconv").canExecute());
        Files.write(file, "Überweisung fehlgeschlagen\n".getBytes(StandardCharsets.ISO_8859_1));

        String command = SshLogFileReader.command(file.toString(), null, 0, false, 1_000, "ISO-8859-1");

        assertEquals("Überweisung fehlgeschlagen\n", SshLogFileReader.parse(run(command)).text());
    }

    // ---------------------------------------------------------------------------------------------
    // the answer
    // ---------------------------------------------------------------------------------------------

    @Test
    public void aSessionEndsLinesAsATerminalDoes() throws Exception {
        LogFileReader.Part part = SshLogFileReader.parse(
                "@@EKG-LOG@@ 4711 20 8 12\r\nthree\r\nfour\r\nfi\r\n@@EKG-LOG@@ end\r\n");

        assertEquals("4711", part.fileKey());
        assertEquals(20, part.size());
        assertEquals(8, part.start());
        assertEquals(20, part.end());
        assertEquals("three\nfour\nfi", part.text());
    }

    @Test
    public void whatALoginScriptPrintsIsNotTheFile() throws Exception {
        LogFileReader.Part part = SshLogFileReader.parse(
                "Welcome to db-01\nLast login: yesterday\n@@EKG-LOG@@ 4711 4 0 4\none\n\n@@EKG-LOG@@ end\n");

        assertEquals("one\n", part.text());
    }

    @Test
    public void anAnswerThatIsNotCompleteIsAnError() {
        for (String answer : new String[]{null, "", "bash: sh: command not found",
                "@@EKG-LOG@@ 4711 4 0 4\none\n", "@@EKG-LOG@@ 4711 x 0 4\none\n\n@@EKG-LOG@@ end\n"}) {
            try {
                SshLogFileReader.parse(answer);
                fail("must be rejected: " + answer);
            } catch (IOException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("not read"));
            }
        }
    }

    @Test
    public void wordsOfACommandAreQuoted() {
        assertEquals("'plain'", SshLogFileReader.quote("plain"));
        assertEquals("'it'\\''s; rm -rf $HOME'", SshLogFileReader.quote("it's; rm -rf $HOME"));
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private LogFileReader.Part read(String fileKey, long position, boolean startAtEnd, int maxBytes) throws Exception {
        return SshLogFileReader.parse(run(SshLogFileReader.command(file.toString(), fileKey, position, startAtEnd,
                maxBytes, null)));
    }

    /**
     * Runs a command as the SSH server of a machine does: handed to the shell of the user.
     */
    static String run(String command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("/bin/sh", "-c", command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        return output;
    }
}
