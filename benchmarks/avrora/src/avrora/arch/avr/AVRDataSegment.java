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
 * Creation date: Nov 11, 2005
 */

package avrora.arch.avr;

import avrora.sim.*;

/**
 * The <code>AVRDataSegment</code> class implements a data segment corresponding to the SRAM
 * on the AVR series microcontrollers. This segment implementation is special in that the first 32 bytes
 * are mapped to the register file, the next 64-224 bytes are active registers (IO registers),
 * and the remaining bytes are read and write RAM.
 *
 * @author Ben L. Titzer
 */
public class AVRDataSegment extends Segment {

    private final int sram_start;
    private final ActiveRegister[] ioregs;

    /**
     * The constructor for the <code>AVRDataSegment</code> class creates a new data segment
     * initialized to zero, with the specified total size (including registers and IORs) and
     * the specified active register set positioned after the register file.
     * @param sz the total size of the segment, including the size of the register file and IORs
     * @param ior the IO registers that are positioned after the register file in memory
     * @param st
     */
    public AVRDataSegment(int sz, ActiveRegister[] ior, State st) {
        super("sram", sz, (byte)0, st);
        sram_start = AVRState.NUM_REGS + ior.length;
        ioregs = ior;
    }

    /**
     * The <code>direct_read()</code> method accesses the actual values stored in the segment, after
     * watches and instrumentation have been applied. It is intended to be used ONLY internally to the
     * segment. It is protected to allow architecture-specific implementations such as sparse memories
     * or memory mapped IO. In the implementation of the AVR, this method checks to see whether the
     * address lies in the register file, in the active register space, or in the rest of RAM.
     * @param address the address in the segment to read
     * @return the value of the of segment at the specified address
     */
    protected byte direct_read(int address) {
        if (address >= sram_start)
            return segment_data[address];
        if (address >= AVRState.NUM_REGS)
            return ioregs[address - AVRState.NUM_REGS].read();
        return segment_data[address];
    }

    /**
     * The <code>direct_write()</code> method writes the value directly into the array. This
     * method is protected and is ONLY used internally. It is protected to allow architecture-
     * specific implementations such as segments that are not contiguous, contain active register
     * ranges, etc. In the implementation of the AVR, this method checks to see whether the
     * address lies in the register file, in the active register space, or in the rest of RAM.
     * @param address the address to write
     * @param val the value to write into the segment
     */
    protected void direct_write(int address, byte val) {
        if (address >= sram_start)
            segment_data[address] = val;
        else if (address >= AVRState.NUM_REGS)
            ioregs[address - AVRState.NUM_REGS].write(val);
        else segment_data[address] = val;
    }

    /**
     * The <code>exposeRegisters()</code> class allows direct access to the underlying array which
     * represents the register file. This is a performance tweak that is intended for use only by the
     * AVR interpreter. Accessing the underlying array directly circumvents instrumentation and
     * therefore should not be done except by the interpreter itself.
     * @return a reference to the byte array that is used internally to store the values of the registers
     */
    protected byte[] exposeRegisters() {
        return segment_data;
    }
}
