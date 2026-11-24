package io.softwareecg.awb.bookmark.api;

import io.softwareecg.awb.query.api.QuerySpec;

/**
 * What a bookmark brings back.
 *
 * @param query     the query of the chart
 * @param chartType the name of the type of the chart; {@code null} or blank if it says none
 */
public record BookmarkContent(QuerySpec query, String chartType) {
}
