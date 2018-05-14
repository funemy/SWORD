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

package avrora.sim.util;

import avrora.sim.Simulator;
import avrora.sim.State;

/**
 * The <code>MulticastProbe</code> is a wrapper around multiple probes that allows them to act as a single
 * probe. It is useful for composing multiple probes into one and is used internally in the simulator.
 *
 * @author Ben L. Titzer
 * @see Simulator
 */
public class MulticastProbe extends TransactionalList implements Simulator.Probe {

    /**
     * The <code>fireBefore()</code> method is called before the probed instruction executes. In the
     * implementation of the multicast probe, it simply calls the <code>fireBefore()</code> method on each of
     * the probes in the multicast set in the order in which they were inserted.
     *
     * @param state   the state of the simulation
     * @param pc the address at which this instruction resides
     */
    public void fireBefore(State state, int pc) {
        beginTransaction();
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.Probe)pos.object).fireBefore(state, pc);
    }

    /**
     * The <code>fireAfter()</code> method is called after the probed instruction executes. In the
     * implementation of the multicast probe, it simply calls the <code>fireAfter()</code> method on each of
     * the probes in the multicast set in the order in which they were inserted.
     *
     * @param state   the state of the simulation
     * @param pc the address at which this instruction resides
     */
    public void fireAfter(State state, int pc) {
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.Probe)pos.object).fireAfter(state, pc);
        endTransaction();
    }
}
