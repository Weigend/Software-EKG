package io.softwareecg.awb.timeseries.port.query.update;

import io.softwareecg.awb.timeseries.schema.EkgSchemaField;

/**
 * Update parameter that used to specify an atomic update
 * of a single field. If multiple fields should update
 * a collection of these params are necessary.
 *
 * @since 25.11.2018
 */
public class FieldUpdateParam {

    private final EkgSchemaField field;

    private final FieldUpdateAction action;

    private final Object value;

    public FieldUpdateParam(EkgSchemaField field, FieldUpdateAction action, Object value) {
        this.field = field;
        this.action = action;
        this.value = value;
    }

    public EkgSchemaField getField() {
        return field;
    }

    public FieldUpdateAction getAction() {
        return action;
    }

    public Object getValue() {
        return value;
    }
}
