package io.softwareecg.awb.metricanalyzer.ui.filterheader.converter;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.ProcessGroup;
import javafx.util.StringConverter;

/**
 * The process group of the filter header as the text of its field.
 */
public class ProcessGroupStringConverter extends StringConverter<ProcessGroup> {

    @Override
    public String toString(ProcessGroup processGroup) {
        return processGroup != null ? processGroup.getValueName() : "*";
    }

    @Override
    public ProcessGroup fromString(String text) {
        return new ProcessGroup(text != null ? text : "*");
    }
}
