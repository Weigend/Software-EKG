/**
 * ______________________________________________________________________________
 *
 * Project: Design for Diagnosability
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.unix.analyzer;

import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.unix.counters.diskfree.DiskFreeCounterEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 */
public class DiskFreeAnalyzer extends AbstractTerminalAnalyzer {

    @Override
    public Collection<? extends MetricDefinition> expandCounter(MetricDefinition metricDefinition) {
        return Collections.singletonList(metricDefinition);
    }

    @Override
    public double getCounter(String name) {
        //check if dir exists
        if (terminalOutputIsInvalid()) {
            return -1;
        }

        DiskFreeCounterEnum counterEnum = getMatchingEnumFrom(name);

        if (counterEnum == null) {
            return -1;
        }

        return getCounterForEnum(counterEnum);
    }


    private DiskFreeCounterEnum getMatchingEnumFrom(String name) {

        switch (name) {
            case "1M-blocks":
                return DiskFreeCounterEnum.Size;
            case "Used":
                return DiskFreeCounterEnum.Used;
            case "Available":
                return DiskFreeCounterEnum.Available;
            case "Use%":
                return DiskFreeCounterEnum.Use;
        }
        return null;
    }

    private double getCounterForEnum(DiskFreeCounterEnum name) {
        //first line:   Filesystem     1K-blocks   Used Available Use% Mounted on
        //second line:  /dev/xvda5       5162796 737804   4162736  16% /home

        if (terminalOutput != null && terminalOutput.length == 2) {
            List<String> values = extractValues();
            String value = values.get(name.getIndex());

            //Use has a percentage in its value
            if (name == DiskFreeCounterEnum.Use) {
                value = value.substring(0, value.length() - 1);
            }

            return asNumber(value);
        }
        return -1;
    }

    private List<String> extractValues() {
        String[] valuesWithWhitespaces = terminalOutput[1].split("\\s");
        return removeWhitespaces(valuesWithWhitespaces);

    }

    private List<String> removeWhitespaces(String[] valuesWithWhitespaces) {
        List<String> onlyValues = new ArrayList<>();

        for (String value : valuesWithWhitespaces) {
            if (StringUtils.isNotEmpty(value)) {
                onlyValues.add(value);
            }
        }
        return onlyValues;
    }


    private double asNumber(String value) {
        return Double.parseDouble(value);
    }

    private boolean terminalOutputIsInvalid() {
        if (terminalOutputIsEmpty()) {
            return true;
        }
        for (String s : terminalOutput) {
            if (s.contains("No such file or directory")) {
                return true;
            }
        }
        return false;
    }

    private boolean terminalOutputIsEmpty() {
        return terminalOutput == null || terminalOutput.length == 0;
    }

    @Override
    public void setTerminalOutput(String[] dfOutput) {
        this.terminalOutput = dfOutput != null ? dfOutput.clone() : null;
    }
}
