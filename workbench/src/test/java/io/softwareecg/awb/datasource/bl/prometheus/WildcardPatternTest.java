package io.softwareecg.awb.datasource.bl.prometheus;

import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WildcardPatternTest {

    private static final List<String> HOSTS = List.of("db-01", "db-02", "DB-10", "web-01", "web-02", "batch");

    @Test
    public void starAndQuestionMarkSelectNamesWhateverTheirCase() {
        assertEquals(List.of("db-01", "db-02", "DB-10"), WildcardPattern.parse("DB*").select(HOSTS));
        assertEquals(List.of("db-01", "db-02", "web-01", "web-02"), WildcardPattern.parse("*-0?").select(HOSTS));
        assertEquals(List.of("batch"), WildcardPattern.parse("batch").select(HOSTS));
    }

    @Test
    public void severalPatternsAreAlternatives() {
        assertEquals(List.of("db-01", "web-01", "batch"), WildcardPattern.parse("*-01, batch").select(HOSTS));
        assertEquals(List.of("db-01", "web-01"), WildcardPattern.parse(" db-01 ; web-01 ;").select(HOSTS));
    }

    @Test
    public void nothingTypedSelectsEverything() {
        assertTrue(WildcardPattern.parse(null).isAll());
        assertTrue(WildcardPattern.parse("  ").isAll());
        assertTrue(WildcardPattern.parse("*").isAll());
        assertTrue(WildcardPattern.parse("db*, *").isAll());
        assertFalse(WildcardPattern.parse("db*").isAll());
        assertEquals(HOSTS, WildcardPattern.ALL.select(HOSTS));
    }

    @Test
    public void whatIsNoWildcardIsTakenLiterally() {
        WildcardPattern pattern = WildcardPattern.parse("10.0.0.1, a+b(1)");

        assertTrue(pattern.matches("10.0.0.1"));
        assertFalse("a dot is a dot, not any character", pattern.matches("10x0x0x1"));
        assertTrue(pattern.matches("a+b(1)"));
        assertEquals("10\\.0\\.0\\.1|a\\+b\\(1\\)", pattern.regex());
    }

    @Test
    public void theExpressionSelectsWhatThePatternSelects() {
        // Prometheus matches the whole name, without regard to case where the caller says so
        Pattern regex = Pattern.compile(WildcardPattern.parse("db*, web-0?").regex(), Pattern.CASE_INSENSITIVE);

        assertEquals(List.of("db-01", "db-02", "DB-10", "web-01", "web-02"),
                HOSTS.stream().filter(host -> regex.matcher(host).matches()).toList());
    }
}
