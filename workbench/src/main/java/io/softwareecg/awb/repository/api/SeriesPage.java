package io.softwareecg.awb.repository.api;

import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.List;

/**
 * A page of the series a {@link SeriesSearch} found.
 *
 * @param totalHits  how many stored parts of series match, on all pages
 * @param series     the parts on this page, with all their values
 * @param nextCursor where the next page starts; {@code null} if this is the last one
 */
public record SeriesPage(long totalHits, List<TimeSeries> series, String nextCursor) {
}
