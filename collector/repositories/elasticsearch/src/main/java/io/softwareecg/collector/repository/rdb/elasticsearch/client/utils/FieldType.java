package io.softwareecg.collector.repository.rdb.elasticsearch.client.utils;

/**
 * todo insert comment here
 *
 * @since 21.09.2018
 */
public enum FieldType {

    TEXT("text"),

    KEYWORD("keyword"),

    BOOLEAN("boolean"),

    BINARY("binary"),

    DATE("date"),

    LONG("long"),

    INT("int");

    private final String type;

    FieldType(String type) {
        this.type = type;
    }

    public String getTypeKey() {
        return type;
    }
}
