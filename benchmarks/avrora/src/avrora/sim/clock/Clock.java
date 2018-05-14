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

/**
 * The <code>Clock</code> class represents a clock within the simulation. There is one main clock for the
 * Simulator itself, and all other clocks, even if they are not physically derived off the same clock signal,
 * are simulated by inserting events into the main clock.
 *
 * @author Ben L. Titzer
 */
public abstract class Clock {

    /**
     * The <code>hz</code> field stores the rate of this clock in cycles per second.
     */
    protected final long hz;

    /**
     * The <code>name</code> field stores the name of this clock as a string.
     */
    protected final String name;

    protected Clock(String n, long hz) {
        this.hz = hz;
        this.name = n;
    }

    /**
     * The <code>getHZ()</code> method returns the number of cycles per second at which this clock runs.
     *
     * @return the number of cycles per second on this clock
     */
    public long getHZ() {
        return hz;
    }

    /**
     * The <code>getName()</code> method returns the name of this clock source. Each clock has a name, so that
     * they can be indexed in the simulator.
     *
     * @return the name of the clock as a string
     */
    public String getName() {
        return name;
    }

    /**
     * The <code>getCount()</code> method returns the number of clock cycles (ticks) that have elapsed for
     * this clock.
     *
     * @return the number of elapsed time ticks in clock cycles
     */
    public abstract long getCount();

    /**
     * The <code>insertEvent()</code> method inserts an event into the event queue of the clock with the
     * specified delay in clock cycles. The event will then be executed at the future time specified.
     *
     * @param e      the event to be inserted
     * @param cycles the number of cycles in the future at which to fire
     */
    public abstract void insertEvent(Simulator.Event e, long cycles);

    /**
     * The <code>removeEvent()</code> method removes an event from the event queue of the clock. The
     * comparison used is reference equality, not <code>.equals()</code>.
     *
     * @param e the event to remove
     */
    public abstract void removeEvent(Simulator.Event e);

    /**
     * The <code>millisToCycles()</code> method converts the specified number of milliseconds to a cycle
     * count. The conversion factor used is the number of cycles per second of this clock. This method serves
     * as a utility so that clients need not do repeated work in converting milliseconds to cycles and back.
     *
     * @param ms a time quantity in milliseconds as a double
     * @return the same time quantity in clock cycles, rounded up to the nearest integer
     */
    public long millisToCycles(double ms) {
        return (long)(ms * hz / 1000);
    }

    /**
     * The <code>cyclesToMillis()</code> method converts the specified number of cycles to a time quantity in
     * milliseconds. The conversion factor used is the number of cycles per second of this clock. This method
     * serves as a utility so that clients need not do repeated work in converting milliseconds to cycles and
     * back.
     *
     * @param cycles the number of cycles
     * @return the same time quantity in milliseconds
     */
    public double cyclesToMillis(long cycles) {
        return 1000 * ((double)cycles) / hz;
    }


}
