//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Martin Binder
//       creation date: 2009
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-03-04 17:28:30 +0100 (Do, 04 Mrz 2010) $
//       revision:      $Revision: 421 $
//       description:   Value of a counter for lazy loaded source collectors.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source;

import io.softwareecg.collector.common.DateTimeUtil;

import java.util.Date;
import java.util.List;


/**
 * The Class CounterSeries.
 */
public class CounterSeries {

    /** The date. */
    private final Date date;

    /** The values. */
    private final List<Double> values;

    /**
     * Instantiates a new counter series.
     *
     * @param values the values
     */
    public CounterSeries(List<Double> values) {
        this.date = DateTimeUtil.getNowAsUtcDate();
        this.values = values;
    }

    /**
     * Instantiates a new counter series.
     *
     * @param date the date
     * @param values the values
     */
    public CounterSeries(Date date, List<Double> values) {
        this.date = (Date) date.clone();
        this.values = values;
    }

    /**
     * Gets the date.
     *
     * @return the date
     */
    public Date getDate() {
        return (Date) date.clone();
    }

    /**
     * Gets the values.
     *
     * @return the values
     */
    public List<Double> getValues() {
        return values;
    }
}
