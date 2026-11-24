package io.softwareecg.awb.repository.adapter.elasticsearch.util;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import io.softwareecg.awb.repository.persistence.schema.EkgSchemaField;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.softwareecg.awb.repository.adapter.elasticsearch.util.FieldType.*;
import static io.softwareecg.awb.repository.persistence.schema.EkgSchemaField.*;

/**
 * The Software EKG schema definition as code.
 * This builder provides the schema as a map of ElasticSearch property definitions
 * that can be used for programmatic index creation with the ElasticSearch Java client.
 *
 * @since 21.09.2018
 */
public class SchemaBuilder {

    /**
     * The default name of the single-type per index enforced since ES v6.
     * Kept as a constant for backward compatibility; ES 8+ no longer uses document types.
     */
    public static final String DOC_TYPE_KEY = "doc";

    /**
     * Builds and provides the Software-EKG repository schema as a map of ElasticSearch property definitions.
     *
     * @return the schema as a map of field name to Property
     */
    public static Map<String, Property> buildSchema() {

        Map<String, Property> properties = new LinkedHashMap<>();

        //==================================================================================================
        // common fields
        //==================================================================================================

        defineField(properties, ID, KEYWORD, true, true, true);

        defineField(properties, DOC_TYPE, KEYWORD, true, true, true);

        //==================================================================================================
        // project type
        //==================================================================================================

        // --- the unique name of the project for a single ElasticSearch index ---
        defineField(properties, PROJECT_NAME, KEYWORD, true, true, true);

        // --- the description that details the content of the project ---
        defineField(properties, PROJECT_DESCRIPTION, TEXT, false, true, false);

        // --- true = SPLIT_SOURCE_PROJECT, false = LOCAL_PROJECT (see ProjectType enum) ---
        defineField(properties, PROJECT_USE_SPLIT_SOURCE, BOOLEAN, false, true, false);

        // --- CLASSIC; HYBRID; CLOUD_NATIVE (see ProjectFlavor enum) ---
        defineField(properties, PROJECT_FLAVOR, KEYWORD, true, true, true);

        // --- DEFAULT; GENERIC; SPECIALIZED (see ProjectTimeSeriesType enum) ---
        defineField(properties, PROJECT_TIMESERIES_TYPE, KEYWORD, true, true, true);

        // --- NONE; OPEN_SHIFT; KUBERNETES; OTHER (see CloudPlatformType enum) ---
        defineField(properties, PROJECT_CN_TYPE, KEYWORD, true, true, true);

        // --- The id of the importer the that is allowed to import data to this project ---
        defineField(properties, PROJECT_IMPORTER_ID, KEYWORD, true, true, true);

        // --- project specific dimension alias (optional filled) ---
        defineField(properties, PROJECT_DIM_ALIAS_HOSTGROUP, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_HOST, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_NAMESPACE, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_SERVICE, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_POD, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_CONTAINER, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_MEASUREMENT, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_PROCESS, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_METRICGROUP, KEYWORD, false, true, false);
        defineField(properties, PROJECT_DIM_ALIAS_METRICNAME, KEYWORD, false, true, false);

        //==================================================================================================
        //  types config used either for EKG repositories and also for importers remote data sources
        //==================================================================================================

        defineField(properties, REPOSITORY_IS_IMPORTSOURCE, BOOLEAN, true, true, false);

        defineField(properties, REPOSITORY_AUTH_TYPE, KEYWORD, false, true, false);

        defineField(properties, REPOSITORY_NAME, KEYWORD, true, true, true);

        defineField(properties, REPOSITORY_DB_TYPE, KEYWORD, true, true, true);

        defineField(properties, REPOSITORY_URL, KEYWORD, false, true, false);

        defineField(properties, REPOSITORY_INDEX_NAME, KEYWORD, false, true, false);

        defineField(properties, REPOSITORY_USER, KEYWORD, false, true, false);

        defineField(properties, REPOSITORY_PASSWORD, KEYWORD, false, true, false);

        defineField(properties, REPOSITORY_ACCORDING_PROJECT, KEYWORD, true, true, false);

        defineField(properties, REPOSITORY_SSO_TOKEN, KEYWORD, false, true, false);

        //==================================================================================================
        //  time-series data types
        //==================================================================================================

        // classic only
        defineField(properties, TS_HOST_GROUP_NAME, KEYWORD, true, true, true);

        defineField(properties, TS_HOST_NAME, KEYWORD, true, true, true);

        // cloud native only
        defineField(properties, TS_NAMESPACE_NAME, KEYWORD, true, true, true);

        defineField(properties, TS_SERVICE_NAME, KEYWORD, true, true, true);

        defineField(properties, TS_POD_NAME, KEYWORD, true, true, true);

        defineField(properties, TS_CONTAINER_NAME, KEYWORD, true, true, true);

        // common dimensions
        defineField(properties, TS_MEASUREMENT, KEYWORD, true, true, true);

        defineField(properties, TS_PROCESS_GROUP_NAME, KEYWORD, true, true, true);

        defineField(properties, TS_PROCESS_NAME, KEYWORD, true, true, true);

        defineField(properties, TS_METRIC_GROUP, KEYWORD, true, true, true);

        defineField(properties, TS_METRIC_NAME, KEYWORD, true, true, true);

        // metadata of the time-series
        defineField(properties, TS_REMOTE_SERIES_KEY, TEXT, false, true, false);

        defineField(properties, TS_IMPORT_DATE, LONG, true, true, false);

        defineField(properties, TS_DATA, BINARY, false, true, false);

        defineField(properties, TS_DATA_AMOUNT_VALUES, INT, false, true, false);

        defineField(properties, TS_START, LONG, true, true, true);

        defineField(properties, TS_STOP, LONG, true, true, true);

        defineField(properties, TS_AGGREGATION_LEVEL, KEYWORD, true, true, true);

        defineField(properties, TS_IS_MERGED, KEYWORD, true, true, true);

        defineField(properties, TS_GROUP_KEY_HASH, INT, true, true, true);


        //==================================================================================================
        // bookmark data types
        //==================================================================================================

        defineField(properties, BOOKMARK_NAME, KEYWORD, true, true, false);

        defineField(properties, BOOKMARK_GROUP_NAME, KEYWORD, true, true, false);

        defineField(properties, BOOKMARK_DESCRIPTION, TEXT, false, true, false);

        defineField(properties, BOOKMARK_PROTOCOL, KEYWORD, false, true, false);

        defineField(properties, BOOKMARK_TIME_ZONE, KEYWORD, false, true, false);

        return properties;
    }

    /**
     * Defines a single field and adds it to the properties map.
     *
     * @param properties the map to add the field definition to
     * @param field the EKG repository schema field that should be defined
     * @param type the type of the repository field
     * @param indexed flag that specifies if the field is indexed or not
     * @param stored flag that specifies if the field is stored or not
     * @param isDocValue flag that specifies if the field is a doc-value or not
     */
    public static void defineField(Map<String, Property> properties, EkgSchemaField field, FieldType type,
                                   boolean indexed, boolean stored, boolean isDocValue) {
        properties.put(field.getName(), createProperty(type, indexed, stored, isDocValue));
    }

    private static Property createProperty(FieldType type, boolean indexed, boolean stored, boolean isDocValue) {
        return switch (type) {
            case KEYWORD -> Property.of(p -> p.keyword(k -> k.index(indexed).store(stored).docValues(isDocValue)));
            case TEXT -> Property.of(p -> p.text(t -> t.index(indexed).store(stored)));
            case BOOLEAN -> Property.of(p -> p.boolean_(b -> b.index(indexed).store(stored).docValues(isDocValue)));
            case LONG -> Property.of(p -> p.long_(l -> l.index(indexed).store(stored).docValues(isDocValue)));
            case INT -> Property.of(p -> p.integer(i -> i.index(indexed).store(stored).docValues(isDocValue)));
            case BINARY -> Property.of(p -> p.binary(b -> b));
            case DATE -> Property.of(p -> p.date(d -> d.index(indexed).store(stored).docValues(isDocValue)));
        };
    }
}
