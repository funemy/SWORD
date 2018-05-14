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
 * The <code>SequenceProbe</code> is a probe composer that allows a probe to be fired for every instruction
 * executed between a specified entrypoint and a specified exit point. For example, if the entrypoint is a
 * call instruction and the exit point is the instruction following the call instruction, then the probe will
 * fire for every instruction executed between the call and return, including both the call and the
 * instruction following the call. <br><br>
 * <p/>
 * This probe supports nested entries (e.g. recursive calls). It is best used on pieces of the program that
 * are single-entry/single-exit such as calls, interrupts, basic blocks, and SSE regions of control flow
 * graphs. It does not work well for loops because of the nesting behavior.
 *
 * @author Ben L. Titzer
 */
public class SequenceProbe implements Simulator.Probe {

    /**
     * The immutable <code>entry_addr</code> field stores the address that enables the per-instruction calling
     * of the probe passed in the constructor.
     */
    public final int entry_addr;

    /**
     * The immutable <code>exit_addr</code> field stores the address that disables the per-instruction calling
     * of the probe passed when the nesting level reaches zero.
     */
    public final int exit_addr;

    /**
     * The immutable <code>probe</code> field stores a reference to the probe passed in the constructor.
     */
    public final Simulator.Probe probe;

    /**
     * The <code>nesting</code> field stores the current nesting level (i.e. the number of times
     * <code>entry_addr</code> has been reached without <code>exit_addr</code> intervening).
     */
    public int nesting;

    /**
     * The constructor for the <code>SequenceProbe</code> class simply stores its arguments into the
     * corresponding public final fields in this object, leaving the probe in a state where it is ready to be
     * inserted into a simulator.
     *
     * @param p     the probe to fire for each instruction when the sequence is entered
     * @param entry the byte address of the entrypoint to the sequence
     * @param exit  the byte address of the exitpoint of the sequence
     */
    public SequenceProbe(Simulator.Probe p, int entry, int exit) {
        entry_addr = entry;
        exit_addr = exit;
        probe = p;
    }

    /**
     * The <code>fireBefore()</code> method is called before the probed instruction executes. In the
     * implementation of the sequence probe, if the address is the entrypoint address, then the nesting level
     * is incremented. When the nesting level is greater than one, then the sequence probe will delegate the
     * <code>fireBefore()</code> call to the user probe.
     *
     * @param state   the state of the simulation
     * @param pc the address at which this instruction resides
     */
    public void fireBefore(State state, int pc) {
        if (pc == entry_addr) nesting++;
        if (nesting > 0) probe.fireBefore(state, pc);
    }

    /**
     * The <code>fireBefore()</code> method is called before the probed instruction executes. When the nesting
     * level is greater than one, then the sequence probe will delegate the <code>fireAfter()</code> call to
     * the user probe.  If the address is the exit point, then the nesting level is decremented after the call
     * to <code>fireAfter()</code> of the user probe.
     *
     * @param state   the state of the simulation
     * @param pc the address at which this instruction resides
     */
    public void fireAfter(State state, int pc) {
        if (nesting > 0) probe.fireAfter(state, pc);
        if (pc == exit_addr)
            nesting = (nesting - 1) <= 0 ? 0 : nesting - 1;
    }

    /**
     * The <code>reset()</code> method simply resets the nesting level of the sequence probe, as if it had
     * exited from all nested entries into the region.
     */
    public void reset() {
        nesting = 0;
    }
}
