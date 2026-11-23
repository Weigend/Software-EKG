package io.softwareecg.awb.metricview.command;

/**
 * An enum that defines the chart commands that are supported by
 * the {@link ChartCommandProcessor} class to execute chart actions
 * via central execution bus.
 *
 * @since 30.08.2018
 */
public enum ChartCommand {

    LOAD_BASE_CHART_DATA,

    PUSH_BASE_TO_BG,

    CHANGE_CHART_COLOR,

    CHANGE_SERIES_COLOR,

    CLEAR_ALL,

    SET_CHART_VISIBLE,

    DELETE_BG_CHART,

    /** a chart of the background becomes the chart in front, and what was in front goes back */
    BRING_BG_TO_FRONT,

    DELETE_ALL_BG_CHARTS,

    RELATIVE_COMBINE_TO_BASE,

    ABSOLUTE_COMBINE_TO_BASE,

    CHANGE_CHART_TYPE,

    FORCE_ALIGN_Y_AXIS,

    FREE_ALIGN_Y_AXIS
}
