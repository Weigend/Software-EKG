//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.metricview.queryeditor.converter;

import io.softwareecg.awb.timeseries.dimension.Metric;
import javafx.util.StringConverter;

/**
 */
public class MetricStringConverter extends StringConverter<Metric> {

    private static final String EMPTY_METRIC = "";

    @Override
    public String toString(Metric metric) {
        if (metric != null) {
            return metric.getName();
        }

        return EMPTY_METRIC;
    }

    @Override
    public Metric fromString(String s) {
        if (s != null) {
            return new Metric(s);
        }

        return new Metric(EMPTY_METRIC);
    }
}
