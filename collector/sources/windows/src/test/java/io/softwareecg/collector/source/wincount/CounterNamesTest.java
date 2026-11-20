package io.softwareecg.collector.source.wincount;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Counters written in English are read on a Windows of another language.
 */
public class CounterNamesTest {

    private final CounterNames names = new CounterNames(new FakeCounterAccess());

    @Test
    public void objectAndCounterGetTheNamesOfTheMachine() {
        assertEquals("\\Prozessor(_Total)\\Prozessorzeit (%)", names.localize("\\Processor(_Total)\\% Processor Time"));
        assertEquals("\\Arbeitsspeicher\\Verfügbare Bytes", names.localize("\\Memory\\Available Bytes"));
        assertEquals("upper and lower case do not matter",
                "\\Arbeitsspeicher\\Verfügbare Bytes", names.localize("\\memory\\available bytes"));
    }

    @Test
    public void theInstanceStaysAsItIs() {
        assertEquals("\\Prozess(java#1)\\Prozessorzeit (%)", names.localize("\\Process(java#1)\\% Processor Time"));
        assertEquals("an instance may contain parentheses",
                "\\Netzwerkschnittstelle(Intel(R) 82574L)\\Empfangene Bytes/s",
                names.localize("\\Network Interface(Intel(R) 82574L)\\Bytes Received/sec"));
    }

    @Test
    public void wildcardsStay() {
        assertEquals("\\Prozess(*)\\*", names.localize("\\Process(*)\\*"));
        assertEquals("\\Netzwerkschnittstelle(*)\\Empfangene Bytes/s", names.localize("\\Network Interface(*)\\Bytes Received/sec"));
    }

    @Test
    public void aNameOfTheMachineStaysAsItIs() {
        assertEquals("\\Prozessor(_Total)\\Prozessorzeit (%)", names.localize("\\Prozessor(_Total)\\Prozessorzeit (%)"));
        assertEquals("\\No Such Object\\No Such Counter", names.localize("\\No Such Object\\No Such Counter"));
    }

    @Test
    public void whatIsNoCounterOfThisMachineStaysAsItIs() {
        assertEquals("a path with its machine is complete already",
                "\\\\server\\Processor(_Total)\\% Processor Time", names.localize("\\\\server\\Processor(_Total)\\% Processor Time"));
        assertEquals("Processor", names.localize("Processor"));
        assertEquals("\\Processor", names.localize("\\Processor"));
        assertEquals("\\Processor(_Total", names.localize("\\Processor(_Total"));
        assertEquals(null, names.localize(null));
    }
}
