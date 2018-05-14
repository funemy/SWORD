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

import avrora.arch.legacy.LegacyInstr;
import avrora.core.Program;
import avrora.sim.State;

/**
 * The <code>MemoryMatrixProfiler</code> class collects information about a program's usage of memory. For
 * each instruction in the program, it tracks the memory locations read and written by that instruction. For
 * example, a load instruction that uses an address in a register might load bytes from various locations in
 * the data memory. This class maintains two internal two-dimensional matrices, one for read counts and one
 * for write counts. The matrices are indexed by code address and data address.
 *
 * @author Ben L. Titzer
 */
public class MemoryMatrixProfiler {

    /**
     * The <code>rcount</code> field stores a two dimensional array that records the read count for each
     * memory location for each instruction. It is indexed by program address, and then by data address. This
     * matrix is row-sparse in that rows of all zero (e.g. a non-memory instruction) are not stored. To access
     * this matrix, use the <code>getReadCount()</code> method.
     */
    public final long[][] rcount;

    /**
     * The <code>rcount</code> field stores a two dimensional array that records the write count for each
     * memory location for each instruction. It is indexed by program address, and then by data address. This
     * matrix is row-sparse in that rows of all zero (e.g. a non-memory instruction) are not stored. To access
     * this matrix, use the <code>getWriteCount()</code> method.
     */
    public final long[][] wcount;

    /**
     * The <code>ramSize</code> field stores the maximum RAM address that should be recorded.
     */
    public final int ramSize;

    /**
     * The constructor for the <code>MemoryMatrixProfiler</code> class creates a new memory probe that can be
     * inserted into the simulator to record the full memory access statistics of the program.
     *
     * @param p    the program to record statistics for
     * @param size the size of the RAM in bytes
     */
    public MemoryMatrixProfiler(Program p, int size) {
        ramSize = size;
        rcount = new long[p.program_end][];
        wcount = new long[p.program_end][];
    }

    /**
     * The <code>fireBeforeRead()</code> method is called before the data address is read by the program. In
     * the implementation of <code>MemoryMatrixProfiler</code>, it simply increments the count of reads at the
     * address of the instruction and memory location by one.
     *
     * @param i         the instruction being probed
     * @param address   the address at which this instruction resides
     * @param state     the state of the simulation
     * @param data_addr the address of the data being referenced
     * @param value     the value of the memory location being read
     */
    public void fireBeforeRead(LegacyInstr i, int address, State state, int data_addr, byte value) {
        if (data_addr < ramSize) {
            if (rcount[address] == null) rcount[address] = new long[ramSize];
            rcount[address][data_addr]++;
        }
    }

    /**
     * The <code>fireBeforeWrite()</code> method is called before the data address is written by the program.
     * In the implementation of <code>MemoryMatrixProfiler</code>, it simply increments the count of writes at
     * the address of the instruction and memory location by one.
     *
     * @param i         the instruction being probed
     * @param address   the address at which this instruction resides
     * @param state     the state of the simulation
     * @param data_addr the address of the data being referenced
     * @param value     the value being written to the memory location
     */
    public void fireBeforeWrite(LegacyInstr i, int address, State state, int data_addr, byte value) {
        if (data_addr < ramSize) {
            if (wcount[address] == null) wcount[address] = new long[ramSize];
            wcount[address][data_addr]++;
        }
    }

    /**
     * The <code>fireAfterRead()</code> method is called after the data address is read by the program. In the
     * implementation of <code>MemoryMatrixProfiler</code>, it does nothing.
     *
     * @param i         the instruction being probed
     * @param address   the address at which this instruction resides
     * @param state     the state of the simulation
     * @param data_addr the address of the data being referenced
     * @param value     the value of the memory location being read
     */
    public void fireAfterRead(LegacyInstr i, int address, State state, int data_addr, byte value) {
        // do nothing
    }

    /**
     * The <code>fireAfterWrite()</code> method is called after the data address is written by the program. In
     * the implementation of <code>MemoryMatrixProfiler</code>, it does nothing.
     *
     * @param i         the instruction being probed
     * @param address   the address at which this instruction resides
     * @param state     the state of the simulation
     * @param data_addr the address of the data being referenced
     * @param value     the value being written to the memory location
     */
    public void fireAfterWrite(LegacyInstr i, int address, State state, int data_addr, byte value) {
        // do nothing
    }

    /**
     * The <code>getReadCount()</code> method returns the number of times the specified instruction read the
     * specified memory address.
     *
     * @param address   the program address of the instruction
     * @param data_addr the address of the byte of memory
     * @return the number of times the specified instruction read the specified memory address.
     */
    public long getReadCount(int address, int data_addr) {
        return getCount(rcount, data_addr, address);
    }

    /**
     * The <code>getWriteCount()</code> method returns the number of times the specified instruction wrote the
     * specified memory address.
     *
     * @param address   the program address of the instruction
     * @param data_addr the address of the byte of memory
     * @return the number of times the specified instruction wrote the specified memory address.
     */
    public long getWriteCount(int address, int data_addr) {
        return getCount(wcount, data_addr, address);
    }

    private long getCount(long[][] matrix, int data_addr, int address) {
        if (data_addr < ramSize)
            if (matrix[address] == null)
                return 0;
            else
                return matrix[address][data_addr];
        return 0;
    }

}
