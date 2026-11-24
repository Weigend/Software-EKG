//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.source.unix;

import io.softwareecg.collector.config.CollectorJobConfiguration;
import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.*;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin.binder on 02.05.2014.
 */
public final class UnixTestHelper {


    public static final String IO_SOURCE_NO_NFS = "analyzer/iostat.n-arg-not-supported.txt";

    public static final String IO_SOURCE1 = "analyzer/iostat.out.001.txt";

    public static final String IO_SOURCE2 = "analyzer/iostat.out.002.txt";

    public static final String IO_SOURCE3 = "analyzer/iostat.out.003.txt";

    public static final String IO_SOURCE4 = "analyzer/iostat.out.004.txt";

    public static final String IO_SOURCE5 = "analyzer/iostat.out.005.txt";

    public static final String IO_SOURCE6 = "analyzer/iostat.out.006.txt";

    /** The Constant SOURCE1. */
    public  static final String TOP_SOURCE1 = "analyzer/top-output-linux-1.txt";

    /** The Constant SOURCE2. */
    public  static final String TOP_SOURCE2 = "analyzer/top-output-linux-2.txt";

    /** The Constant SOURCE3. */
    public static final String TOP_SOURCE3 = "analyzer/top-output-linux-3.txt";

    /** The Constant SOURCE4. */
    public  static final String TOP_SOURCE4 = "analyzer/top-output-linux-4-fullpath.txt";

    /** The Constant SOURCE5. */
    public  static final String TOP_SOURCE5 = "analyzer/top-output-linux-5-fullpath.txt";

    /** The Constant SOURCE6. */
    public  static final String TOP_SOURCE6 = "analyzer/top-output-linux-6-fullpath.txt";

    /** The Constant SOURCE6. */
    public  static final String TOP_SOURCE7  = "analyzer/top-output-linux-with-errors.txt";

    public  static final String TOP_SOURCE8  = "analyzer/uptime-false.txt";

    public  static final String TOP_SOURCE9  = "analyzer/top-output-linux-1-user.txt";

    public  static final String TOP_SOURCE10 = "analyzer/top-output-linux-leading-error_msg.txt";

    public  static final String DF_SOURCE1 = "analyzer/dfout.txt";

    private UnixTestHelper() {
        //empty
    }

    /**
     * Helper for the Test.
     *
     * @param source the source
     * @return the string[]
     * @throws java.io.IOException Signals that an I/O exception has occurred.
     */
    public static String[] loadResult(String source) throws IOException {
        List<String> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(UnixTestHelper.class.getResourceAsStream(source)));
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            result.add(currentLine);
        }
        return result.toArray(new String[result.size()]);
    }

    public static String loadResultAsString(String source) throws IOException {
        return IOUtils.toString(UnixTestHelper.class.getResourceAsStream(source), StandardCharsets.UTF_8);
    }


    public static MetricGroupInfo getCounterGroupInfo() {
        return new MetricGroupInfo("testGroup", "mock", 1000L, 1000L, null);
    }

    public static ProcessInfo getProcessInfo() {
        return new ProcessInfo("mockProcess", "processLabel", "testSource", null);
    }

    public static HostInfo getHostInfo() {
        return new HostInfo("localhost", "127.0.0.1", "hostGroup", "testuser", "testpwd");
    }

    public static CollectorJobConfiguration getCollectorJobConfig() {
        return TestConfigFactory.createTestConfig(
                getHostInfo(),
                getProcessInfo(),
                getCounterGroupInfo(),
                new MetricDefinition[0]
        );
    }

}
