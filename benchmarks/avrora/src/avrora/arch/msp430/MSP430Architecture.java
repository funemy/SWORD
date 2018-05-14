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
 * Creation date: Nov 14, 2005
 */

package avrora.arch.msp430;

import avrora.arch.*;
import cck.util.Util;

/**
 * The <code>MSP430Architecture</code> class implements an architecture for use in Avrora.
 * An instance of this class allows access to important architectural tools such as
 * an assembler, disassembler, etc.
 *
 * @author Ben L. Titzer
 */
public class MSP430Architecture implements AbstractArchitecture {

    public static final MSP430Architecture INSTANCE = new MSP430Architecture();

    /**
     * The <code>getDisassembler()</code> method returns an instance of the appropriate
     * disassembler for the architecture. The disassembler can be used to decode binary
     * instructions into <code>AbstractInstr</code> instances of the appropriate type.
     * @return an instance of the <code>AbstractDisassembler</code> interface appropriate
     * for this architecture
     */
    public AbstractDisassembler getDisassembler() {
        return new MSP430Disassembler();
    }

    public AbstractAssembler getAssembler() {
        throw Util.unimplemented();
    }

    public AbstractParser getParser() {
        throw Util.unimplemented();
    }

    public AbstractInstr[] newInstrArray(int len) {
        return new MSP430Instr[len];
    }

}
