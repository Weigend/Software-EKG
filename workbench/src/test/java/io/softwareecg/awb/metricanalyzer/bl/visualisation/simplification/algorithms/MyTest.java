package io.softwareecg.awb.metricanalyzer.bl.visualisation.simplification.algorithms;

import io.softwareecg.awb.sdk.datamodel.Value;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * todo insert comment here
 *
 * @since 12.08.2019
 */
public class MyTest {

    @Test
    public void test() {


        List<Value> values = new ArrayList<>();

        values.add(new Value(1, 0));
        values.add(new Value(2, 1));
        values.add(new Value(2, 0));
        values.add(new Value(2, 1));
        values.add(new Value(3, 0));
        values.add(new Value(3, 1));
        values.add(new Value(3, 2));
        values.add(new Value(3, 1));
        values.add(new Value(3, 2));
        values.add(new Value(4, 2));
        values.add(new Value(4, 3));
        values.add(new Value(4, 2));



        double valueAtPreviousTimestamp = 0;
        Value valueWithHighestDiff = null;
        double lastLoopDiff = Double.NaN;
        long lastLoopTimestamp = -1;
        List<Value> normalizedValues = new ArrayList<>();

        for (Value value : values) {
            if (value.getTimestamp() != lastLoopTimestamp) {
                lastLoopTimestamp = value.getTimestamp();
                lastLoopDiff = Double.NaN;

                if (valueWithHighestDiff == null) {
                    valueWithHighestDiff = value;
                } else {
                    normalizedValues.add(valueWithHighestDiff);
                    valueAtPreviousTimestamp = valueWithHighestDiff.getValue();
                }
            }

            double diff = Math.abs(valueAtPreviousTimestamp - value.getValue());
            if (Double.isNaN(lastLoopDiff) || lastLoopDiff < diff || (lastLoopDiff == diff && valueWithHighestDiff.getValue() < value.getValue())) {
                lastLoopDiff = diff;
                valueWithHighestDiff = value;
            }
        }

        if (valueWithHighestDiff != null && !normalizedValues.get(normalizedValues.size() - 1).equals(valueWithHighestDiff)) {
            normalizedValues.add(valueWithHighestDiff);
        }


        int i = 0;

    }
}
