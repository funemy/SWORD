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

package avrora.sim;

import cck.util.Arithmetic;

/**
 * The <code>RW16Register</code> class is an implementation of an IO register that has the simple, default
 * behavior of being able to read and write just as a general purpose register or byte in SRAM.
 *
 * @author Ben L. Titzer
 */
public class RW16Register { // TODO implements ActiveRegister

    public short value;

    /**
     * The <code>read16()</code> method reads the 16-bit value of the IO register as an <code>int</code>. For
     * simple <code>RWRegister</code> instances, this simply returns the internally stored value.
     *
     * @return the value of the register as a byte
     */
    public int read16() {
        return (int)value & 0xffff;
    }

    /**
     * The <code>write()</code> method writes an 16-bit value to the IO register as an <code>int</code>. For
     * simple <code>RW16Register</code> instances, this simply writes the internally stored value.
     *
     * @param val the value to write
     */
    public void write(int val) {
        value = (short)val;
    }

}
