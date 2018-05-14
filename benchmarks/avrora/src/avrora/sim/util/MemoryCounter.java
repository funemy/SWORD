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
 * The <code>MemoryCounter</code> is the simplest example of memory profiling functionality. When inserted as
 * a watch at a particular memory location, it will simply count the number of reads and writes to that memory
 * location. It is analagous to the <code>Counter</code> probe which is used to count the execution frequency
 * of a particular instruction.
 *
 * @author Ben L. Titzer
 * @see Simulator.Watch
 * @see Counter
 */
public class MemoryCounter extends Simulator.Watch.Empty {

    /**
     * The <code>rcount</code> field stores the number of reads encountered for this memory location.
     */
    public long rcount;

    /**
     * The <code>wcount</code> field stores the number of writes encountered for this memory location.
     */
    public long wcount;

    /**
     * The <code>fireBeforeRead()</code> method is called before the data address is read by the program. In
     * the implementation of <code>MemoryCounter</code>, it simply increments the count of reads by one.
     *
     * @param state     the state of the simulation
     * @param data_addr the address of the data being referenced
     */
    public void fireBeforeRead(State state, int data_addr) {
        rcount++;
    }

    /**
     * The <code>fireBeforeWrite()</code> method is called before the data address is written by the program.
     * In the implementation of <code>MemoryCounter</code>, it simply increments the count of writes by one.
     *
     * @param state     the state of the simulation
     * @param data_addr the address of the data being referenced
     * @param value     the value being written to the memory location
     */
    public void fireBeforeWrite(State state, int data_addr, byte value) {
        wcount++;
    }

}
