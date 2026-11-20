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
 * the possible cpu states as enum
 * <p/>
 * s -> User CPU time: The time the CPU has spent running users’ processes that are not niced.
 * sy -> System CPU time: The time the CPU has spent running the kernel and its processes.
 * ni -> Nice CPU time: The time the CPU has spent running users’ proccess that have been niced.
 * wa -> iowait: Amount of time the CPU has been waiting for I/O to complete.
 * hi -> Hardware IRQ: The amount of time the CPU has been servicing hardware interrupts.
 * si -> Software Interrupts.: The amount of time the CPU has been servicing software interrupts.
 */
public enum CPUCounterEnum {

    /**
     * The us.
     */
    us,
    /**
     * The sy.
     */
    sy,
    /**
     * The ni.
     */
    ni,
    /**
     * The id.
     */
    id,
    /**
     * The wa.
     */
    wa,
    /**
     * The hi.
     */
    hi,
    /**
     * The si.
     */
    si,
    /**
     * The st.
     */
    st
}


