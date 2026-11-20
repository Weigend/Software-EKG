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

import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Management for the colors of series.
 * Colors are assigned sequentially: 1st series gets color 0, 2nd gets color 1, etc.
 * Custom colors set by user are preserved across chart reloads.
 *
 * <p>Pure static-utility class (private constructor). The legacy {@code @Singleton}
 * annotation was misleading — all callers use static methods like
 * {@code ChartColorStyle.getColor(...)}.
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

    private static int availableColorIdx = 0;
    
    /** Stores custom colors set by user, keyed by position index */
    private static final Map<Integer, Color> CUSTOM_COLORS = new HashMap<>();

    private ChartColorStyle() {
    }

    /**
     * Returns the next color for a series.
     * If user has set a custom color for this position, that color is returned.
     *
     * @param series series (unused, kept for API compatibility)
     * @return the next color
     */
    public static Color getColor(XYChart.Series series) {
        int currentIdx = availableColorIdx++;
        
        // Check if user has set a custom color for this position
        if (CUSTOM_COLORS.containsKey(currentIdx)) {
            return CUSTOM_COLORS.get(currentIdx);
        }
        
        return AVAILABLE_COLORS[currentIdx % AVAILABLE_COLORS.length];
    }
    
    /**
     * Returns the index of the last assigned color.
     * Call this immediately after getColor() to get the index for the series.
     */
    public static int getLastAssignedIndex() {
        return availableColorIdx - 1;
    }
    
    /**
     * Sets a custom color for a specific position index.
     * This color will be used when the chart is reloaded.
     */
    public static void setCustomColor(int index, Color color) {
        CUSTOM_COLORS.put(index, color);
    }

    /**
     * Resets the color index to start from the beginning.
     * Call this when the chart is cleared.
     */
    public static void reset() {
        availableColorIdx = 0;
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

    /**
     * Returns the string representation of the color of the given series
     * @param series series
     * @return the string representation of the color
     */
    public static String getRGBCodeColor(XYChart.Series series) {
        return toRGBCode(getColor(series));
    }

}
