package io.softwareecg.collector.repository.rdb.api.schema;

import static io.softwareecg.collector.repository.rdb.api.schema.Field.Multiplicity.SINGLE_VALUE;

/**
 * The EkgSchemaField class defines constants for each attribute field that can
 * occur in a document stored in an EKG repository. Each field is described with
 * it's name and the Multiplicity (single value or multi value field).
 *
 * @since 06.08.2018
 */
public enum EkgSchemaField implements Field {

    //================================================================================================================
    //  common fields
    //================================================================================================================

    /**
     * The unique id of the document in the database specified
     */
    ID("id", SINGLE_VALUE),

    /**
     * fixed document type for multi-datatype tables that specifies that
     * this bean(document)
     */
    DOC_TYPE("type", SINGLE_VALUE),


    //================================================================================================================
    //  project fields
    //================================================================================================================

    /**
     * The name of the project (should be unique)
     */
    PROJECT_NAME("project_name", SINGLE_VALUE),

    /**
     * A description text that provides more details to the project
     */
    PROJECT_DESCRIPTION("project_description", SINGLE_VALUE),

    /**
     * A boolean flag that indicates if the project queries time series data
     * in real time from foreign databases or fetch it from the EKG repository itself
     */
    PROJECT_USE_SPLIT_SOURCE("project_useSplitSource", SINGLE_VALUE),

    /**
     * representation of the ProjectType that can be null or
     * one of the following strings:
     * <ul>
     *     <li>CLASSIC</li>
     *     <li>HYBRID</li>
     *     <li>CLOUD_NATIVE</li>
     * </ul>
     */
    PROJECT_FLAVOR("project_flavor", SINGLE_VALUE),

    /**
     * A string representation of the CloudPlatformType that can be null or
     * one of the following strings:
     * <ul>
     *     <li>NONE</li>
     *     <li>OPEN_SHIFT</li>
     *     <li>KUBERNETES</li>
     *     <li>OTHER</li>
     * </ul>
     */
    PROJECT_CN_TYPE("project_cn_type", SINGLE_VALUE),


    /**
     * The id of the importer that has initial created this
     * project (creation in import dialog)
     *
     * The id can used to filter projects in import dialogs if a specific importer
     * enforces to import new only in projects with it's own alias mapping.
     */
    PROJECT_IMPORTER_ID("project_importerId", SINGLE_VALUE),

    /**
     * An enum value that indicated ith the time series that belongs to the project
     * are from EKG default type (domain of IT/Software systems), generic (can used by any) or importer specific
     * DEFAULT; GENERIC; SPECIALIZED (see ProjectTimeSeriesType enum)
     */
    PROJECT_TIMESERIES_TYPE("project_timeseries_type", SINGLE_VALUE),

    /**
     * the optional alias names if project specific mapping is in use
     */
    PROJECT_DIM_ALIAS_HOSTGROUP("project_dimension_alias_hostGroup", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_HOST("project_dimension_alias_host", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_NAMESPACE("project_dimension_alias_namespace", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_SERVICE("project_dimension_alias_service", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_POD("project_dimension_alias_pod", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_CONTAINER("project_dimension_alias_container", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_MEASUREMENT("project_dimension_alias_measurement", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_PROCESS("project_dimension_alias_process", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_METRICGROUP("project_dimension_alias_metricGroup", SINGLE_VALUE),
    PROJECT_DIM_ALIAS_METRICNAME("project_dimension_alias_metricName", SINGLE_VALUE),


    //================================================================================================================
    //  time series fields
    //================================================================================================================

    TS_HOST_GROUP_NAME("ts_hostGroup", SINGLE_VALUE),

    TS_HOST_NAME("ts_host", SINGLE_VALUE),

    TS_NAMESPACE_NAME("ts_namespace", SINGLE_VALUE),

    TS_SERVICE_NAME("ts_service", SINGLE_VALUE),

    TS_POD_NAME("ts_pod", SINGLE_VALUE),

    TS_CONTAINER_NAME("ts_container", SINGLE_VALUE),

    TS_MEASUREMENT("ts_measurement", SINGLE_VALUE),

    /**
     * The id of the process group the process belongs to in the collector configuration.
     */
    TS_PROCESS_GROUP_NAME("ts_processGroup", SINGLE_VALUE),

    TS_PROCESS_NAME("ts_process", SINGLE_VALUE),

    TS_METRIC_GROUP("ts_metricGroup", SINGLE_VALUE),

    TS_METRIC_NAME("ts_metricName", SINGLE_VALUE),

    TS_DATA("ts_data", SINGLE_VALUE),

    TS_DATA_AMOUNT_VALUES("ts_data_amountValues", SINGLE_VALUE),

    TS_REMOTE_SERIES_KEY("ts_remoteSeriesKey", SINGLE_VALUE),

    TS_AGGREGATION_LEVEL("ts_ag", SINGLE_VALUE),

    TS_START("ts_start", SINGLE_VALUE),

    TS_STOP("ts_end", SINGLE_VALUE),

    TS_EXCLUDE("ts_exclude", SINGLE_VALUE),

    TS_IMPORT_DATE("ts_importDate", SINGLE_VALUE),

    TS_IS_MERGED("ts_isMerged", SINGLE_VALUE),

    TS_GROUP_KEY_HASH("ts_groupKeyHash", SINGLE_VALUE),

    //================================================================================================================
    //  types fields
    //================================================================================================================

    /**
     * PersistedField with unique id of the types
     */
    REPOSITORY_ID("id", SINGLE_VALUE),

    /**
     * PersistedField with the name of the project this types belongs to
     */
    REPOSITORY_ACCORDING_PROJECT("repository_accordingProject", SINGLE_VALUE),

    /**
     * PersistedField with the name of the types
     */
    REPOSITORY_NAME("repository_name", SINGLE_VALUE),

    /**
     * PersistedField with the types type (remote Solr classic, remote Solr cloud, remote ElasticSearch, local Solr)
     */
    REPOSITORY_DB_TYPE("repository_dbType", SINGLE_VALUE),

    /**
     * PersistedField with the types url used to fetch the data
     */
    REPOSITORY_URL("repository_url", SINGLE_VALUE),

    /**
     * PersistedField with the types url used to fetch the data
     */
    REPOSITORY_INDEX_NAME("repository_dbIndexName", SINGLE_VALUE),

    /**
     * PersistedField with a flag if the types is an import source
     */
    REPOSITORY_IS_IMPORTSOURCE("repository_isSource", SINGLE_VALUE),

    /**
     * PersistedField that stores the authentication type of the types (NONE, API_KEY, USER_PASSWORD)
     */
    REPOSITORY_AUTH_TYPE("repository_authType", SINGLE_VALUE),

    /**
     * PersistedField that stores the user name if credentials are required
     */
    REPOSITORY_USER("repository_user", SINGLE_VALUE),

    /**
     * PersistedField stores the user password or API key (depending of auth type that is in use)
     */
    REPOSITORY_PASSWORD("repository_pass", SINGLE_VALUE),


    //================================================================================================================
    //  bookmark fields
    //================================================================================================================

    /**
     * The name of the bookmark that will displayed in the UI
     */
    BOOKMARK_NAME("bookmarkName", SINGLE_VALUE),

    /**
     * The description of the bookmark that provides further information's about the bookmark
     */
    BOOKMARK_DESCRIPTION("bookmarkDescription", SINGLE_VALUE),

    /**
     * The JSON serialized protocol of each chart action that were handled by the ProtocolManager
     */
    BOOKMARK_PROTOCOL("bookmarkProtocol", SINGLE_VALUE),

    /**
     * The timezone setting the user currently uses
     */
    BOOKMARK_TIME_ZONE("bookmarkTimeZone", SINGLE_VALUE);


    //================================================================================================================
    //  business logic
    //================================================================================================================

    private final String name;

    private final Multiplicity multiplicity;

    /**
     * Constructs a constant.
     *
     * @param name         Name of the solr field
     * @param multiplicity Multiplicity of the field
     */
    EkgSchemaField(String name, Multiplicity multiplicity) {
        this.name = name;
        this.multiplicity = multiplicity;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

}