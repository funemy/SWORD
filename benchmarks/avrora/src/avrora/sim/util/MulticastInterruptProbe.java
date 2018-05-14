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
 * The <code>MulticastInterruptProbe</code> is a wrapper around multiple probes that allows them to act as a single
 * probe. It is useful for composing multiple probes into one and is used internally in the simulator.
 *
 * @author Ben L. Titzer
 * @see Simulator
 */
public class MulticastInterruptProbe extends TransactionalList implements Simulator.InterruptProbe {
    /**
     * The <code>fireBeforeInvoke()</code> method of an interrupt probe will be called by the
     * simulator before control is transferred to this interrupt, before the microcontroller
     * has been woken from its current sleep mode. In this implementation, the method broadcasts the call to all of
     * the other probes.
     * @param s the state of the simulator
     * @param inum the number of the interrupt being entered
     */
    public void fireBeforeInvoke(State s, int inum) {
        beginTransaction();
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.InterruptProbe)pos.object).fireBeforeInvoke(s, inum);
    }

    /**
     * The <code>fireAfterInvoke()</code> method of an interrupt probe will be called by the
     * simulator after control is transferred to this interrupt handler, i.e. after the current
     * PC is pushed onto the stack, interrupts are disabled, and the current PC is set to
     * the start of the interrupt handler. In this implementation, the method broadcasts the call to all of
     * the other probes.
     * @param s the state of the simulator
     * @param inum the number of the interrupt being entered
     */
    public void fireAfterInvoke(State s, int inum) {
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.InterruptProbe)pos.object).fireAfterInvoke(s, inum);
        endTransaction();
    }

    /**
     * The <code>fireWhenDisabled()</code> method of an interrupt probe will be called by the
     * simulator when the interrupt is masked out (disabled) by the program.  In this implementation,
     * the method broadcasts the call to all of
     * the other probes.
     * @param s the state of the simulator
     * @param inum the number of the interrupt being masked out
     */
    public void fireWhenDisabled(State s, int inum) {
        beginTransaction();
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.InterruptProbe)pos.object).fireWhenDisabled(s, inum);
        endTransaction();
    }

    /**
     * The <code>fireWhenEnabled()</code> method of an interrupt probe will be called by the
     * simulator when the interrupt is unmasked (enabled) by the program. In this implementation, the method broadcasts the call to all of
     * the other probes.
     * @param s the state of the simulator
     * @param inum the number of the interrupt being unmasked
     */
    public void fireWhenEnabled(State s, int inum) {
        beginTransaction();
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.InterruptProbe)pos.object).fireWhenEnabled(s, inum);
        endTransaction();
    }

    /**
     * The <code>fireWhenPosted()</code> method of an interrupt probe will be called by the
     * simulator when the interrupt is posted. When an interrupt is posted to the simulator,
     * it will be coming pending if it is enabled (unmasked) and eventually be handled. In this
     * implementation, the method broadcasts the call to all of
     * the other probes.
     * @param s the state of the simulator
     * @param inum the number of the interrupt being posted
     */
    public void fireWhenPosted(State s, int inum) {
        beginTransaction();
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.InterruptProbe)pos.object).fireWhenPosted(s, inum);
        endTransaction();
    }

    /**
     * The <code>fireWhenUnposted()</code> method of an interrupt probe will be called by the
     * simulator when the interrupt is unposted. This can happen if the software resets the
     * flag bit of the corresponding IO register or, for most interrupts, when the pending
     * interrupt is handled. In this implementation, the method broadcasts the call to all of
     * the other probes.
     * @param s the state of the simulator
     * @param inum the number of the interrupt being unposted
     */
    public void fireWhenUnposted(State s, int inum) {
        beginTransaction();
        for (Link pos = head; pos != null; pos = pos.next)
            ((Simulator.InterruptProbe)pos.object).fireWhenUnposted(s, inum);
        endTransaction();
    }
}
