package io.softwareecg.collector.ui;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DisplayTextTest {

    @Test
    public void valuesAreWrittenWithTheDigitsThatTellSomething() {
        assertEquals("0", DisplayText.value(0));
        assertEquals("42", DisplayText.value(42));
        assertEquals("0.250", DisplayText.value(0.25));
        assertEquals("12.50", DisplayText.value(12.5));
        assertEquals("512.3", DisplayText.value(512.34));
        assertEquals("9999", DisplayText.value(9_999));
        assertEquals("12.5 k", DisplayText.value(12_500));
        assertEquals("3.40 M", DisplayText.value(3_400_000));
        assertEquals("512 M", DisplayText.value(512_000_000));
        assertEquals("16.0 G", DisplayText.value(16e9));
        assertEquals("-12.5 k", DisplayText.value(-12_500));
        assertEquals("–", DisplayText.value(Double.NaN));
    }

    @Test
    public void timesAreSaidAsHowLongAgo() {
        Instant now = Instant.parse("2026-11-21T10:00:00Z");
        assertEquals("now", DisplayText.ago(now.minusSeconds(1), now));
        assertEquals("15 s ago", DisplayText.ago(now.minusSeconds(15), now));
        assertEquals("5 min ago", DisplayText.ago(now.minusSeconds(330), now));
        assertEquals("3 h ago", DisplayText.ago(now.minusSeconds(3 * 3_600 + 5), now));
        assertEquals("2 d ago", DisplayText.ago(now.minusSeconds(2 * 86_400 + 5), now));
        assertEquals("a clock that is a little ahead", "now", DisplayText.ago(now.plusSeconds(3), now));
        assertEquals("–", DisplayText.ago(null, now));

        assertEquals("45 s", DisplayText.span(Duration.ofSeconds(45)));
        assertEquals("12 min", DisplayText.span(Duration.ofMinutes(12)));
        assertEquals("3 h 20 min", DisplayText.span(Duration.ofMinutes(200)));
        assertEquals("2 d 4 h", DisplayText.span(Duration.ofHours(52)));
    }

    @Test
    public void theDisplayIsAWindowOfTheSizeOfASmallPanelUnlessToldOtherwise() {
        DisplaySettings settings = DisplaySettings.of(new Properties());
        assertFalse(settings.fullScreen());
        assertEquals(800, settings.width());
        assertEquals(480, settings.height());

        Properties properties = new Properties();
        properties.setProperty(DisplaySettings.FULLSCREEN_KEY, " true ");
        properties.setProperty(DisplaySettings.SIZE_KEY, "1024 x 600");
        settings = DisplaySettings.of(properties);
        assertTrue(settings.fullScreen());
        assertEquals(1024, settings.width());
        assertEquals(600, settings.height());

        properties.setProperty(DisplaySettings.SIZE_KEY, "large");
        try {
            DisplaySettings.of(properties);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("width x height"));
        }
    }
}
