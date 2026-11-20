package io.softwareecg.awb.project.api;

import java.time.Instant;

/**
 * Tells whether data is being collected for a project - continuously, from its data sources -
 * and how that is going. Views that show a project use it to show that something is running.
 * <p>
 * Implemented by the data source module. Where that module is not part of the application,
 * there is no implementation and nothing is collected.
 */
public interface ProjectDataCollection {

    /**
     * @param projectName the name of the project
     * @return the state of the data collection of the project; never {@code null}. A project
     *         without data sources has the state {@link State#NONE}.
     */
    Status statusOf(String projectName);

    /**
     * What the data collection of a project is doing, all its sources taken together.
     */
    enum State {

        /** the project has no data sources */
        NONE(""),

        /** values arrive and are stored */
        RUNNING("live"),

        /** connecting, or connected and waiting for the first values */
        WAITING("connecting"),

        /** at least one source cannot be reached, delivers nothing or cannot be stored */
        ERROR("error"),

        /** all sources are switched off */
        PAUSED("paused");

        private final String label;

        State(String label) {
            this.label = label;
        }

        /** A short word for the state, as shown next to the project. */
        public String label() {
            return label;
        }
    }

    /**
     * @param state       the state, all sources of the project taken together
     * @param sourceCount how many data sources the project has
     * @param lastValue   the time of the newest value collected since the application started,
     *                    or {@code null}
     * @param detail      what is wrong if the state is {@link State#ERROR}, otherwise {@code null}
     */
    record Status(State state, int sourceCount, Instant lastValue, String detail) {

        public static final Status NONE = new Status(State.NONE, 0, null, null);
    }
}
