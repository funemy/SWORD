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

package avrora.stack;

import avrora.arch.legacy.LegacyRegister;
import cck.util.Util;

/**
 * The <code>MutableState</code> class represents an abstract state of the processor that is mutable. This is
 * used in computation of next states, but is not used in the state space.
 *
 * @author Ben L. Titzer
 * @see AbstractArithmetic
 * @see StateCache
 */
public class MutableState extends AbstractState implements IORegisterConstants {

    /**
     * The constructor of the <code>MutableState</code> class builds the default values of each of the
     * registers and each IO register that is being modelled. The default is that all registers are known
     * zero, all IO registers are known zero, and the program counter is zero.
     */
    public MutableState() {
        av_SREG = AbstractArithmetic.ZERO;
        av_EIMSK = AbstractArithmetic.ZERO;
        av_TIMSK = AbstractArithmetic.ZERO;
        for (int cntr = 0; cntr < NUM_REGS; cntr++) {
            av_REGISTERS[cntr] = AbstractArithmetic.ZERO;
        }
    }

    public MutableState(int npc, char nSREG, char nEIMSK, char nTIMSK, char[] nregs) {
        av_REGISTERS = new char[NUM_REGS];
        System.arraycopy(nregs, 0, av_REGISTERS, 0, NUM_REGS);
        pc = npc;
        av_SREG = nSREG;
        av_EIMSK = nEIMSK;
        av_TIMSK = nTIMSK;
    }

    /**
     * The <code>merge()</code> method merges this abstract state with another abstract state and returns a
     * new copy. This abstract state is not updated. The operation is a simple pointwise merging operation:
     * each value is merged with its corresponding abstract value in the other abstract state.
     *
     * @param s the abstract state to merge with
     * @return a new abstract state that represents the merged abstract states
     * @see AbstractArithmetic
     */
    public MutableState merge(MutableState s) {

        if (pc != s.pc)
            throw Util.failure("cannot merge abstract states with different program counters");

        MutableState n = copy();

        n.av_SREG = AbstractArithmetic.merge(this.av_SREG, s.av_SREG);
        for (int cntr = 0; cntr < NUM_REGS; cntr++)
            n.av_REGISTERS[cntr] = AbstractArithmetic.merge(this.av_REGISTERS[cntr], s.av_REGISTERS[cntr]);

        return n;
    }

    /**
     * The <code>hashCode()</code> method computes an integer hash code for this state. A good hash code is
     * needed to make hashtables in <code>StateSpace</code> efficient.
     *
     * @throws Util.InternalError
     */
    public int hashCode() {
        throw Util.failure("cannot compute hash code of MutableState");
    }

    /**
     * The <code>equals()</code> method implements the standard <code>java.lang.Object</code> equality testing
     * contract.
     *
     * @param o the object to test equality against.
     * @throws Util.InternalError
     */
    public boolean equals(Object o) {
        throw Util.failure("cannot perform .equals() on MutableState");
    }

    /**
     * The <code>setPC()</code> method updates the concrete value of the program counter. The program counter
     * is known in ever abstract state.
     *
     * @param npc the new concrete value of the program counter
     */
    public void setPC(int npc) {
        pc = npc;
    }

    /**
     * The <code>writeSREG</code> method updates the abstract value of the status register.
     *
     * @param val the new abstract value to write to the status register.
     */
    public void writeSREG(char val) {
        av_SREG = AbstractArithmetic.canon(val);
    }

    /**
     * The <code>writeSREG</code> method updates one bit of the abstract value of the status register.
     *
     * @param val the new abstract value of the bit
     */
    public void setSREG_bit(int bit, char val) {
        av_SREG = AbstractArithmetic.setBit(av_SREG, bit, val);
    }

    /**
     * The <code>setFlag_I()</code> method updates the abstract value of the I flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_I(char val) {
        setSREG_bit(SREG_I, val);
    }

    /**
     * The <code>setFlag_T()</code> method updates the abstract value of the T flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_T(char val) {
        setSREG_bit(SREG_T, val);
    }

    /**
     * The <code>setFlag_H()</code> method updates the abstract value of the H flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_H(char val) {
        setSREG_bit(SREG_H, val);
    }

    /**
     * The <code>setFlag_S()</code> method updates the abstract value of the S flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_S(char val) {
        setSREG_bit(SREG_S, val);
    }

    /**
     * The <code>setFlag_V()</code> method updates the abstract value of the V flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_V(char val) {
        setSREG_bit(SREG_V, val);
    }

    /**
     * The <code>setFlag_N()</code> method updates the abstract value of the N flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_N(char val) {
        setSREG_bit(SREG_N, val);
    }

    /**
     * The <code>setFlag_Z()</code> method updates the abstract value of the Z flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_Z(char val) {
        setSREG_bit(SREG_Z, val);
    }

    /**
     * The <code>setFlag_C()</code> method updates the abstract value of the C flag.
     *
     * @param val the new abstract bit of the flag
     */
    public void setFlag_C(char val) {
        setSREG_bit(SREG_C, val);
    }

    /**
     * The <code>setIORegisterAV()</code> method writes the abstract value of an IO register. If the register
     * is being modelled, then its value will be updated. Otherwise, the write will be ignored.
     *
     * @param num the IO register number to be updated
     * @param val the new abstract value of the IO register
     */
    public void setIORegisterAV(int num, char val) {
        if (num == IORegisterConstants.SREG) av_SREG = val;
        if (num == IORegisterConstants.EIMSK) av_EIMSK = val;
        if (num == IORegisterConstants.TIMSK) av_TIMSK = val;
    }

    /**
     * The <code>setRegisterAV()</code> method writes the abstract value of a register in the abstract state
     *
     * @param r   the register to write
     * @param val the new abstract value of the register
     */
    public void setRegisterAV(LegacyRegister r, char val) {
        av_REGISTERS[r.getNumber()] = AbstractArithmetic.canon(val);
    }

}
