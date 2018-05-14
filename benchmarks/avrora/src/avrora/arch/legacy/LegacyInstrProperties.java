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

package avrora.arch.legacy;

/**
 * The <code>LegacyInstrProperties</code> represents a grab bag of the properties of an instruction. The fields are
 * public and final, which allows fast access from the interpreter.
 *
 * @author Ben L. Titzer
 * @see LegacyInstr
 */
public class LegacyInstrProperties {

    /**
     * The <code>name</code> field stores an immutable reference to the name of the instruction as a string.
     */
    public final String name;

    /**
     * The <code>variant</code> field stores an immutable reference to the variant of the instruction as a
     * string.
     */
    public final String variant;

    /**
     * The <code>size</code> field stores the size of the instruction in bytes.
     */
    public final int size;

    /**
     * The <code>cycles</code> field stores the minimum number of cycles required to invoke this
     * instruction.
     */
    public final int cycles;

    /**
     * The constructor for the <code>LegacyInstrProperties</code> class simply initializes the final fields of this
     * class based on the input parameters.
     *
     * @param n the name of the instruction as a string
     * @param v the variant of the instruction as a string
     * @param s the size of the instruction in bytes
     * @param c the minimum number of cycles required to execute this instruction
     */
    public LegacyInstrProperties(String n, String v, int s, int c) {
        name = n;
        variant = v;
        size = s;
        cycles = c;
    }
}
