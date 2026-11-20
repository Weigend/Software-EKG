package io.softwareecg.awb.datasource.bl;

import java.time.Instant;

/**
 * A data source of an analysis and how its collection is doing, as shown to the user.
 *
 * @param id            the identity of the source
 * @param projectName   the analysis the source feeds
 * @param sourceType    the kind of source, e.g. {@code prometheus}
 * @param executionMode where the collection runs, e.g. {@code EMBEDDED}
 * @param endpoint      the address of the source
 * @param state         what the collection is doing
 * @param detail        what is wrong if the state is an error, otherwise {@code null}
 * @param lastValue     the time of the newest value collected since the AWB started, or {@code null}
 * @param seriesCount   how many series are collected at the moment
 */
public record DataSourceInfo(String id, String projectName, String sourceType, String executionMode,
                             String endpoint, State state, String detail, Instant lastValue, int seriesCount) {

    /**
     * What the collection of a source is doing.
     */
    public enum State {

        /** switched off by the user; nothing is collected */
        PAUSED("paused"),

        /** connecting and finding out what there is to collect */
        STARTING("starting"),

        /** connected, the first values have not arrived yet */
        WAITING("waiting for data"),

        /** values arrive and are stored */
        RUNNING("running"),

        /** the source cannot be reached, delivers nothing or the values cannot be stored */
        ERROR("error"),

        /** not collecting although not paused, e.g. while the AWB shuts down */
        STOPPED("stopped");

        private final String label;

        State(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
