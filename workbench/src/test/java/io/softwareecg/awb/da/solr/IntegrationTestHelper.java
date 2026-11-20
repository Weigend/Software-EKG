//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.da.solr;



/**
 * This contains helper methods for easier testing with solr.
 *
 */
public final class IntegrationTestHelper {
    private IntegrationTestHelper() {
    }

//    /**
//     * Cleanup the solr storage. It will removes all entries.
//     *
//     * @param serverLocator the solr server locator.
//     * @throws IOException         in case of io errors while
//     * @throws SolrServerException in case of io errors while
//     */
//    public static void cleanUpSolr(RepositoryClientProvider serverLocator) throws IOException, SolrServerException {
//        SolrClient solrServer = serverLocator.getSolrClient();
//        solrServer.deleteByQuery("*:*");
//        solrServer.commit();
//    }
//
//
//    /**
//     * Imports some {@link TimeSeries} into solr.
//     *
//     * @param serverLocator the solr server locator
//     * @throws IOException         in case of io errors while importing
//     * @throws SolrServerException in case of io errors while importing
//     */
//    public static void importTimeSeries(RepositoryClientProvider serverLocator) throws IOException, SolrServerException {
//        SolrClient client = serverLocator.getSolrClient();
//        client.addBeans(Arrays.asList(
//                new TimeSeries("a", "b", "c", "d", "e", "f", "g",
//                        "h", "i" ,"j" ,"k", Arrays.asList(new Value(1, Instant.now().toEpochMilli()))),
//
//                new TimeSeries("a1", "b1", "c1", "d1", "e1", "f1", "g1",
//                        "h1", "i1", "j1", "k1", Arrays.asList(new Value(1, Instant.now().toEpochMilli())))
//        ).stream().map(TimeSeriesMapper::createTimeSeriesEt).iterator());
//        client.commit();
//    }
}
