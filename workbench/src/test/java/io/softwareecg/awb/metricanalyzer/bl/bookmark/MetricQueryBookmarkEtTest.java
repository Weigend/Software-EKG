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
package io.softwareecg.awb.metricanalyzer.bl.bookmark;

import io.softwareecg.awb.bookmark.model.MetricQueryBookmark;
import org.junit.Test;

import java.util.Stack;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test for the {@link MetricQueryBookmark}.
 *
 */
public class MetricQueryBookmarkEtTest {



    @Test
    public void testEquals() throws Exception {
        MetricQueryBookmark et = new MetricQueryBookmark.Builder().build();
        et.setId(null);
        assertThat(et, is(equalTo(et)));
        MetricQueryBookmark et1 = new MetricQueryBookmark.Builder().build();
        et1.setId(null);
        assertThat(et, is(equalTo(et1)));
        assertThat(et, is(not(equalTo(null))));
        assertThat(et, is(not(equalTo(new Object()))));
    }

    public static MetricQueryBookmark.Builder buildMetricQueryBookmark() {
        Stack<MetricQueryBookmark> stack = new Stack();
        stack.push(
                new MetricQueryBookmark.Builder()
                        .withName("Bookmark Name")
                        .build());
        stack.push(
                new MetricQueryBookmark.Builder()
                        .withMetric("abc")
                        .withExclude("exclude")
                        .withExpertMode(true)
                        .withRawQuery("metric:*Heap*")
                        .withMultiMetricMode(false)
                        .withName("Bookmark Name")
                        .build());
        return (MetricQueryBookmark.Builder) new MetricQueryBookmark.Builder()
                .withMetric("abc")
                .withExclude("exclude")
                .withExpertMode(true)
                .withMetricQueryBookmarks(stack)
                .withRawQuery("metric:*Heap*")
                .withMultiMetricMode(false)
                .withName("Bookmark Name");
    }
}
