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

/**
 * The <code>ActiveRegister</code> interface models the behavior of a register that may perform
 * some simulation work as a result of being read or written. For example, the register might
 * configure a device, begin a transmission, or unpost an interrupt. Since some IO registers behave
 * specially with regards to the devices they control, their functionality can be implemented externally
 * to the interpreter, in the device implementation.
 *
 * @author Ben L. Titzer
 */
public interface ActiveRegister {

    /**
     * The <code>read()</code> method reads the 8-bit value of the IO register as a byte. For special IO
     * registers, this may cause some action like device activity, or the actual value of the register may
     * need to be fetched or computed.
     *
     * @return the value of the register as a byte
     */
    public byte read();

    /**
     * The <code>write()</code> method writes an 8-bit value to the IO register as a byte. For special IO
     * registers, this may cause some action like device activity, masking/unmasking of interrupts, etc.
     *
     * @param val the value to write
     */
    public void write(byte val);

}
