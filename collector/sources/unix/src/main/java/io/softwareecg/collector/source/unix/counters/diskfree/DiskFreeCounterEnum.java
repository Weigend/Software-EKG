/**
 * ______________________________________________________________________________
 * <p/>
 * Project: Design for Diagnosability ______________________________________________________________________________
 * <p/>
 * Copyright: (c) Weigend AM, all rights reserved ______________________________________________________________________________
 */
package io.softwareecg.collector.source.unix.counters.diskfree;

import java.text.MessageFormat;

/**
 */
public enum DiskFreeCounterEnum {
    Size("1M-blocks", 1),
    Used("Used", 2),
    Available("Available", 3),
    Use("Use%", 4);

    private final String expression;
    private final int index;

    DiskFreeCounterEnum(String expression, int index) {
        this.expression = expression;
        this.index = index;
    }

    /**
     * Returns the diskfree-expression for the counter-group.
     *
     * @param arguments possible arguments for formatting.
     * @return expression.
     */
    public String getExpression(String... arguments) {
        return MessageFormat.format(expression, (Object[]) arguments);
    }

    public int getIndex() {
        return index;
    }
}
