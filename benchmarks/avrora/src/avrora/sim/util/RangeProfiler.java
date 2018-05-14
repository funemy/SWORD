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

import avrora.core.Program;
import avrora.sim.Simulator;
import avrora.sim.State;

/**
 * The <code>RangeProfiler</code> class implements a probe that can be used to profile a range of addresses in
 * the program. It maintains a simple array of <code>long</code> that stores the count for each instruction in
 * the specified range. It is more space efficient than the <code>ProgramProfiler</code> since it only stores
 * the count for the range specified instead of for the entire program.
 *
 * @author Ben L. Titzer
 * @see Counter
 * @see ProgramProfiler
 */
public class RangeProfiler extends Simulator.Probe.Empty {
    /**
     * The <code>program</code> field stores a reference to the program being profiled.
     */
    public final Program program;

    /**
     * The <code>low_addr</code> stores the lowest address in the range.
     */
    public final int low_addr;

    /**
     * The <code>high_addr</code> stores the highest address in the range.
     */
    public final int high_addr;

    /**
     * The <code>itime</code> field stores the invocation count for each instruction in the range. It is
     * indexed by byte addresses, with expr 0 corresponding to the lowest address in the range
     * (<code>low_addr</code>). at <code>program.getInstr(addr)</code>.
     */
    public final long[] icount;

    /**
     * The constructor for the program profiler constructs the required internal state to store the invocation
     * counts of each instruction.
     *
     * @param p    the program to profile
     * @param low  the low address in the range
     * @param high the high address in the range
     */
    public RangeProfiler(Program p, int low, int high) {
        int size = p.program_end;
        icount = new long[size];
        program = p;
        low_addr = low;
        high_addr = high;
    }

    /**
     * The <code>fireBefore()</code> method is called before the probed instruction executes. In the
     * implementation of the range profiler, it simply increments the count of the instruction at the
     * specified address if that address is in the given range.
     *
     * @param state   the state of the simulation
     * @param address the address at which this instruction resides
     */
    public void fireBefore(State state, int address) {
        if (address < low_addr) return;
        if (address >= high_addr) return;
        icount[address - low_addr]++;
    }
}
