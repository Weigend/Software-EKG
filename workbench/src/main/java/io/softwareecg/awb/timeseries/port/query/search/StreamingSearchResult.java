package io.softwareecg.awb.timeseries.port.query.search;

import io.softwareecg.awb.timeseries.port.query.status.QueryStatus;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

/**
 * Response wrapper that encapsulate a data stream
 * with additional status information beside of it.
 *
 * @since 17.09.2018
 */
public record StreamingSearchResult<T>(QueryStatus queryStatus, Stream<T> resultStream) {

    public StreamingSearchResult {
        Validate.notNull(resultStream);

    }
}
