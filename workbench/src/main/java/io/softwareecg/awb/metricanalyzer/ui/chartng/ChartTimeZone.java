package io.softwareecg.awb.metricanalyzer.ui.chartng;


import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * TimeZone item that combines the Java ZoneId with optional
 * custom alias labels.
 *
 * @since 03.10.2018
 */
public class ChartTimeZone {

    /**
     * static mapping from some of the GMT time zones to the same but
     * with country/capital city appended
     */
    private static final Map<String, String> TIME_ZONE_ALIAS_MAPPING = Map.of(
            "GMT+0", "GMT+0  (Iceland/Reykjavik)",
            "GMT+2", "GMT+2  (Namibia/Windhoek)"
    );

    private static final String BERLIN = "Europe/Berlin";

    /**
     * The time zone a chart shows its times in unless another one is chosen: that of the
     * computer. It was Europe/Berlin for everybody once, which is right in Germany only.
     */
    private static final ChartTimeZone DEFAULT_TIME_ZONE = localTimeZone(ZoneId.systemDefault());

    /**
     * @param zone the time zone of the computer
     * @return its entry in the list of time zones, which names it as the local time
     */
    static ChartTimeZone localTimeZone(ZoneId zone) {
        return new ChartTimeZone("Local time (" + zone.getId() + ")", zone.getId());
    }

    /**
     * The ZoneId that is a unique id that represents the time zone
     * of this ChartTimeZone instance
     */
    private ZoneId timeZone;

    /**
     * The human readable alias of the ZoneId
     * that can be equal but don't need to
     */
    private final String alias;

    //==============================================================================================================
    // constructors and API of ChartTimeZone class
    //==============================================================================================================

    /**
     * Constructs a ChartTimeZone based on the given zoneId
     * that must match on of available id's provided {@link ZoneId} class.
     *
     * @param zoneId the zone id that represents the timezone
     */
    public ChartTimeZone(String zoneId) {
        this(zoneId, zoneId);
    }

    /**
     * Constructs a ChartTimeZone based on the given zoneId and an explicitly set alias.
     * The zoneId parameter must match on of available id's provided {@link ZoneId} class.
     *
     * @param zoneAlias the human readable alias of the time zone
     * @param zoneId the zone id that represents the timezone
     */
    public ChartTimeZone(String zoneAlias, String zoneId) {
        this.alias = TIME_ZONE_ALIAS_MAPPING.getOrDefault(zoneAlias, zoneAlias);

        if (StringUtils.isNotBlank(zoneId)) {
            timeZone = ZoneId.of(zoneId);
        }
    }

    /**
     * Returns the underlying ZoneId that represents this time zone.
     *
     * @return a JDK ZoneId instance
     */
    public ZoneId getTimeZone() {
        return timeZone;
    }

    /**
     * Returns the human readable alias for the time zone
     *
     * @return an alias name for the time zone
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Returns the time zone the EKG AWB should use as default setting to display
     * data in the chart
     *
     * @return the default ime zone
     */
    public static ChartTimeZone getDefaultTimeZone() {
        return DEFAULT_TIME_ZONE;
    }

    /**
     * Returns a list of all time zones that are supported by the ChartNG engine.
     *
     * @return a list of supported time zone in sorted order
     */
    public static List<ChartTimeZone> getChartTimeZones() {

        List<ChartTimeZone> resultList = new ArrayList<>();

        resultList.add(DEFAULT_TIME_ZONE);
        resultList.add(new ChartTimeZone("UTC", "UTC"));
        if (!BERLIN.equals(DEFAULT_TIME_ZONE.getTimeZone().getId())) {
            // the zone every chart had before; the only one beside the local one that knows summer time
            resultList.add(new ChartTimeZone(BERLIN));
        }

        ArrayList<ChartTimeZone> gmtTimeZones = ZoneId.getAvailableZoneIds().stream()
                .filter(id -> id.contains("GMT+") || id.contains("GMT-"))
                .map(id -> id.replaceAll("([+-])([1-9])$", "$10$2"))
                .sorted()
                .map(id -> id.replaceAll("([+-])0([1-9])$", "$1$2"))
                .map(id -> id.replace("Etc/", ""))
                .map(ChartTimeZone::new)
                .collect(Collectors.toCollection(ArrayList::new));

        resultList.addAll(gmtTimeZones);

        return resultList;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ChartTimeZone.class.getSimpleName() + "[", "]")
                .add("alias='" + alias + "'")
                .toString();
    }
}
