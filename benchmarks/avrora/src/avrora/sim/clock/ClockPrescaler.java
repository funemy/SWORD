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
 * The <code>ClockPrescaler</code> class represents a clock that is another clock scaled appropriately; e.g.
 * 8x slower.
 *
 * @author Ben L. Titzer
 */
public class ClockPrescaler extends Clock {

    /**
     * The <code>driveClock</code> field stores a reference to the clock that the prescaler is derived from.
     */
    protected final Clock driveClock;

    /**
     * The <code>divider</code> stores the number of cycles of the underlying clock are equivalent to one
     * cycle of this clock. For example, with a divider or 8, the underlying clock ticks 8 times for each tick
     * of this clock.
     */
    protected final int divider;

    /**
     * The <code>base</code> field stores the cycle count of the underlying clock at the last time that this
     * clock was reset.
     */
    protected long base;

    /**
     * The <code>ticksBeforeBase</code> field stores the number of ticks that were recorded before the
     * prescaler was reset. This is used in the calculation of the total number of ticks that have elapsed.
     */
    protected long ticksBeforeBase;

    /**
     * The constructor of the <code>ClockPrescaler</code> creates a new clock that is an integer multiple
     * slower than the clock that it is derived from. Additionally, the phase at which this clock fires can be
     * adjusted by resetting.
     *
     * @param n       the name of the new clock
     * @param drive   the clock that drives this derived clock
     * @param divider the muliple by which the derived clock is slower than the source
     */
    public ClockPrescaler(String n, Clock drive, int divider) {
        super(n, drive.getHZ() / divider);
        driveClock = drive;
        this.divider = divider;
    }

    /**
     * The <code>getCount()</code> method returns the number of clock cycles (ticks) that have elapsed for
     * this clock. In the implementation of the <code>ClockPrescaler</code>, this method calculates the number
     * of scaled cycles since the last reset.
     *
     * @return the number of elapsed time ticks in clock cycles
     */
    public long getCount() {
        return (driveClock.getCount() - base) / divider;
    }

    /**
     * The <code>getTotalCount()</code> method returns the total number of clock cycles (ticks) that have
     * elapsed for this clock. In the implementation of the <code>ClockPrescaler</code>, this method
     * calculates the number of scaled cycles since the last reset, plus the number of ticks elapsed before
     * the reset.
     *
     * @return the number of elapsed time ticks in clock cycles
     */
    public long getTotalCount() {
        return getCount() + ticksBeforeBase;
    }

    /**
     * The <code>insertEvent()</code> method inserts an event into the event queue of the clock with the
     * specified delay in clock cycles. The event will then be executed at the future time specified. In the
     * implementation of <code>ClockPrescaler</code>, the event will be scheduled in the underlying clock,
     * with the delay calculated correctly from the last time that the prescaler was reset.
     *
     * @param e     the event to be inserted
     * @param delta the number of (scaled) cycles in the future at which to fire
     */
    public void insertEvent(Simulator.Event e, long delta) {
        long driverCount = driveClock.getCount() - base;
        long nextTick = ((driverCount / divider) + 1) * divider;
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

    /**
     * The <code>reset()</code> method resets the internal clock prescaler to zero. Thus, the prescaler's
     * previous phase is broken, and the clock signal continues with the same frequency, only that the first
     * tick will happen <code>divider</code> cycles from now.
     */
    public void reset() {
        long newbase = driveClock.getCount();
        long diff = newbase - base;
        ticksBeforeBase += diff / divider;
        base = newbase;
    }

}
