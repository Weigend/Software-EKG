package io.softwareecg.awb.metadata.model;

import java.time.Instant;

/**
 * A data source of a project as it is stored.
 */
public class CollectionSourceRow {

    /** The identity of the source. */
    public String id;

    /** The project the source feeds. */
    public String projectName;

    /** The kind of source, e.g. {@code prometheus}. */
    public String sourceType;

    /** Where the collection runs, e.g. {@code EMBEDDED} for the collector inside the AWB. */
    public String executionMode;

    /** The address of the source, for display. */
    public String endpoint;

    /** The collector configuration. Contains no credentials, only a placeholder for them. */
    public String config;

    /** The reference of the credentials in the secret store, {@code null} if the source needs none. */
    public String secretRef;

    /**
     * The name under which the series of this source are stored, unique within the project.
     * It keeps the series of two sources apart that monitor systems of the same names. It is
     * given once and never changes, also not when the address of the source does.
     * {@code null} in sources stored before there was such a name.
     */
    public String seriesNamespace;

    /** Whether the source is collected at all; a paused source stays stored. */
    public boolean enabled = true;

    public Instant createdDate;
}
