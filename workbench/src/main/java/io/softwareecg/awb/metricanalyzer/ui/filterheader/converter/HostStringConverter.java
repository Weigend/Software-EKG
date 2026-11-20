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
package io.softwareecg.awb.metricanalyzer.ui.filterheader.converter;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.Host;
import javafx.util.StringConverter;

/**
 */
public class HostStringConverter extends StringConverter<Host> {

    @Override
    public String toString(Host host) {
        if (host != null) {
            return host.getName();
        }
        return "*";
    }

    @Override
    public Host fromString(String s) {
        if (s != null) {
            return new Host(s);
        }
        return new Host("*");
    }
}
