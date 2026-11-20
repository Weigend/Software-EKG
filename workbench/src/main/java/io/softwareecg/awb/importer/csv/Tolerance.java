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
package io.softwareecg.awb.importer.csv;

/**
 * Tolerance is needed by the csv importer. It defines a maximum difference between two
 * series values. If the real difference between those values is smaller than the
 * maximum than the two values are seen as equals.
 *
 */
public enum Tolerance {

    /**
     * The none.
     */
    NONE(-1.0f) {
        @Override
        public String toString() {
            return "Off";
        }
    },

    /**
     * The zero.
     */
    ZERO(0.0f) {
        @Override
        public String toString() {
            return "0.0% Tolerance";
        }
    },

    /**
     * The zero point one.
     */
    ZERO_POINT_ONE(0.1f) {
        @Override
        public String toString() {
            return "0.1% Tolerance";
        }
    },

    /**
     * The zero point two.
     */
    ZERO_POINT_TWO(0.2f) {
        @Override
        public String toString() {
            return "0.2% Tolerance";
        }
    },

    /**
     * The zero point five.
     */
    ZERO_POINT_FIVE(0.5f) {
        @Override
        public String toString() {
            return "0.5% Tolerance";
        }
    },

    /**
     * The one.
     */
    ONE(1.0f) {
        @Override
        public String toString() {
            return "1.0% Tolerance";
        }
    },

    /**
     * The two.
     */
    TWO(2.0f) {
        @Override
        public String toString() {
            return "2.0% Tolerance";
        }
    },

    /**
     * The five.
     */
    FIVE(5.0f) {
        @Override
        public String toString() {
            return "5.0% Tolerance";
        }
    };

    /**
     * The threshold in percent.
     */
    private final float thresholdInPercent;

    /**
     * Constructor that defines the threshold in percent.
     *
     * @param thresholdInPercent the threshold in percent.
     */
    Tolerance(float thresholdInPercent) {
        this.thresholdInPercent = thresholdInPercent;
    }


    /**
     * FIXME:   UNUSED
     * <p>
     * Who:     Fabian Reißmann
     * Date:    28.11.2014
     * Time:    09:45
     *
     * Getter for property 'thresholdInPercent'.
     *
     * @return Value for property 'thresholdInPercent'.
     */
    public float getThresholdInPercent() {
        return thresholdInPercent;
    }

}
