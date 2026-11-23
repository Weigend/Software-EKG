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
package io.softwareecg.awb.timeseries;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A value at a specific timestamp time.
 *
 */
public class Value implements Comparable<Value>, Serializable {

    private static final long serialVersionUID = -4300246192241166788L;

    /**
     * The value.
     */
    private double value;

    /**
     * The timestamp.
     */
    private long timestamp;

    /**
     * Creates a counter.
     *
     * @param timestamp  the record timestamp.
     * @param value the value.
     */
    public Value(long timestamp, double value) {
        this.value = value;
        this.timestamp = timestamp;
    }

    /**
     * Copy.
     *
     * @param v value to copy
     */
    public Value(Value v) {
        this.value = v.value;
        this.timestamp = v.timestamp;
    }

    /**
     * Getter for property 'value'.
     *
     * @return Value for property 'value'.
     */
    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void addAndSum(double valueToAdd) {
        this.value += valueToAdd;
    }

    private transient int avgCount = 1;

    public void addAndAvg(double valueToAdd) {
        avgCount++;
        this.value += (valueToAdd - this.value) / avgCount;
    }


    /**
     * Getter for property 'timestamp'.
     *
     * @return Value for property 'timestamp'.
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(Value v) {
        return Long.compare(timestamp, v.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Value value1)) {
            return false;
        }
        return new EqualsBuilder()
                .append(getValue(), value1.getValue())
                .append(getTimestamp(), value1.getTimestamp())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getValue())
                .append(getTimestamp())
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("date", Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC")).toString())
                .append("value", value)
                .toString();
    }



}
