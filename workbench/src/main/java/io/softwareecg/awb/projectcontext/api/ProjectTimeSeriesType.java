package io.softwareecg.awb.projectcontext.api;

/**
 * This enum defines a set of time series types that
 * will use use to decide if the project is supported by a importer
 * and will shown in the selection.
 *
 * @since 14.07.2018
 */
public enum ProjectTimeSeriesType {

    /**
     * project with time series of that type are only supported by importers
     * that uses the EKG default filter dimensions (importers for IT/Software systems)
     */
    DEFAULT,

    /**
     * projects with time series that can filled by each importer
     */
    GENERIC,

    /**
     * project with time series that are binded to a special importer because the importer
     * defines it's own filter dimensions. These project will disabled for selection at
     * import dialogs of all other importers.
     */
    SPECIALIZED
}
