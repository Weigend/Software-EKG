package io.softwareecg.awb.importer.adapter.prometheus.client.model.api;


/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public class ApiResult {

    private ResultStatus status;

    private ErrorType errorType;

    private String error;

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ApiResult{" +
                "status=" + status +
                ", errorType=" + errorType +
                ", error='" + error + '\'' +
                '}';
    }
}
