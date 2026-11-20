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
package io.softwareecg.collector.source.unix.counters.top;

/**
 * the counters of the process list.
 * PID = Process ID
 * USER = the user that has started the process
 * PR = the priority of the process
 * NI = the "nice value"
 * VIRT = the amount of virtual memory the process allocated
 * RES = die resident size (the amount of physical memory the process allocated)
 * SHR = the size of shared memory
 * S = the state of the process. 'D' non interruptible sleeping, 'R' running, 'S' sleeping, 'T'  traced/stopped, 'Z' Zombi process
 * %CPU = der prozentuelle Anteil an seit dem letzten Neu-laden der Seite durch den Prozess verbrauchten CPU Zeit
 * %MEM = the amount of physical memory in percent, that the process allocated
 * TIME+ = the amount of CPU time the process has consumed
 */
public enum ProcessCounterEnum {

    /**
     * The pid.
     */
    PID,
    /**
     * The user.
     */
    USER,
    /**
     * The pr.
     */
    PR,
    /**
     * The ni.
     */
    NI,
    /**
     * The virt.
     */
    VIRT,
    /**
     * The res.
     */
    RES,
    /**
     * The shr.
     */
    SHR,
    /**
     * The s.
     */
    S,
    /**
     * The cpu.
     */
    CPU,
    /**
     * The mem.
     */
    MEM,
    /**
     * The time.
     */
    TIME,
    /**
     * The command.
     */
    COMMAND
}


