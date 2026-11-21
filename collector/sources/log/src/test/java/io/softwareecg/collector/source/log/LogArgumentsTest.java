package io.softwareecg.collector.source.log;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LogArgumentsTest {

    @Test
    public void withoutArgumentsOnlyNewLinesOfAUtf8FileAreRead() {
        for (String none : new String[]{null, "", "  ", ";"}) {
            LogArguments arguments = LogArguments.parse(none);

            assertEquals(0, arguments.lookbackMillis());
            assertNull(arguments.timestampPattern());
            assertEquals(ZoneId.systemDefault(), arguments.zone());
            assertEquals(StandardCharsets.UTF_8, arguments.charset());
            assertEquals(22, arguments.port());
        }
    }

    @Test
    public void argumentsAreSeparatedBySemicolons() {
        LogArguments arguments = LogArguments.parse(
                "lookback=24h; timestamp=dd.MM.yyyy HH:mm:ss,SSS ;zone=Europe/Berlin; charset=ISO-8859-1; port=2222");

        assertEquals(24 * 3_600_000L, arguments.lookbackMillis());
        assertEquals("a pattern keeps its blanks and commas", "dd.MM.yyyy HH:mm:ss,SSS", arguments.timestampPattern());
        assertEquals(ZoneId.of("Europe/Berlin"), arguments.zone());
        assertEquals(StandardCharsets.ISO_8859_1, arguments.charset());
        assertEquals(2222, arguments.port());
    }

    @Test
    public void lookbackHasAUnit() {
        assertEquals(30_000, LogArguments.parse("lookback=30s").lookbackMillis());
        assertEquals(1_800_000, LogArguments.parse("lookback=30m").lookbackMillis());
        assertEquals(7 * 86_400_000L, LogArguments.parse("lookback=7d").lookbackMillis());
    }

    @Test
    public void whatIsNotKnownOrNotUsableIsAnError() {
        assertRejected("lookbak=24h", "no argument 'lookbak'");
        assertRejected("lookback=24", "not a duration");
        assertRejected("lookback=24h, zone=UTC", "separated by semicolons");
        assertRejected("zone=Berlin", "not a time zone");
        assertRejected("charset=klingon", "not a character set");
        assertRejected("port=ssh", "not a port");
        assertRejected("port=0", "not a port");
        assertRejected("lookback", "name=value");
        assertRejected("timestamp=", "'timestamp' is empty");
    }

    private static void assertRejected(String arguments, String expected) {
        try {
            LogArguments.parse(arguments);
            fail(arguments + " must be rejected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
        }
    }
}
