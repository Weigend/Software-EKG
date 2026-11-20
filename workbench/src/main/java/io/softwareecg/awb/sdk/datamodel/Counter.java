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
package io.softwareecg.awb.sdk.datamodel;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a windows performance counter for a given host and all recorded values for that counter.
 *
 */
public final class Counter {

    /**
     * The counter name.
     */
    private final String counterName;

    /**
     * The values.
     */
    private List<Value> values;

    /**
     * Creates a counter.
     *
     * @param counterName the unique name.
     */
    public Counter(String counterName) {
        this.counterName = counterName;
        values = new ArrayList<>();
    }

    /**
     * Getter for property 'counterName'.
     *
     * @return Value for property 'counterName'.
     */
    public String getCounterName() {
        return counterName;
    }

    /**
     * Returns an list of values that are sorted by time
     *
     * @return Value for property 'values'.
     */
    public List<Value> getValues() {
        return values;
    }

    /**
     * Set an list of values that have to be sorted by time
     *
     * @param values the list of Value instances
     */
    void setValues(List<Value> values) {
        this.values = values;
    }

    /**
     * Adds the value.
     *
     * @param value the value
     */
    public void addValue(Value value) {
        this.values.add(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Counter counter = (Counter) o;
        return new EqualsBuilder()
                .append(getCounterName(), counter.getCounterName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getCounterName())
                .toHashCode();
    }
}
