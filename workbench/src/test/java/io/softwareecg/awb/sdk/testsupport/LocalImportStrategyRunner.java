package io.softwareecg.awb.sdk.testsupport;

import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.serviceloader.ServiceLoaderLookupStrategy;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventBusListener;
import io.softwareecg.awb.sdk.datamodel.TimeSeries;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.testsupport.mocks.*;

import java.util.EventObject;
import java.util.List;

/**
 * This runner is a lightweight executor for LocalImportStrategy implementations
 * it can use to run the strategy for unit test purposes and also as bridge to
 * a remote (standalone) Solr instance.
 * The second option is the recommend way to test the import module behaviour manually.
 * Connect the AWB to the remote Solr and create an empty project that get filled by this
 * bridge with data.
 *
 * @since 24.03.2020
 */
@SuppressWarnings({"unused"})
public class LocalImportStrategyRunner {

    private LocalProjectImportStrategy strategyToTest;

    private String projectName;

    private int solrPort = 8983;

    private String solrHostName = "localhost";

    private String solrCollection = "ekgdata";

    private boolean writeToRemoteSolr = false;

    private boolean useHttps = false;

    static {
        ServiceLoaderLookupStrategy lookupStrategy = new ServiceLoaderLookupStrategy();
        lookupStrategy.init(EventBus.class, new NoOpEventBus());
        Lookup.init(lookupStrategy);
    }

    /**
     * Private constructor because this class should only created
     * by the builder instance.
     */
    private LocalImportStrategyRunner() {
        // NoOp
    }

    private static final class NoOpEventBus implements EventBus<EventObject> {

        @Override
        public boolean publish(EventObject event) {
            return false;
        }

        @Override
        public <E extends EventObject> void subscribe(Class<E> type, EventBusListener<? super E> listener) {
        }

        @Override
        public <E extends EventObject> boolean unsubscribe(Class<E> type, EventBusListener<? super E> listener) {
            return false;
        }
    }

    /**
     * Creates a new builder instance that can use to configure
     * and construct a new LocalImportStrategyRunner via fluent api.
     *
     * @return a new builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Executes the LocalProjectImportStrategy instance using the
     * configured runtime settings. The output data of the strategy will
     * collected and can used for asserts in the tests.
     *
     * @return a list of TimeSeries data the import strategy generates from the source data.
     */
    public List<TimeSeries> runStrategy() {

        CollectorSeriesImportService importService;

        if (writeToRemoteSolr) {
            // this instance will collect the series data locally and also write it to the remote Solr
            importService = new HybridSeriesImportService(
                solrHostName,
                solrPort,
                solrCollection,
                useHttps
            );
        } else {
            // this instance will only collect the imported data locally
            importService = new SimpleCollectorSeriesImportService();
        }

        // execute the strategy
        strategyToTest.createImportTask(importService, new RunnerProjectConfig(projectName)).doImport();

        return importService.getCollectedSeriesData();
    }

    //=================================================================================================================
    //  Builder pattern to create a runner
    //=================================================================================================================

    /**
     * A builder to create a LocalImportStrategyRunner with a couple of
     * default settings and a convenient fluent API.
     */
    public static class Builder {

        private LocalProjectImportStrategy strategyToTest;

        private String projectName;

        private final int solrPort = 8983;

        private String solrHostName = "localhost";

        private String solrCollection = "ekgdata";

        private boolean writeToRemoteSolr = false;

        private boolean useHttps = false;

        /**
         * Set the LocalProjectImportStrategy instance that should run
         * by the LocalImportStrategyRunner.
         *
         * @param strategy the LocalProjectImportStrategy to run
         * @return this builder instance to use it in a fluent way
         */
        public Builder strategyToTest(LocalProjectImportStrategy strategy) {
            this.strategyToTest = strategy;
            return this;
        }

        /**
         * Set the name of the EKG project in the remote Solr database
         * the time series data should assigned to.
         * This will only have any effect if {@link Builder#activateRemoteSolrExport}
         * is called to enable the export feature.
         *
         * This project will not created in the remote database by the runner.
         * Use project managing features of Software-EKG itself to create project.
         *
         * @param projectName the name of the EKG project in the remote database
         * @return this builder instance to use it in a fluent way
         */
        public Builder useProject(String projectName) {
            this.projectName = projectName;
            return this;
        }

        /**
         * Activate the remote export feature of the runner.
         * If this feature is activated the runner tries to reach the configured remote
         * Solr instance with the following behavior:
         * 1.) ping Solr, if not respond an warning will logged and remote export will disabled
         * 2.) if respond delete the series data if some exists in the specified project
         * 3.) run the importer and write the series data simultaneously into the Solr instance
         * 4.) commit at the end of the process.
         *
         * The data will collected locally independent of this feature toggle.
         *
         * @return this builder instance to use it in a fluent way
         */
        public Builder activateRemoteSolrExport() {
            this.writeToRemoteSolr = true;
            return this;
        }

        /**
         * Sets the Solr port that will used if the remote export feature is
         * enabled. Setting the port without export feature activation won't have any effect.
         *
         * Default value: 8983 (Solr standard port)
         *
         * @param port the port the remote Solr is listen on
         * @return this builder instance to use it in a fluent way
         */
        public Builder useSolrPort(int port) {
            this.writeToRemoteSolr = true;
            return this;
        }

        /**
         * Sets the hostname of the node the remote Solr instance is running on.
         * Setting the port without export feature activation won't have any effect.
         *
         * Default value: localhost
         *
         * @param solrHostName the hostname of the Solr instance
         * @return this builder instance to use it in a fluent way
         */
        public Builder useSolrHost(String solrHostName) {
            this.solrHostName = solrHostName;
            return this;
        }

        /**
         * Activate the usage of transport layer encryption via HTTPS.
         * The runner will always ignore the server side certificates, but uses
         * SSL encryption if activated.
         * Setting the port without export feature activation won't have any effect.
         *
         *  Default behaviour: no SSL / plain http
         *
         * @return this builder instance to use it in a fluent way
         */
        public Builder useHttps() {
            this.useHttps = true;
            return this;
        }

        /**
         * The Solr collection that will the exported data will written to.
         * Setting the port without export feature activation won't have any effect.
         *
         * Default value: ekgdata
         *
         * @param solrCollection the name of the target Solr collection
         * @return this builder instance to use it in a fluent way
         */
        public Builder useSolrCollection(String solrCollection) {
            this.solrCollection = solrCollection;
            return this;
        }

        /**
         * Builds a new runner instance that will configured with the
         * specified parameters.
         *
         * @return the created runner instance
         */
        public LocalImportStrategyRunner build() {
            LocalImportStrategyRunner runner = new LocalImportStrategyRunner();

            runner.strategyToTest = strategyToTest;
            runner.projectName = projectName;
            runner.solrPort = solrPort;
            runner.solrHostName = solrHostName;
            runner.solrCollection = solrCollection;
            runner.writeToRemoteSolr = writeToRemoteSolr;
            runner.useHttps = useHttps;

            return runner;
        }
    }
}
