package io.softwareecg.awb.datasource.adapter.windows;

import io.softwareecg.awb.datasource.adapter.windows.WindowsDiscovery.ProcessCandidate;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.softwareecg.awb.datasource.adapter.windows.WindowsDiscovery;
public class WindowsDiscoveryTest {

    @Test
    public void theProcessesOfAHostAreGroupedByProgramTheBiggestFirst() {
        List<ProcessCandidate> candidates = WindowsDiscovery.candidates(List.of(
                "\\\\SRV\\Process(Idle)\\Working Set",
                "\\\\SRV\\Process(svchost)\\Working Set",
                "\\\\SRV\\Process(java)\\Working Set",
                "\\\\SRV\\Process(svchost#1)\\Working Set",
                "\\\\SRV\\Process(java#1)\\Working Set",
                "\\\\SRV\\Process(sqlservr)\\Working Set",
                "\\\\SRV\\Process(c#)\\Working Set",
                "\\\\SRV\\Process(_Total)\\Working Set"),
                new double[]{8_000, 20e6, 900e6, 30e6, 300e6, 2e9, -1, 5e9});

        assertEquals("neither the idle process nor the sum of all is a program",
                List.of("sqlservr", "java", "svchost", "c#"),
                candidates.stream().map(ProcessCandidate::name).collect(Collectors.toList()));
        assertEquals(2, candidates.get(1).instances());
        assertEquals(1_200e6, candidates.get(1).memoryBytes(), 1);
        assertEquals("a process without a value counts as one that uses nothing", 0, candidates.get(3).memoryBytes(), 0);
    }

    @Test
    public void notMoreCandidatesThanCanBeShown() {
        List<String> counters = new java.util.ArrayList<>();
        double[] memory = new double[40];
        for (int i = 0; i < 40; i++) {
            counters.add("\\\\SRV\\Process(program" + i + ")\\Working Set");
            memory[i] = i;
        }
        List<ProcessCandidate> candidates = WindowsDiscovery.candidates(counters, memory);
        assertEquals(12, candidates.size());
        assertEquals("program39", candidates.get(0).name());
    }

    @Test
    public void theCountersAreReadOnWindowsOnly() {
        boolean windows = System.getProperty("os.name", "").startsWith("Win");
        String problem = WindowsDiscovery.problem();
        if (windows) {
            // with or without the library: the reason is not the system
            assertFalse(problem != null && problem.contains("runs on Windows"));
        } else {
            assertNotNull(problem);
            assertNull("nothing is asked of a host that cannot be read from here",
                    WindowsDiscovery.inspect(List.of()).stream().findAny().orElse(null));
        }
    }
}
