package io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query;

import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.ApiResult;

/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public class QueryResult extends ApiResult {

    private Matrix data;

    public Matrix getData() {
        return data;
    }

    public void setData(Matrix data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "data=" + data +
                "} " + super.toString();
    }
}
