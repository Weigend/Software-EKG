package io.softwareecg.collector.common;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * A date time datamanagement that return UTC time that is the default timezone
 * for Software EKG.
 *
 * @since 14.08.2018
 */
public class DateTimeUtil {

    /**
     * Returns the current UTC time in millis from the epoch
     * of 1970-01-01T00:00:00Z.
     *
     * @return the current time in millis
     */
    public static long getNowAsUtcMillis() {
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        return Instant.from(utc).toEpochMilli();
    }

    /**
     * Returns the current UTC time as Instant from the epoch
     * of 1970-01-01T00:00:00Z.
     *
     * @return the current time as Instant
     */
    public static Instant getNowAsUtcInstant() {
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        return Instant.from(utc);
    }

    /**
     * Returns the current UTC time as good old java Date instance
     * from the epoch of 1970-01-01T00:00:00Z.
     *
     * @return the current time as Date instance
     */
    public static Date getNowAsUtcDate() {
        return Date.from(getNowAsUtcInstant());
    }
}
