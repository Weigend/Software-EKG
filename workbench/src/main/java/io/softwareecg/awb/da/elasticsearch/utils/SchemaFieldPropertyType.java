package io.softwareecg.awb.da.elasticsearch.utils;

/**
 * todo insert comment here
 *
 * @since 21.09.2018
 */
public enum SchemaFieldPropertyType {

    FIELD_TYPE("type"),

    INDEXED("index"),

    STORED("store"),

    IS_DOC_VALUE("doc_values");

    private final String type;

    SchemaFieldPropertyType(String type) {
        this.type = type;
    }

    public String getKey() {
       return type;
    }
}
