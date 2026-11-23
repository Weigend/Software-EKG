package io.softwareecg.awb.metadata.model;

import java.time.Instant;

/**
 * One entry in the history of a data source: a change of its state.
 *
 * @param time    when it happened
 * @param state   the state the source went into
 * @param message details, e.g. the error; may be {@code null}
 */
public record CollectionSourceEvent(Instant time, String state, String message) {
}
