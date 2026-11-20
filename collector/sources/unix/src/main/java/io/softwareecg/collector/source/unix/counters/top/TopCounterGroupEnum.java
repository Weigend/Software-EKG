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
package io.softwareecg.collector.source.unix.counters.top;

import java.text.MessageFormat;

/**
 * the group is the first part of the counter string.
 */
public enum TopCounterGroupEnum {
    /**
     * The Process.
     */
    Process("\\Process({0})\\"),
    /**
     * The Memory.
     */
    Memory("\\Memory\\"),
    /**
     * The Swap.
     */
    Swap("\\Swap\\"),
    /**
     * The Tasks.
     */
    Tasks("\\Tasks\\"),
    /**
     * The cpu.
     */
    Cpu("\\Cpu\\"),
    /**
     * The User.
     */
    User("\\User\\"),
    /**
     * The Uptime.
     */
    Uptime("\\Uptime\\"),
    /**
     * The Load.
     */
    Load("\\Load\\");

    private final String expression;

    TopCounterGroupEnum(String expression) {
        this.expression = expression;
    }

    /**
     * Returns the TOP-expression for the counter-group.
     *
     * @param arguments possible arguments for formatting.
     * @return expression.
     */
    public String getExpression(String... arguments) {
        return MessageFormat.format(expression, (Object[]) arguments);
    }
}
