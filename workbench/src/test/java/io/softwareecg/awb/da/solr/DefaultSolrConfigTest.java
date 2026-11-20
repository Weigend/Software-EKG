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

import io.softwareecg.awb.da.solr.embedded.DefaultSolrConfig;
import io.softwareecg.awb.da.solr.embedded.SolrConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for the {@link DefaultSolrConfig}.
 *
 */
public class DefaultSolrConfigTest {
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");
    private final SolrConfiguration config = new DefaultSolrConfig();

    private String solrBaseDir;

    @Before
    public void setUp() throws Exception {
        this.solrBaseDir = System.getProperty(DefaultSolrConfig.SOLR_BASE_DIR_PROPERTY);
    }

    @After
    public void tearDown() throws Exception {
        if (solrBaseDir != null) {
            System.setProperty(DefaultSolrConfig.SOLR_BASE_DIR_PROPERTY, solrBaseDir);
        } else {
            System.clearProperty(DefaultSolrConfig.SOLR_BASE_DIR_PROPERTY);
        }
    }

    @Ignore("Integration test - requires solr.xml in search paths")
    @Test
    public void testGetSolrConfig() throws Exception {
        assertThat(config.getSolrConfig().toPath(), is(equalTo(Paths.get(config.getSolrHomeDir().toString(), "solr.xml"))));
    }

    @Ignore("Integration test - requires solr.xml in search paths")
    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testGetSolrConfigNoPathsFoundProperty() throws Exception {
        System.clearProperty(DefaultSolrConfig.SOLR_BASE_DIR_PROPERTY);

        List<String> defaultSearchPaths = (List<String>) getInternalState(config, "DEFAULT_SEARCH_PATHS");
        String originalValue = defaultSearchPaths.get(0);
        defaultSearchPaths.set(0, "../target/solr/");
        try {
            config.getSolrConfig();
        } finally {
            defaultSearchPaths.set(0, originalValue);
        }
    }

    /**
     * Helper method to access private fields via reflection (replaces deprecated Mockito Whitebox).
     */
    private static Object getInternalState(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
