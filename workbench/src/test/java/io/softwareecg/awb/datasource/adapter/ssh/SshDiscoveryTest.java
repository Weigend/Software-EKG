package io.softwareecg.awb.datasource.adapter.ssh;

import io.softwareecg.awb.datasource.adapter.ssh.SshDiscovery.HostCheck;
import io.softwareecg.awb.datasource.adapter.ssh.SshDiscovery.ProcessCandidate;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.softwareecg.awb.datasource.adapter.ssh.SshDiscovery;
import io.softwareecg.awb.datasource.adapter.ssh.SshPlan;
public class SshDiscoveryTest {

    private static final String TOP = """
            top - 15:42:21 up 2 days,  5:09,  1 user,  load average: 1.24, 1.28, 1.68
            Tasks: 771 total,   1 running, 767 sleeping,   0 stopped,   3 zombie
            %Cpu(s):  1.6 us,  0.9 sy,  0.0 ni, 97.3 id,  0.2 wa,  0.0 hi,  0.0 si,  0.0 st
            MiB Mem :  96546.5 total,  58774.7 free,  14577.2 used,  26575.1 buff/cache
            MiB Swap:  50321.3 total,  50321.3 free,      0.0 used.  81969.4 avail Mem

                PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
               5063 analyst   20   0 5391564 586996 212284 S  21.4   0.6 189:58.98 /usr/bin/java -Xmx512m -jar /opt/shop/shop.jar
               6120 analyst   20   0 4391564 286996 112284 S   3.5   0.3  12:08.10 /usr/bin/java -Xmx256m -jar /opt/shop/batch.jar
               1750 root      20   0  258588  14796  12752 S   7,1   0,2   0:41.19 /usr/sbin/postgres -D /var/lib/postgres
                 42 root      20   0       0      0      0 I   9.0   0.0   0:00.10 [kworker/3:1]
               7001 analyst   20   0   14520   5748   3488 R  28.6   0.0   0:00.08 top -c -n 1 -b
                900 root      20   0   15000   6000   4000 S   0.0   0.1   0:00.50 sshd: analyst@pts/0
                  1 root      20   0  168940  12680   8396 S   0.0   0.0   0:09.71 /sbin/init splash
            """;

    @Test
    public void theProgramsOfAHostAreOfferedBusiestFirst() {
        List<ProcessCandidate> candidates = SshDiscovery.parseProcesses(TOP);

        assertEquals("grouped by program, without kernel threads and without top itself",
                List.of("java", "postgres", "sshd", "init"),
                candidates.stream().map(ProcessCandidate::name).collect(Collectors.toList()));
        ProcessCandidate java = candidates.get(0);
        assertEquals(2, java.instances());
        assertEquals(24.9, java.cpuPercent(), 1e-9);
        assertEquals(0.9, java.memoryPercent(), 1e-9);
        assertEquals("a decimal comma is read as well", 7.1, candidates.get(1).cpuPercent(), 1e-9);
    }

    @Test
    public void whatIsNotTheOutputOfTopIsRecognised() {
        assertNull(SshDiscovery.parseProcesses("bash: top: command not found"));
        assertNull(SshDiscovery.parseProcesses(""));
    }

    @Test
    public void thisMachineIsReadWithoutSsh() {
        Assume.assumeTrue("needs the Unix command top", new File("/usr/bin/top").exists() || new File("/bin/top").exists());

        List<HostCheck> checks = SshDiscovery.inspect(SshPlan.parseHosts("localhost"), null, null);

        assertEquals(1, checks.size());
        assertTrue(checks.get(0).problem(), checks.get(0).reachable());
        assertFalse("something is always running", checks.get(0).processes().isEmpty());
    }
}
