/**
 * Copyright (c) 2005, Regents of the University of California
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
 *
 * Creation date: Nov 22, 2005
 */

package avrora.sim.util;

import avrora.actions.SimAction;
import avrora.sim.Simulator;
import avrora.sim.State;

/**
 * The <code>InstructionCountTimeout</code> class is a probe that simply counts down and throws an
 * exception when the count reaches zero. It is useful for ensuring termination of the simulator, for
 * performance testing, or for profiling and stopping after a specified number of invocations.
 *
 * @author Ben L. Titzer
 */
public class ClockCycleTimeout implements Simulator.Event {
    public final long timeout;
    private final Simulator simulator;

    /**
     * The constructor for <code>InstructionCountTimeout</code> creates a timeout event with the specified
     * initial value.
     *
     * @param sim the simulator to terminate after the specified time
     * @param t the number of cycles in the future
     */
    public ClockCycleTimeout(Simulator sim, long t) {
        simulator = sim;
        timeout = t;
    }

    /**
     * The <code>fire()</code> method is called when the timeout is up. It gathers the state from the
     * simulator and throws an instance of <code>Simulator.TimeoutException</code> that signals that the
     * timeout has been reached. This exception then falls through the <code>run()</code> method of the
     * caller of the simulator.
     */
    public void fire() {
        State state = simulator.getState();
        throw new SimAction.TimeoutException(state.getPC(), state, timeout, "clock cycles");
    }

}
