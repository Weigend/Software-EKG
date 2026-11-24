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
package io.softwareecg.awb.query.api;

import io.softwareecg.awb.timeseries.dimension.Process;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test for the {@link QuerySpec.Builder}.
 *
 */
public class QueryContextBuilderTest {

    @Test
    public void testWithProcess() throws Exception {
        QuerySpec query = new QuerySpec.Builder().withProcess(new Process("abc", "")).build();
        assertThat(query.getProcessName(), is(equalTo("abc")));
    }

    @Test
    public void testWithRawQuery() throws Exception {
        QuerySpec query = new QuerySpec.Builder().withRawQuery("a Raw Query").build();
        assertThat(query.getRawQuery(), is(equalTo("a Raw Query")));
    }

    @Test
    public void testBuild() throws Exception {
        QuerySpec.Builder builder = new QuerySpec.Builder().withStop(Long.MAX_VALUE);
        QuerySpec build1 = builder.build();
        QuerySpec build2 = builder.build();
        assertThat(build1, is(notNullValue()));
        assertThat(build2, is(notNullValue()));
        assertThat(build2, is(equalTo(build1)));
        assertThat(build2, is(not(sameInstance(build1))));
    }
}
