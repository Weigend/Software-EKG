package io.softwareecg.collector.repository.rdb.api.datamanagement.compact;

import java.util.Date;

/**
 * todo insert comment here
 *
 * @since 29.11.2018
 */
public class CompactInterval {

    private final Date rangeBeginDatetime;

    private final Date rangeEndDatetime;

    public CompactInterval(Date rangeBegin, Date rangeEnd) {
        this.rangeBeginDatetime = rangeBegin;
        this.rangeEndDatetime = rangeEnd;
    }


    public Date getRangeBeginDatetime() {
        return rangeBeginDatetime;
    }

    public Date getRangeEndDatetime() {
        return rangeEndDatetime;
    }
}
