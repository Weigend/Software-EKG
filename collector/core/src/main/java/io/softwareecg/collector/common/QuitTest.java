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
package io.softwareecg.collector.common;


/**
 * Test class for Test which will disable anoying error messages during exceptional tests.
 * Created by johannes.weigend on 18.04.14.
 */
public class QuitTest {

    /**
     * A quit test executes the runnable and disables all output
     *
     * @param runnable a runnable which will be stated with disabled output streams and logging.
     */
    public QuitTest(Runnable runnable) {
        LoggingControl control = new LoggingControl();
        control.makeQuit();
        try {
            runnable.run();
        } finally {
            control.restore();
        }
    }
}