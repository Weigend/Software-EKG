//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-02-17 11:56:17 +0100 (Mi, 17 Feb 2010) $
//       revision:      $Revision: 359 $
//       description:   JMX Performance MetricDefinition Access
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source.rlog;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.common.AbstractCounterSource;
import org.apache.commons.io.FileUtils;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Requests a counter from a logfile.
 */
public class LogfileRemoteAccess extends AbstractCounterSource {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();


    /** Date format for directory information. */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.ENGLISH);

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.common.AbstractCounterSource#querySingleCounter(CounterDescriptor, MetricDefinition)
     */
    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {

        ContentExchange contentExchange = new ContentExchange();
        String logfile = metricDefinition.getDefinition();
        try (FileOutputStream fileOutputStream = new FileOutputStream(getFile(descriptor, metricDefinition))) {
            HttpClient httpClient = new HttpClient();
            httpClient.start();
            httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
            contentExchange.setURL("https://" + descriptor.getNodeAddress() + ":19888");

            contentExchange.addRequestHeader("logfile", logfile);

            LOGGER.debug("Requesting file: \"" + logfile + "\"");

            httpClient.send(contentExchange);

            int rc = contentExchange.waitForDone();
            if (rc != ContentExchange.STATUS_COMPLETED) {
                return -1;
            }

            contentExchange.writeResponseBytesTo(fileOutputStream);
            httpClient.stop();

            return fileOutputStream.getChannel().size();

        } catch (Exception e) {
            LOGGER.warn("cant acquire remote file", e);
            return -1; // ok for the collector

        }
    }

    /**
     * 192.20.20.1 -> 20.10.2010 -> WSM -> Manager.log
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @param metricDefinition the metricDefinition.
     * @return a file.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private File getFile(CounterDescriptor descriptor, MetricDefinition metricDefinition) throws IOException {
        String log = CollectorProperties.getLogdir();
        String filePath = log + "/logfiles/" + descriptor.getNodeName() + "/" +
                ( (DateFormat) DATE_FORMAT.clone()).format(new Date()) + "/" + metricDefinition.getLabel();
        File directory = new File(new File(filePath).getParentFile().getAbsolutePath());
        FileUtils.forceMkdir(directory);
        return new File(filePath).getAbsoluteFile();
    }

    @Override
    public String getId() {
        return "rlog";
    }
}
