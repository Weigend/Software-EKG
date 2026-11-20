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
package io.softwareecg.awb.da.solr.embedded;

import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Default implementation for the configuration.
 * <p/>
 * It tries several locations to find the solr core. <ol> <li>Paths given in <code>ekg.solr.baseDir</code> system
 * property. This property allows multiple paths separated with : or ; (depends on os)</li>
 * <li><code>target/solr</code></li> <li><code>solr/</code></li> </ol>
 *
 */
@Singleton
public class DefaultSolrConfig implements SolrConfiguration {

    public static final String SOLR_BASE_DIR_PROPERTY = "ekg.solr.baseDir";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolrConfig.class);

    //private static final List<String> DEFAULT_SEARCH_PATHS = asList("C:/Users/stefan.krause/Documents/Software-EKG AWB 6.1_beta4/solr/" );
    private static final List<String> DEFAULT_SEARCH_PATHS = asList(
            "build/solr/", "solr/", "../solr/", "target/solr/"
    );

    protected static final String PATH_SEPARATOR = System.getProperty("path.separator");

    private File solrConfig;

    @Override
    public File getSolrConfig() {
        if (solrConfig == null) {
            findSolrConfig();
        }
        return solrConfig;
    }

    @Override
    public File getSolrHomeDir() {
        if (solrConfig == null) {
            findSolrConfig();
        }
        return solrConfig.getParentFile();
    }

    /**
     * Find the solr.xml path.
     */
    private void findSolrConfig() {
        String externalConfig = System.getProperty(SOLR_BASE_DIR_PROPERTY);
        List<String> searchPaths = new ArrayList<>();
        if (StringUtils.isNotBlank(externalConfig)) {
            searchPaths.addAll(asList(externalConfig.split(PATH_SEPARATOR)));
        }
        addRuntimeSearchPath(searchPaths);
        searchPaths.addAll(DEFAULT_SEARCH_PATHS);
        for (String path : searchPaths) {
            File solrXml = Paths.get(path, "solr.xml").toFile();
            LOGGER.debug("Try to find solr.xml in {}", solrXml.getAbsolutePath());
            if (solrXml.exists()) {
                solrConfig = solrXml.getAbsoluteFile();
                return;
            }
        }
        throw new IllegalStateException("Can not find solr.xml");
    }

    /**
     * Add the Solr directory next to the running classes or application jar.
     * This makes a reactor start independent of Maven's current directory.
     */
    private void addRuntimeSearchPath(List<String> searchPaths) {
        try {
            Path codeLocation = Paths.get(DefaultSolrConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path runtimeDirectory = codeLocation.getParent();
            if (runtimeDirectory != null) {
                searchPaths.add(runtimeDirectory.resolve("solr").toString());
            }
        } catch (NullPointerException | URISyntaxException | IllegalArgumentException exception) {
            LOGGER.debug("Can not derive Solr directory from application location", exception);
        }
    }
}
