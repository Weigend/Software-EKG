package io.softwareecg.awb.importer.prometheus.client.model.api;

/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public enum ResultStatus {

    SUCCESS,

    ERROR;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
