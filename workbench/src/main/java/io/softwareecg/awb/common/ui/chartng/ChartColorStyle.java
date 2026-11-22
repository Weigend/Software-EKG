//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.common.ui.chartng;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The colours of series: the palette, and which of it a new series gets. A series keeps its
 * colour as long as it is shown, see {@link ColoredChart#replaceSeries}.
 *
 * <p>Pure static-utility class (private constructor).
 */
public final class ChartColorStyle {

    private static final Color[] AVAILABLE_COLORS = {
            Color.DARKSEAGREEN,
            Color.DARKSLATEBLUE,
            Color.DARKSLATEGRAY
            , Color.BLACK
            , Color.BLUE
            , Color.BLUEVIOLET
            , Color.MAROON
            , Color.BROWN
            , Color.MEDIUMBLUE
            , Color.CADETBLUE
            , Color.MEDIUMORCHID
            , Color.CHOCOLATE
            , Color.MEDIUMSEAGREEN
            , Color.MEDIUMSLATEBLUE
            , Color.CRIMSON
            , Color.MEDIUMVIOLETRED
            , Color.MIDNIGHTBLUE
            , Color.DARKBLUE
            , Color.DARKGOLDENROD
            , Color.DARKGREEN
            , Color.NAVY
            , Color.OLIVE
            , Color.DARKOLIVEGREEN
            , Color.OLIVEDRAB
            , Color.DARKORANGE
            , Color.DARKORCHID
            , Color.ORANGERED
            , Color.DARKRED
            , Color.PALEVIOLETRED
            , Color.DARKVIOLET
            , Color.DEEPSKYBLUE
            , Color.DIMGRAY
            , Color.DODGERBLUE
            , Color.FIREBRICK
            , Color.PURPLE
            , Color.RED
            , Color.FORESTGREEN
            , Color.ROYALBLUE
            , Color.SADDLEBROWN
            , Color.SEAGREEN
            , Color.GREEN
            , Color.SIENNA
            , Color.SLATEBLUE
            , Color.INDIANRED
            , Color.SLATEGRAY
            , Color.INDIGO
            , Color.STEELBLUE
            , Color.TEAL
    };

    public static final int INT_255 = 255;

    private ChartColorStyle() {
    }

    /**
     * The colour of a series that is added to a chart.
     * <p>
     * A chart that is alone in its panel takes the colours in the order of the palette, the
     * first one none of its series has. With other charts in the panel - those in the
     * background - the order would give the new chart the colours they have: every chart
     * begins with the first ones. Then the colour is the one of the palette that differs most
     * from all colours in use, so that front and background can be told apart.
     *
     * @param ofTheChart     the colours of the series the chart has already
     * @param ofOtherCharts the colours of the series of the other charts of the panel
     * @return the colour of the new series
     */
    public static Color nextColor(Collection<Color> ofTheChart, Collection<Color> ofOtherCharts) {
        List<Color> free = new ArrayList<>();
        for (Color color : AVAILABLE_COLORS) {
            if (!ofTheChart.contains(color) && !ofOtherCharts.contains(color)) {
                free.add(color);
            }
        }
        if (free.isEmpty()) {
            // more series than colours: around again
            return AVAILABLE_COLORS[ofTheChart.size() % AVAILABLE_COLORS.length];
        }
        if (ofOtherCharts.isEmpty()) {
            return free.get(0);
        }

        List<Color> inUse = new ArrayList<>(ofOtherCharts);
        inUse.addAll(ofTheChart);
        Color farthest = free.get(0);
        double distanceOfTheFarthest = -1;
        for (Color candidate : free) {
            double nearest = Double.MAX_VALUE;
            for (Color used : inUse) {
                nearest = Math.min(nearest, distance(candidate, used));
            }
            if (nearest > distanceOfTheFarthest) {
                farthest = candidate;
                distanceOfTheFarthest = nearest;
            }
        }
        return farthest;
    }

    /**
     * How different two colours look: the distance of their red, green and blue, weighted by
     * how sensitive the eye is to each of them at this level of red ("redmean").
     */
    static double distance(Color one, Color other) {
        double meanRed = (one.getRed() + other.getRed()) / 2 * INT_255;
        double red = (one.getRed() - other.getRed()) * INT_255;
        double green = (one.getGreen() - other.getGreen()) * INT_255;
        double blue = (one.getBlue() - other.getBlue()) * INT_255;
        return Math.sqrt((2 + meanRed / 256) * red * red + 4 * green * green + (2 + (255 - meanRed) / 256) * blue * blue);
    }

    /**
     * Returns the string representation of the color
     *
     * @param color color
     * @return the string representation of the color
     */
    public static String toRGBCode(Color color) {
        Color nColor = (color == null) ? Color.BLACK : color;
        return String.format("#%02X%02X%02X", (int) (nColor.getRed() * INT_255), (int) (nColor.getGreen() * INT_255),
                (int) (nColor.getBlue() * INT_255));
    }

    /**
     * Returns the string representation of the color
     *
     * @param color   color
     * @param opacity opacity
     * @return the string representation of the color
     */
    public static String toRGBCode(Color color, double opacity) {
        return String.format("%s%02X", toRGBCode(color), (int) (opacity * INT_255));
    }
}
