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

import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import javafx.util.StringConverter;

/**
 */
public class MetricGroupStringConverter extends StringConverter<MetricGroup> {

    @Override
    public String toString(MetricGroup metricGroup) {
        if (metricGroup != null) {
            return metricGroup.getName();
        }
        return "*";
    }

    @Override
    public MetricGroup fromString(String s) {
        if (s != null) {
            return new MetricGroup(s);
        }
        return new MetricGroup("*");
    }
}
