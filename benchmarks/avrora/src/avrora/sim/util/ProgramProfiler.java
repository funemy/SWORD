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
 * The <code>ProgramProfiler</code> class implements a probe that can be used to profile pieces of the program
 * or the whole program. It maintains a simple array of <code>long</code> that stores the count for every
 * instruction.
 *
 * @author Ben L. Titzer
 * @see Counter
 */
public class ProgramProfiler extends Simulator.Probe.Empty {

    /**
     * The <code>program</code> field stores a reference to the program being profiled.
     */
    public final Program program;

    /**
     * The <code>itime</code> field stores the invocation count for each instruction in the program. It is
     * indexed by byte addresses. Thus <code>itime[addr]</code> corresponds to the invocation for the
     * instruction at <code>program.getInstr(addr)</code>.
     */
    public final long[] icount;

    /**
     * The constructor for the program profiler constructs the required internal state to store the invocation
     * counts of each instruction.
     *
     * @param p the program to profile
     */
    public ProgramProfiler(Program p) {
        int size = p.program_end;
        icount = new long[size];
        program = p;
    }

    /**
     * The <code>fireBefore()</code> method is called before the probed instruction executes. In the
     * implementation of the program profiler, it simply increments the count of the instruction at the
     * specified address.
     *
     * @param state   the state of the simulation
     * @param pc the address at which this instruction resides
     */
    public void fireBefore(State state, int pc) {
        icount[pc]++;
    }

}
