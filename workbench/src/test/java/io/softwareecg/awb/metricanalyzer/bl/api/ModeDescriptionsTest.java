package io.softwareecg.awb.metricanalyzer.bl.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.softwareecg.awb.investigation.model.SeriesCombineMode;
import io.softwareecg.awb.investigation.model.SeriesSmoothingType;
/**
 * The modes of the lists "Combine metrics" and "Smoothing" say what they do: the header of a
 * chart shows it as a tooltip, because names such as "Concatenate" do not tell.
 */
public class ModeDescriptionsTest {

    @Test
    public void everyCombineModeIsDescribedBeginningWithItsName() {
        for (SeriesCombineMode mode : SeriesCombineMode.values()) {
            assertTrue(mode + ": " + mode.getDescription(), mode.getDescription().startsWith(mode.getName() + ": "));
            assertTrue(mode + " says nothing", mode.getDescription().length() > mode.getName().length() + 20);
        }
    }

    @Test
    public void everySmoothingIsDescribedBeginningWithItsName() {
        for (SeriesSmoothingType type : SeriesSmoothingType.values()) {
            assertTrue(type + ": " + type.getDescription(), type.getDescription().startsWith(type.getName() + ": "));
            assertTrue(type + " says nothing", type.getDescription().length() > type.getName().length() + 20);
        }
    }

    @Test
    public void concatenateIsTheJoiningOfTheMeasurements() {
        assertEquals("Concatenate: the parts a series has in several measurements are joined to one line",
                SeriesCombineMode.CONCAT.getDescription());
    }

    @Test
    public void theSumsNameTheirInterval() {
        assertEquals("Sum (per Minute): one series instead of all: the sum of their values in every minute",
                SeriesCombineMode.SUM_MIN.getDescription());
    }
}
