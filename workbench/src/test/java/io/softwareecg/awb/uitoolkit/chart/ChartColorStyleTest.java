package io.softwareecg.awb.uitoolkit.chart;

import javafx.scene.paint.Color;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.softwareecg.awb.uitoolkit.chart.ChartColorStyle;
/**
 * Which colour a new series gets.
 */
public class ChartColorStyleTest {

    private static final List<Color> NONE = List.of();

    /** the colours of the first three series of a chart */
    private static final List<Color> FIRST_THREE = List.of(Color.DARKSEAGREEN, Color.DARKSLATEBLUE, Color.DARKSLATEGRAY);

    @Test
    public void aChartAloneTakesTheColoursInTheOrderOfThePalette() {
        List<Color> colours = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            colours.add(ChartColorStyle.nextColor(colours, NONE));
        }

        assertEquals(FIRST_THREE, colours);
    }

    @Test
    public void aSeriesThatLeftTheChartGivesItsColourBack() {
        assertEquals(Color.DARKSLATEBLUE,
                ChartColorStyle.nextColor(List.of(Color.DARKSEAGREEN, Color.DARKSLATEGRAY), NONE));
    }

    /**
     * A chart in front of others begins with a colour none of them has. It used to begin with
     * the first colour of the palette as they did, and its line looked like one of theirs.
     */
    @Test
    public void aChartInFrontOfOthersGetsColoursThatDifferFromTheirs() {
        List<Color> inFront = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Color colour = ChartColorStyle.nextColor(inFront, FIRST_THREE);

            assertFalse("a colour of the background: " + colour, FIRST_THREE.contains(colour));
            assertFalse("a colour the chart has already: " + colour, inFront.contains(colour));
            for (Color used : FIRST_THREE) {
                assertTrue(colour + " is too much like " + used, ChartColorStyle.distance(colour, used) > 200);
            }
            inFront.add(colour);
        }
    }

    @Test
    public void theSameSituationGivesTheSameColour() {
        assertEquals(ChartColorStyle.nextColor(NONE, FIRST_THREE), ChartColorStyle.nextColor(NONE, FIRST_THREE));
    }

    @Test
    public void moreSeriesThanColoursBeginAgain() {
        List<Color> colours = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            colours.add(ChartColorStyle.nextColor(colours, NONE));
        }

        assertEquals(60, colours.size());
        assertEquals(48, colours.stream().distinct().count());
    }

    @Test
    public void blackAndDarkGreyAreNearAndBlackAndOrangeAreFar() {
        assertTrue(ChartColorStyle.distance(Color.BLACK, Color.DARKSLATEGRAY) < ChartColorStyle.distance(Color.BLACK, Color.DARKORANGE));
        assertEquals(0, ChartColorStyle.distance(Color.NAVY, Color.NAVY), 0.0001);
    }
}
