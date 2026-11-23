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

import io.softwareecg.awb.projectcontext.model.Project;
import javafx.util.StringConverter;

/**
 */
public class SeriesStringConverter extends StringConverter<Project> {

    @Override
    public String toString(Project project) {
        if (project != null) {
            return project.getName();
        }
        return "*";
    }

    @Override
    public Project fromString(String s) {
        if (s != null) {
            return new Project(s);
        }
        return new Project("*");
    }
}
