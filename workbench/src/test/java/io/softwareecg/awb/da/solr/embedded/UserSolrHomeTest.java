package io.softwareecg.awb.da.solr.embedded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserSolrHomeTest {

    @TempDir
    Path directory;

    @Test
    void configurationIsBroughtUpToDateAndTheDataOfTheUserIsLeftAlone() throws Exception {
        Path template = template("schema 2");
        Path home = directory.resolve("home");
        write(home.resolve("ekgdata/conf/schema.xml"), "schema 1");
        write(home.resolve("ekgdata/data/index/segments_1"), "the index of the user");

        UserSolrHome.prepare(template, home);

        assertThat(home.resolve("solr.xml")).hasContent("<solr/>");
        assertThat(home.resolve("ekgdata/core.properties")).exists();
        assertThat(home.resolve("ekgdata/conf/schema.xml")).as("a new version brings its configuration").hasContent("schema 2");
        assertThat(home.resolve("ekgdata/data/index/segments_1")).hasContent("the index of the user");
    }

    @Test
    void anEmptyHomeGetsTheConfigurationAndNoData() throws Exception {
        Path home = directory.resolve("fresh/solr");

        UserSolrHome.prepare(template("schema"), home);

        assertThat(home.resolve("ekgdata/conf/schema.xml")).exists();
        assertThat(home.resolve("ekgdata/data")).doesNotExist();
    }

    @Test
    void anIndexLeftBesideTheApplicationIsMovedOnceAndNeverOverAnExistingOne() throws Exception {
        Path template = template("schema");
        write(template.resolve("ekgdata/data/index/segments_7"), "left behind in the build tree");
        Path home = directory.resolve("home");

        UserSolrHome.prepare(template, home);

        assertThat(home.resolve("ekgdata/data/index/segments_7")).hasContent("left behind in the build tree");
        assertThat(template.resolve("ekgdata/data")).as("moved, not copied").doesNotExist();

        // a later index beside the application does not replace the one of the user
        write(template.resolve("ekgdata/data/index/segments_9"), "another one");
        UserSolrHome.prepare(template, home);

        assertThat(home.resolve("ekgdata/data/index/segments_7")).exists();
        assertThat(home.resolve("ekgdata/data/index/segments_9")).doesNotExist();
    }

    private Path template(String schema) throws Exception {
        Path template = directory.resolve("template");
        write(template.resolve("solr.xml"), "<solr/>");
        write(template.resolve("ekgdata/core.properties"), "name=ekgdata");
        write(template.resolve("ekgdata/conf/schema.xml"), schema);
        return template;
    }

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
