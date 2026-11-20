package io.softwareecg.awb.metricanalyzer.bl.bookmarks.et;

/**
 * Concrete bookmark type that represents a bookmark for
 * a simple or complex chart setup that should be view in future.
 * A complex setup means a chart that displays multiple background
 * charts and optional use different aggregations or color settings.
 *
 * This is the default bookmark that is currently supported in the AWB.
 * Stored in the H2 metadata database.
 *
 * @since 20.10.2018
 */
public class MetricBookmark extends Bookmark {

    private String commandProtocol;

    /**
     * Protected constructor due init of Bookmarks through the {@link Builder}.
     */
    public MetricBookmark() {
        super();
    }

    public String getSerializedCommandProtocol() {
        return commandProtocol;
    }

    /**
     * Builder to create a bookmark instance.
     */
    public static class Builder extends Bookmark.Builder<MetricBookmark> {

        private String commandProtocol;

        @Override
        public MetricBookmark build() {
            MetricBookmark bookmark = new MetricBookmark();
            super.build(bookmark);
            bookmark.commandProtocol = commandProtocol;
            return bookmark;
        }

        public Builder withCommandProtocol(String commandProtocol) {
            this.commandProtocol = commandProtocol;
            return this;
        }
    }


}
