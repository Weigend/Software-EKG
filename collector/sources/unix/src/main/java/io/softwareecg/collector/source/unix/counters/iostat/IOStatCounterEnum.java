//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.source.unix.counters.iostat;

//--------------------------------------------------------------------------------------------------------------
//  Group and MetricDefinition Definition the analyzer looking for.
//  Different UNIX distributions have different supported counters at iostat.
//  If you work with an UNIX that have counters that aren't supported by this
//  class, please feel free to add them to the relating counter-group
//--------------------------------------------------------------------------------------------------------------

import java.text.MessageFormat;

/**
 * The supported kinds of iostat MetricDefinition Groups
 */
public enum IOStatCounterEnum {
    Device("\\Device({0})\\"),
    Filesystem("\\Filesystem({0})\\");

    private final String expression;

    IOStatCounterEnum(String expression) {
        this.expression = expression;
    }

    /**
     * Returns the iostat-expression for the counter-group.
     *
     * @param arguments possible arguments for formatting.
     * @return expression.
     */
    public String getExpression(String... arguments) {
        return MessageFormat.format(expression, (Object[]) arguments);
    }
}
