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
package io.softwareecg.collector.source.unix.executor.local;

import io.softwareecg.collector.source.unix.UnixSourceConfig;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Executes a command as a local process. The method {@link #executeCommand} returns after the process has finished.
 * <p>
 * Created by l.steens on 22.07.15.
 */
public class SyncLocalExecutor implements CommandExecutor {

    @Override
    public String executeCommand(String terminalCommand, UnixSourceConfig sourceConfig) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(terminalCommand.split(" "));
        Process proc = pb.start();

        StringBuilder sb = new StringBuilder();

        //read from process stdout
        InputStream stdin = proc.getInputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stdin.read(buffer)) > 0) {
            sb.append(new String(buffer, 0, read));
        }

        return sb.toString();
    }
}
