/**
 * Copyright (c) 2004-2005, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package avrora.sim.clock;

import avrora.sim.Simulator;
import cck.util.Util;

/**
 * The <code>DerivedClock</code> class represents a clock that is derived from another clock; i.e. the derived
 * clock runs slower but is synchronized with the clock that it is derived from. An example is the 32khz real
 * time clock on the Mica2--it runs independently of the main clock, but in the simulation, it uses the
 * main clock signal to synchronize its execution to the execution of the program.
 *
 * @author Ben L. Titzer
 */
public class DerivedClock extends Clock {

    /**
     * The <code>driveClock</code> field stores a reference to the clock that is underlying this derived
     * clock.
     */
    protected final Clock driveClock;

    /**
     * The <code>divider</code> stores a the ration between the clockspeed of the drive clock and the
     * clockspeed of this clock.
     */
    protected final double divider;

    /**
     * The constructor of the <code>DerivedClock</code> creates a new clock with the specified name, driven by
     * the specified clock, with the specified clockrate. The derived clock can have any speed that is slower
     * than the clock that it is derived from. Roundoff errors will happen when the rates are not an integer
     * multiple of each other, but are guaranteed never to exceed 1 cycle of the underlying clock.
     *
     * @param n      the name of the clock
     * @param driver the clock source from which this clock is derived
     * @param hz     the number of cycles per second of this clock
     */
    public DerivedClock(String n, Clock driver, long hz) {
        super(n, hz);
        this.driveClock = driver;
        if (driver.getHZ() < hz)
            throw Util.failure("cannot derive faster clock from slower clock");
        divider = driver.getHZ() / hz;
    }

    /**
     * The <code>getCount()</code> method returns the total count of clock ticks that have happened for this
     * clock. Since this clock is a derived clock, it computes the number of clock cycles that have happened
     * based on the number of clock cycles that have happened for the underlying clock from which it is
     * derived.
     *
     * @return the count in cycles of this clock
     */
    public long getCount() {
        return (long)(driveClock.getCount() / divider);
    }

    /**
     * The <code>insertEvent()</code> method inserts an event into the event queue of the clock with the
     * specified delay in clock cycles. The event will then be executed at the future time specified.
     *
     * @param e     the event to be inserted
     * @param delta the number of cycles in the future at which to event
     */
    public void insertEvent(Simulator.Event e, long delta) {
        long driverCount = driveClock.getCount();
        long nextTick = (long)(((long)(driverCount / divider) + delta) * divider);
        driveClock.insertEvent(e, nextTick - driverCount);
    }

    /**
     * The <code>removeEvent()</code> method removes an event from the event queue of the clock. The
     * comparison used is reference equality, not <code>.equals()</code>.
     *
     * @param e the event to remove
     */
    public void removeEvent(Simulator.Event e) {
        driveClock.removeEvent(e);
    }
}
