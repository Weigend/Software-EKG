package io.softwareecg.awb.importer.adapter.prometheus.client.model.label;

import io.softwareecg.awb.importer.adapter.prometheus.client.model.ApiResult;


/**
 * todo enter comment here
 *
 * @since 12.07.2018
 */
public class ListLabelResult extends ApiResult {

    private String[] data = new String[0];

    public String[] getData() {
        return data;
    }

    public void setData(String[] data) {
        this.data = data;
    }
}
