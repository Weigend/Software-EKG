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
 * Frequency is needed by the csv importer. It defines how much time has to be
 * between two series values before a new value can be stored.
 *
 */
@SuppressWarnings("all")
public enum Frequency {

    /**
     * The all.
     */
    ALL(1) {
        @Override
        public String toString() {
            return "All";
        }
    },

    /**
     * The one second.
     */
    ONE_SECOND(1000) {
        @Override
        public String toString() {
            return "1s";
        }
    },

    /**
     * The five seconds.
     */
    FIVE_SECONDS(5000) {
        @Override
        public String toString() {
            return "5s";
        }
    },

    /**
     * The ten seconds.
     */
    TEN_SECONDS(10000) {
        @Override
        public String toString() {
            return "10s";
        }
    },

    /**
     * The thirty seconds.
     */
    THIRTY_SECONDS(30000) {
        @Override
        public String toString() {
            return "30s";
        }
    },

    /**
     * The one minute.
     */
    ONE_MINUTE(60000) {
        @Override
        public String toString() {
            return "1 min";
        }
    },

    /**
     * The five minutes.
     */
    FIVE_MINUTES(60000 * 5) {
        @Override
        public String toString() {
            return "5 min";
        }
    },

    /**
     * The thirty minutes.
     */
    THIRTY_MINUTES(60000 * 30) {
        @Override
        public String toString() {
            return "30 min";
        }
    },

    /**
     * The one hour.
     */
    ONE_HOUR(60000 * 60) {
        @Override
        public String toString() {
            return "1 hour";
        }
    };

    /**
     * The milliseconds.
     */
    private final long milliseconds;

    /**
     * Constructor defining the time in ms.
     *
     * @param milliseconds the time.
     */
    private Frequency(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     * Getter for property 'milliseconds'.
     *
     * @return Value for property 'milliseconds'.
     */
    public long getMilliseconds() {
        return milliseconds;
    }
}
