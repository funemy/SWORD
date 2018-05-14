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
 * The <code>LegacyInstrProto</code> interface represents an object that is capable of building
 * <code>LegacyInstr</code> instances given an array of <code>LegacyOperand</code> instances. It also contains methods
 * that describe the instructions such as their name, their variant name, and their size in bytes.
 *
 * @author Ben L. Titzer
 */
public interface LegacyInstrProto {
    /**
     * The <code>build()</code> method constructs a new <code>LegacyInstr</code> instance with the given operands,
     * checking the operands against the constraints that are specific to each instruction.
     *
     * @param pc  the address at which the instruction will be located
     * @param ops the operands to the instruction
     * @return a new <code>LegacyInstr</code> instance representing the instruction with the given operands
     */
    public LegacyInstr build(int pc, LegacyOperand[] ops);

    /**
     * The <code>getSize()</code> method returns the size of the instruction in bytes. Since each prototype
     * corresponds to exactly one instruction variant, all instructions built by this prototype will have the
     * same size.
     *
     * @return the size of the instruction in bytes
     */
    public int getSize();

    /**
     * The <code>getVariant()</code> method returns the variant name of the instruction as a string. Since
     * instructions like load and store have multiple variants, they each have specific variant names to
     * distinguish them internally in the core of Avrora. For example, for "ld x+, (addr)", the variant is
     * "ldpi" (load with post increment), but the actual instruction is "ld", so this method will return
     * "ldpi".
     *
     * @return the variant of the instruction that this prototype represents
     */
    public String getVariant();

    /**
     * The <code>getName()</code> method returns the name of the instruction as a string. For instructions
     * that are variants of instructions, this method returns the actual name of the instruction. For example,
     * for "ld x+, (addr)", the variant is "ldpi" (load with post increment), but the actual instruction is
     * "ld", so this method will return "ld".
     *
     * @return the name of the instruction
     */
    public String getName();
}
