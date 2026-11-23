package io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query;

import java.util.Arrays;

/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public class Matrix {

   private String resultType;

   private MatrixSeries[] result;

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public MatrixSeries[] getResult() {
        return result;
    }

    public void setResult(MatrixSeries[] result) {
        this.result = result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(result).forEach(series -> {
            builder.append('\n');
            builder.append(series.toString());
        });

        builder.append("\n");

        return "Matrix{" +
                "resultType='" + resultType + '\'' +
                ", result=[" + builder + "]}";
    }
}
