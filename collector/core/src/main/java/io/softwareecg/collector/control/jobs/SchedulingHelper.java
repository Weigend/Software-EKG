package io.softwareecg.collector.control.jobs;

import io.softwareecg.collector.common.LoggerFactory;
import org.slf4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Helper for scheduling.
 */
public final class SchedulingHelper {

    /**
     * at time format.
     */
    public static final String DATE_FORMAT = "HH:mm:ss";

    /**
     * Our logger
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Utility class.
     */
    private SchedulingHelper() {

    }


    /**
     * The method calculates a date with the given at-time for the next day.
     * The string has to be HH:mm:ss
     *
     * @param at the time to run.
     * @return a date.
     * @throws ParseException the parse exception
     */
    public static Date nextDate(String at) throws ParseException {
        Date time = getDateFormat().parse(at);
        Date now = new Date();
        Calendar nowCal = Calendar.getInstance();
        // now as calendar
        nowCal.setTime(now);

        // adjust time HH:MM:SS
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        // adjust date fields
        calendar.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, nowCal.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_YEAR, nowCal.get(Calendar.DAY_OF_YEAR));

        if (nowCal.getTimeInMillis() >= calendar.getTimeInMillis()) {
            // add one day only now is later than the scheduled time.
            calendar.add(Calendar.DATE, 1);
        }


        return calendar.getTime();
    }

    /**
     * Helper to wait until a given date is reached.
     *
     * @param date the date
     */
    public static void waitUntil(Date date) {
        Lock lock = new ReentrantLock();
        try {
            lock.newCondition().await(date.getTime(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted Condition.", e);
        }
    }

    /**
     * Helper to clone format.
     * @return a cloned format.
     */
    private static DateFormat getDateFormat() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.US);
    }
}
